package com.trading.signal.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.TradingSignal;
import com.trading.signal.event.SignalGeneratedEvent;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.repository.TradingSignalRepository;
import com.trading.signal.strategy.SignalCandidate;
import com.trading.signal.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;

/**
 * Signal Engine — delegates signal generation to TradingStrategy implementations.
 * Unified logic: same strategy used here and in BacktestEngineService (DRY).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SignalEngineService {

    private final IndicatorSnapshotRepository indicatorSnapshotRepository;
    private final TradingSignalRepository tradingSignalRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final PriceCandleRepository priceCandleRepository;
    private final List<TradingStrategy> strategies;
    private final ObjectMapper objectMapper;

    @Transactional
    public void generateSignals(Instrument instrument, String timeframe, Instant from, Instant to) {
        List<IndicatorSnapshot> snapshots = indicatorSnapshotRepository
                .findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(instrument.getId(), from, to);

        if (snapshots.size() < 2) return;

        for (TradingStrategy strategy : strategies) {
            List<SignalCandidate> candidates = strategy.evaluate(snapshots);

            for (SignalCandidate candidate : candidates) {
                if (!candidate.isActionable()) {
                    log.debug("Skipping low-confidence signal: {} {} confidence={}", 
                            candidate.type(), candidate.pattern(), candidate.confidence());
                    continue;
                }

                // Check duplicate
                boolean exists = tradingSignalRepository
                        .findByInstrumentIdOrderByGeneratedAtDesc(instrument.getId())
                        .stream()
                        .anyMatch(s -> s.getGeneratedAt()
                                .atZone(ZoneId.of("UTC")).toInstant()
                                .equals(candidate.candleTime()));
                if (exists) continue;

                saveSignal(instrument, timeframe, candidate, strategy.getName());
            }
        }
    }

    private void saveSignal(Instrument instrument, String timeframe, SignalCandidate candidate, String strategyName) {
        try {
            var candleOpt = priceCandleRepository.findClosestCandle(
                    instrument.getId(), timeframe, candidate.candleTime());
            BigDecimal closePrice = candleOpt
                    .map(c -> c.getClose())
                    .orElse(BigDecimal.ZERO);

            // Build rule basis JSON with full context
            ObjectNode ruleBasis = objectMapper.createObjectNode();
            ruleBasis.put("strategy", strategyName);
            ruleBasis.put("pattern", candidate.pattern());
            ruleBasis.put("reason", candidate.reason());
            ruleBasis.put("confidence", candidate.confidence());
            ruleBasis.put("closePrice", closePrice);

            // Include all indicator values
            candidate.indicators().forEach((key, value) -> ruleBasis.put(key, value));

            // Include candle OHLV if available
            candleOpt.ifPresent(c -> {
                ruleBasis.put("openPrice", c.getOpen());
                ruleBasis.put("highPrice", c.getHigh());
                ruleBasis.put("lowPrice", c.getLow());
                ruleBasis.put("volume", c.getVolume());
            });

            TradingSignal signal = TradingSignal.builder()
                    .instrument(instrument)
                    .generatedAt(candidate.candleTime().atZone(ZoneId.of("UTC")).toLocalDateTime())
                    .signalType(candidate.type())
                    .confidenceScore(candidate.confidence())
                    .ruleBasis(objectMapper.writeValueAsString(ruleBasis))
                    .build();

            tradingSignalRepository.save(signal);
            eventPublisher.publishEvent(new SignalGeneratedEvent(this, signal, closePrice));

            log.info("📊 {} signal [{}] confidence={} for {} at {}",
                    candidate.type(), candidate.pattern(), candidate.confidence(),
                    instrument.getSymbol(), candidate.candleTime());
        } catch (Exception e) {
            log.error("Failed to save signal for {} at {}: {}",
                    instrument.getSymbol(), candidate.candleTime(), e.getMessage());
        }
    }
}
