package com.trading.signal.service;

import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.InstrumentRepository;
import com.trading.signal.repository.PriceCandleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.ta4j.core.BarSeries;
import org.ta4j.core.BaseBarSeriesBuilder;
import org.ta4j.core.indicators.EMAIndicator;
import org.ta4j.core.indicators.MACDIndicator;
import org.ta4j.core.indicators.RSIIndicator;
import org.ta4j.core.indicators.SMAIndicator;
import org.ta4j.core.indicators.ATRIndicator;
import org.ta4j.core.indicators.adx.ADXIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsMiddleIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsUpperIndicator;
import org.ta4j.core.indicators.bollinger.BollingerBandsLowerIndicator;
import org.ta4j.core.indicators.statistics.StandardDeviationIndicator;
import org.ta4j.core.indicators.helpers.ClosePriceIndicator;
import org.ta4j.core.indicators.helpers.VolumeIndicator;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndicatorEngineService {

    private final PriceCandleRepository priceCandleRepository;
    private final IndicatorSnapshotRepository indicatorSnapshotRepository;
    private final InstrumentRepository instrumentRepository;

    /** Map timeframe string → Duration for bar end-time calculation */
    private static final Map<String, Duration> TIMEFRAME_DURATION = Map.of(
            "1m", Duration.ofMinutes(1),
            "5m", Duration.ofMinutes(5),
            "15m", Duration.ofMinutes(15),
            "1h", Duration.ofHours(1),
            "4h", Duration.ofHours(4),
            "1d", Duration.ofDays(1)
    );

    @Transactional
    public void calculateAndSaveIndicators(Long instrumentId, String timeframe, Instant from, Instant to) {
        Instrument instrument = instrumentRepository.findById(instrumentId)
                .orElseThrow(() -> new IllegalArgumentException("Instrument not found: " + instrumentId));

        List<PriceCandle> candles = priceCandleRepository.findCandlesBetween(instrumentId, timeframe, from, to);
        if (candles.isEmpty()) return;

        BarSeries series = buildBarSeries(instrument.getSymbol(), candles, timeframe);

        // Core indicators
        ClosePriceIndicator closePrice = new ClosePriceIndicator(series);
        VolumeIndicator volume = new VolumeIndicator(series);

        RSIIndicator rsi = new RSIIndicator(closePrice, 14);
        SMAIndicator sma5 = new SMAIndicator(closePrice, 5);
        SMAIndicator sma20 = new SMAIndicator(closePrice, 20);
        SMAIndicator sma50 = new SMAIndicator(closePrice, 50);

        // MACD: standard = EMA(12) - EMA(26), signal = EMA(9) of MACD
        MACDIndicator macd = new MACDIndicator(closePrice, 12, 26);
        EMAIndicator macdSignal = new EMAIndicator(macd, 9);

        SMAIndicator volumeSma20 = new SMAIndicator(volume, 20);

        // Anti-loss indicators
        ATRIndicator atr = new ATRIndicator(series, 14);
        ADXIndicator adx = new ADXIndicator(series, 14);

        // Bollinger Bands (20-period, 2 std dev)
        SMAIndicator bbMiddle = new SMAIndicator(closePrice, 20);
        StandardDeviationIndicator stdDev = new StandardDeviationIndicator(closePrice, 20);
        BollingerBandsMiddleIndicator bbMid = new BollingerBandsMiddleIndicator(bbMiddle);
        BollingerBandsUpperIndicator bbUpper = new BollingerBandsUpperIndicator(bbMid, stdDev);
        BollingerBandsLowerIndicator bbLower = new BollingerBandsLowerIndicator(bbMid, stdDev);

        // Build snapshots
        List<IndicatorSnapshot> snapshots = new ArrayList<>();
        for (int i = 0; i < series.getBarCount(); i++) {
            IndicatorSnapshot snapshot = new IndicatorSnapshot();
            snapshot.setInstrument(instrument);
            snapshot.setCandleTime(candles.get(i).getOpenTime());

            if (i >= 14) snapshot.setRsi14(toBd(rsi, i, 4));
            if (i >= 4) snapshot.setMa5(toBd(sma5, i, 8));
            if (i >= 19) {
                snapshot.setMa20(toBd(sma20, i, 8));
                double volVal = volume.getValue(i).doubleValue();
                double volAvg = volumeSma20.getValue(i).doubleValue();
                if (volAvg > 0) {
                    snapshot.setVolumeRatio(BigDecimal.valueOf(volVal / volAvg).setScale(4, RoundingMode.HALF_UP));
                }
                // Bollinger Bands (need 20 periods)
                snapshot.setBbUpper(toBd(bbUpper, i, 8));
                snapshot.setBbMiddle(toBd(bbMid, i, 8));
                snapshot.setBbLower(toBd(bbLower, i, 8));
            }
            if (i >= 49) snapshot.setMa50(toBd(sma50, i, 8));
            if (i >= 26) {
                snapshot.setMacd(toBd(macd, i, 8));
                snapshot.setMacdSignal(toBd(macdSignal, i, 8));
            }
            // ATR & ADX need 14+ bars (ADX internally needs more, but ta4j handles warmup)
            if (i >= 14) {
                snapshot.setAtr14(toBd(atr, i, 8));
            }
            if (i >= 28) {  // ADX needs ~2x period for stable values
                snapshot.setAdx14(toBd(adx, i, 4));
            }

            snapshots.add(snapshot);
        }

        // Batch upsert: skip existing
        int saved = 0;
        for (IndicatorSnapshot snapshot : snapshots) {
            if (!indicatorSnapshotRepository.existsByInstrumentIdAndCandleTime(
                    instrument.getId(), snapshot.getCandleTime())) {
                indicatorSnapshotRepository.save(snapshot);
                saved++;
            }
        }
        if (saved > 0) {
            log.info("Saved {} indicator snapshots for {} [{}]", saved, instrument.getSymbol(), timeframe);
        }
    }

    @Transactional(readOnly = true)
    public List<com.trading.signal.dto.ChartDataDto> getChartData(Long instrumentId, String timeframe, Instant from, Instant to) {
        List<PriceCandle> candles = priceCandleRepository.findCandlesBetween(instrumentId, timeframe, from, to);
        List<IndicatorSnapshot> indicators = indicatorSnapshotRepository
                .findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(instrumentId, from, to);

        Map<Instant, IndicatorSnapshot> indicatorMap = indicators.stream()
                .collect(Collectors.toMap(IndicatorSnapshot::getCandleTime, ind -> ind));

        return candles.stream().map(candle -> {
            IndicatorSnapshot ind = indicatorMap.get(candle.getOpenTime());
            return com.trading.signal.dto.ChartDataDto.builder()
                    .time(candle.getOpenTime())
                    .open(candle.getOpen())
                    .high(candle.getHigh())
                    .low(candle.getLow())
                    .close(candle.getClose())
                    .volume(candle.getVolume())
                    .rsi14(ind != null ? ind.getRsi14() : null)
                    .ma5(ind != null ? ind.getMa5() : null)
                    .ma20(ind != null ? ind.getMa20() : null)
                    .ma50(ind != null ? ind.getMa50() : null)
                    .macd(ind != null ? ind.getMacd() : null)
                    .macdSignal(ind != null ? ind.getMacdSignal() : null)
                    .volumeRatio(ind != null ? ind.getVolumeRatio() : null)
                    .build();
        }).collect(Collectors.toList());
    }

    private BarSeries buildBarSeries(String name, List<PriceCandle> candles, String timeframe) {
        Duration barDuration = TIMEFRAME_DURATION.getOrDefault(timeframe, Duration.ofHours(1));
        BarSeries series = new BaseBarSeriesBuilder().withName(name).build();
        for (PriceCandle candle : candles) {
            ZonedDateTime endTime = ZonedDateTime.ofInstant(candle.getOpenTime(), ZoneId.of("UTC")).plus(barDuration);
            series.addBar(endTime, candle.getOpen(), candle.getHigh(), candle.getLow(), candle.getClose(), candle.getVolume());
        }
        return series;
    }

    /** Helper: extract indicator value as BigDecimal with specified scale */
    private BigDecimal toBd(org.ta4j.core.Indicator<org.ta4j.core.num.Num> indicator, int index, int scale) {
        return BigDecimal.valueOf(indicator.getValue(index).doubleValue()).setScale(scale, RoundingMode.HALF_UP);
    }
}
