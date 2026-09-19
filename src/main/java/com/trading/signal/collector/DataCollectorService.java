package com.trading.signal.collector;

import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.InstrumentRepository;
import com.trading.signal.repository.PriceCandleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Orchestrator: lấy danh sách instruments active → gọi adapter phù hợp → upsert vào price_candles.
 * Chạy theo @Scheduled cron từ application.yml.
 */
@Service
public class DataCollectorService {

    private static final Logger log = LoggerFactory.getLogger(DataCollectorService.class);

    private final InstrumentRepository instrumentRepository;
    private final PriceCandleRepository candleRepository;
    private final List<ExchangeClient> exchangeClients;

    public DataCollectorService(InstrumentRepository instrumentRepository,
                                PriceCandleRepository candleRepository,
                                List<ExchangeClient> exchangeClients) {
        this.instrumentRepository = instrumentRepository;
        this.candleRepository = candleRepository;
        this.exchangeClients = exchangeClients;
    }

    /** Scheduled job cho crypto — mỗi giờ */
    @Scheduled(cron = "${app.collector.schedule.crypto-cron}")
    public void collectCrypto() {
        collect(Instrument.InstrumentType.CRYPTO, "1h", 2); // 2 giờ lookback
    }

    /** Scheduled job cho stock — mỗi ngày */
    @Scheduled(cron = "${app.collector.schedule.stock-cron}")
    public void collectStock() {
        collect(Instrument.InstrumentType.STOCK, "1d", 48); // 2 ngày lookback
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
            } catch (Exception e) {
                // Lỗi 1 instrument không ảnh hưởng các instrument khác
                log.error("Failed to collect data for {}: {}", instrument.getSymbol(), e.getMessage());
            }
        }
    }
}
