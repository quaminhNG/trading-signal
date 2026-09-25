package com.trading.signal.collector;

import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.InstrumentRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.service.IndicatorEngineService;
import com.trading.signal.service.SignalEngineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Orchestrator: lấy danh sách instruments active → gọi adapter phù hợp → upsert vào price_candles.
 * Chạy theo @Scheduled cron từ application.yml.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataCollectorService {

    private volatile Instant lastCollectedAt;

    public Instant getLastCollectedAt() { return lastCollectedAt; }

    private final InstrumentRepository instrumentRepository;
    private final PriceCandleRepository candleRepository;
    private final List<ExchangeClient> exchangeClients;
    private final IndicatorEngineService indicatorEngineService;
    private final SignalEngineService signalEngineService;

    /** Scheduled job cho crypto — chạy theo cron config (ví dụ: mỗi 15 phút) */
    @Scheduled(cron = "${app.collector.schedule.crypto-cron}")
    public void collectCrypto() {
        collect(Instrument.InstrumentType.CRYPTO, "15m", 720); // 15-minute candles, 720 hours lookback (30 days) to warm up indicators
    }

    /** Scheduled job cho stock — mỗi ngày */
    @Scheduled(cron = "${app.collector.schedule.stock-cron}")
    public void collectStock() {
        collect(Instrument.InstrumentType.STOCK, "1d", 100); // 100 days lookback
    }

    /**
     * Thu thập dữ liệu cho một loại instrument.
     * Public để có thể gọi manual qua controller nếu cần.
     */
    public void collect(Instrument.InstrumentType type, String timeframe, int lookbackHours) {
        List<Instrument> instruments = instrumentRepository.findByActiveTrueAndType(type);
        if (instruments.isEmpty()) {
            log.debug("No active {} instruments to collect", type);
            return;
        }

        ExchangeClient client = exchangeClients.stream()
                .filter(c -> c.supports(type))
                .findFirst()
                .orElse(null);

        if (client == null) {
            log.warn("No exchange client available for type {}", type);
            return;
        }

        Instant to = Instant.now();
        Instant from = to.minus(lookbackHours, ChronoUnit.HOURS);

        for (Instrument instrument : instruments) {
            try {
                List<PriceCandle> candles = client.fetchCandles(instrument.getSymbol(), timeframe, from, to);
                int saved = 0;
                for (PriceCandle candle : candles) {
                    // Upsert: skip nếu đã tồn tại (ON CONFLICT logic ở Java level)
                    if (!candleRepository.existsByInstrumentIdAndTimeframeAndOpenTime(
                            instrument.getId(), candle.getTimeframe(), candle.getOpenTime())) {
                        candle.setInstrument(instrument);
                        candleRepository.save(candle);
                        saved++;
                    }
                }
                log.info("Collected {}: {} new / {} total candles", instrument.getSymbol(), saved, candles.size());
                lastCollectedAt = Instant.now();
                
                if (saved > 0 || !candles.isEmpty()) {
                    indicatorEngineService.calculateAndSaveIndicators(instrument.getId(), timeframe, from, to);
                    signalEngineService.generateSignals(instrument, timeframe, from, to);
                }
            } catch (Exception e) {
                // Lỗi 1 instrument không ảnh hưởng các instrument khác
                log.error("Failed to collect data for {}: {}", instrument.getSymbol(), e.getMessage());
            }
        }
    }
}
