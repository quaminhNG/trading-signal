package com.trading.signal.service;

import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.entity.TradingSignal;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.repository.TradingSignalRepository;
import com.trading.signal.strategy.MaCrossoverStrategy;
import com.trading.signal.strategy.TradingStrategy;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class SignalEngineServiceTest {

    @Mock private IndicatorSnapshotRepository indicatorSnapshotRepository;
    @Mock private TradingSignalRepository tradingSignalRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private PriceCandleRepository priceCandleRepository;

    private SignalEngineService signalEngineService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        List<TradingStrategy> strategies = List.of(new MaCrossoverStrategy());
        signalEngineService = new SignalEngineService(
            indicatorSnapshotRepository,
            tradingSignalRepository,
            eventPublisher,
            priceCandleRepository,
            strategies,
            new ObjectMapper()
        );
    }

    @Test
    void testGenerateSignals_BuySignal_GoldenCross() {
        Instrument instrument = new Instrument();
        instrument.setId(1L);

        Instant time1 = Instant.parse("2024-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2024-01-01T11:00:00Z");

        IndicatorSnapshot prev = new IndicatorSnapshot();
        prev.setCandleTime(time1);
        prev.setRsi14(new BigDecimal("28.0")); // Oversold bonus +10
        prev.setMa5(new BigDecimal("35.0"));
        prev.setMa20(new BigDecimal("40.0")); // MA5 < MA20

        IndicatorSnapshot curr = new IndicatorSnapshot();
        curr.setCandleTime(time2);
        curr.setRsi14(new BigDecimal("28.0")); // RSI < 30 → +10
        curr.setMa5(new BigDecimal("42.0"));
        curr.setMa20(new BigDecimal("40.0")); // MA5 > MA20 → Golden Cross
        curr.setMa50(new BigDecimal("38.0")); // MA5 > MA50 → +15 trend alignment
        curr.setVolumeRatio(new BigDecimal("1.5")); // Volume above threshold → +10

        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(prev, curr));
        when(tradingSignalRepository.findByInstrumentIdOrderByGeneratedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        PriceCandle candle = new PriceCandle();
        candle.setClose(new BigDecimal("50000"));
        candle.setOpen(new BigDecimal("49500"));
        candle.setHigh(new BigDecimal("50500"));
        candle.setLow(new BigDecimal("49000"));
        candle.setVolume(new BigDecimal("100"));
        when(priceCandleRepository.findClosestCandle(eq(1L), any(), any()))
                .thenReturn(Optional.of(candle));

        signalEngineService.generateSignals(instrument, "1h", time1, time2);

        ArgumentCaptor<TradingSignal> captor = ArgumentCaptor.forClass(TradingSignal.class);
        verify(tradingSignalRepository, times(1)).save(captor.capture());
        assertEquals("BUY", captor.getValue().getSignalType());
    }

    @Test
    void testGenerateSignals_SellSignal_DeathCross() {
        Instrument instrument = new Instrument();
        instrument.setId(1L);

        Instant time1 = Instant.parse("2024-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2024-01-01T11:00:00Z");

        IndicatorSnapshot prev = new IndicatorSnapshot();
        prev.setCandleTime(time1);
        prev.setRsi14(new BigDecimal("75.0"));
        prev.setMa5(new BigDecimal("45.0"));
        prev.setMa20(new BigDecimal("40.0")); // MA5 > MA20

        IndicatorSnapshot curr = new IndicatorSnapshot();
        curr.setCandleTime(time2);
        curr.setRsi14(new BigDecimal("75.0")); // RSI > 70 → +10 sell bonus
        curr.setMa5(new BigDecimal("38.0"));
        curr.setMa20(new BigDecimal("40.0")); // MA5 < MA20 → Death Cross
        curr.setMa50(new BigDecimal("42.0")); // MA5 < MA50 → +15 downtrend alignment
        curr.setVolumeRatio(new BigDecimal("1.5")); // Volume → +10

        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(prev, curr));
        when(tradingSignalRepository.findByInstrumentIdOrderByGeneratedAtDesc(1L))
                .thenReturn(Collections.emptyList());

        PriceCandle candle = new PriceCandle();
        candle.setClose(new BigDecimal("50000"));
        candle.setOpen(new BigDecimal("50500"));
        candle.setHigh(new BigDecimal("51000"));
        candle.setLow(new BigDecimal("49500"));
        candle.setVolume(new BigDecimal("100"));
        when(priceCandleRepository.findClosestCandle(eq(1L), any(), any()))
                .thenReturn(Optional.of(candle));

        signalEngineService.generateSignals(instrument, "1h", time1, time2);

        ArgumentCaptor<TradingSignal> captor = ArgumentCaptor.forClass(TradingSignal.class);
        verify(tradingSignalRepository, times(1)).save(captor.capture());
        assertEquals("SELL", captor.getValue().getSignalType());
    }

    @Test
    void testLowConfidenceSignal_NotSaved() {
        // Signal with ADX < 20 (sideways market) should have low confidence → skip
        Instrument instrument = new Instrument();
        instrument.setId(1L);

        Instant time1 = Instant.parse("2024-01-01T10:00:00Z");
        Instant time2 = Instant.parse("2024-01-01T11:00:00Z");

        IndicatorSnapshot prev = new IndicatorSnapshot();
        prev.setCandleTime(time1);
        prev.setRsi14(new BigDecimal("45.0"));
        prev.setMa5(new BigDecimal("35.0"));
        prev.setMa20(new BigDecimal("40.0"));

        IndicatorSnapshot curr = new IndicatorSnapshot();
        curr.setCandleTime(time2);
        curr.setRsi14(new BigDecimal("55.0")); // No extreme RSI
        curr.setMa5(new BigDecimal("42.0"));
        curr.setMa20(new BigDecimal("40.0")); // Golden Cross
        curr.setAdx14(new BigDecimal("12.0")); // SIDEWAYS! ADX < 20 → -25 penalty → confidence < 60

        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(eq(1L), any(), any()))
                .thenReturn(Arrays.asList(prev, curr));

        signalEngineService.generateSignals(instrument, "1h", time1, time2);

        // Should NOT save any signal (low confidence due to sideways market)
        verify(tradingSignalRepository, never()).save(any());
    }
}
