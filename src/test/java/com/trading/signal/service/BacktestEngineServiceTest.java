package com.trading.signal.service;

import com.trading.signal.dto.BacktestRequestDto;
import com.trading.signal.dto.BacktestResultDto;
import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.strategy.MaCrossoverStrategy;
import com.trading.signal.strategy.TradeRiskConfig;
import com.trading.signal.strategy.TradingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
public class BacktestEngineServiceTest {

    @Mock private IndicatorSnapshotRepository indicatorSnapshotRepository;
    @Mock private PriceCandleRepository priceCandleRepository;

    private BacktestEngineService backtestEngineService;

    @BeforeEach
    void setUp() {
        List<TradingStrategy> strategies = List.of(new MaCrossoverStrategy());
        TradeRiskConfig config = new TradeRiskConfig();
        backtestEngineService = new BacktestEngineService(
                indicatorSnapshotRepository, priceCandleRepository, strategies, config
        );
    }

    @Test
    void testRunBacktest_BuySellCycle() {
        BacktestRequestDto request = new BacktestRequestDto();
        request.setInstrumentId(1L);
        request.setTimeframe("1h");
        request.setFrom(Instant.now().minus(2, ChronoUnit.DAYS));
        request.setTo(Instant.now());
        request.setInitialBalance(new BigDecimal("10000.0000"));

        // Setup: prev → Golden Cross (BUY) → Death Cross (SELL at higher price)
        IndicatorSnapshot prev = new IndicatorSnapshot();
        prev.setRsi14(new BigDecimal("45.00"));
        prev.setMa5(new BigDecimal("50000"));
        prev.setMa20(new BigDecimal("51000")); // MA5 < MA20
        prev.setCandleTime(request.getFrom());

        IndicatorSnapshot currBuy = new IndicatorSnapshot();
        currBuy.setRsi14(new BigDecimal("28.00")); // Oversold bonus
        currBuy.setMa5(new BigDecimal("51500"));
        currBuy.setMa20(new BigDecimal("51000")); // MA5 > MA20 → Golden Cross
        currBuy.setMa50(new BigDecimal("50000")); // MA5 > MA50 → +15 trend alignment
        currBuy.setVolumeRatio(new BigDecimal("1.5")); // Volume above threshold → +10
        currBuy.setCandleTime(request.getFrom().plus(1, ChronoUnit.HOURS));

        IndicatorSnapshot currSell = new IndicatorSnapshot();
        currSell.setRsi14(new BigDecimal("75.00")); // Above 70 so Death Cross fires
        currSell.setMa5(new BigDecimal("49000"));
        currSell.setMa20(new BigDecimal("50000")); // MA5 < MA20 → Death Cross
        currSell.setMa50(new BigDecimal("51000")); // MA5 < MA50 → +15 downtrend alignment
        currSell.setVolumeRatio(new BigDecimal("1.5")); // Volume → +10
        currSell.setCandleTime(request.getFrom().plus(2, ChronoUnit.HOURS));

        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(
                request.getInstrumentId(), request.getFrom(), request.getTo()
        )).thenReturn(Arrays.asList(prev, currBuy, currSell));

        PriceCandle buyCandle = new PriceCandle();
        buyCandle.setClose(new BigDecimal("50000"));
        buyCandle.setHigh(new BigDecimal("50500"));
        buyCandle.setLow(new BigDecimal("49500"));

        PriceCandle sellCandle = new PriceCandle();
        sellCandle.setClose(new BigDecimal("55000"));
        sellCandle.setHigh(new BigDecimal("55500"));
        sellCandle.setLow(new BigDecimal("54500"));

        lenient().when(priceCandleRepository.findClosestCandle(request.getInstrumentId(), request.getTimeframe(), currBuy.getCandleTime()))
                .thenReturn(Optional.of(buyCandle));
        lenient().when(priceCandleRepository.findClosestCandle(request.getInstrumentId(), request.getTimeframe(), currSell.getCandleTime()))
                .thenReturn(Optional.of(sellCandle));

        BacktestResultDto result = backtestEngineService.runBacktest(request);

        // Verify we had a profitable cycle
        assertTrue(result.getFinalBalance().compareTo(request.getInitialBalance()) > 0,
                "Should be profitable: " + result.getFinalBalance());
        assertEquals(1, result.getTotalTrades());
        assertEquals(1, result.getWinningTrades());
        assertTrue(result.getTradeHistory().size() >= 2, "Should have BUY + SELL entries");
        assertEquals("BUY", result.getTradeHistory().get(0).getType());

        // Profit Factor should be positive
        assertTrue(result.getProfitFactor().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testRunBacktest_StopLossTriggered() {
        BacktestRequestDto request = new BacktestRequestDto();
        request.setInstrumentId(1L);
        request.setTimeframe("1h");
        request.setFrom(Instant.now().minus(2, ChronoUnit.DAYS));
        request.setTo(Instant.now());
        request.setInitialBalance(new BigDecimal("10000.0000"));

        IndicatorSnapshot prev = new IndicatorSnapshot();
        prev.setRsi14(new BigDecimal("45.00"));
        prev.setMa5(new BigDecimal("50000"));
        prev.setMa20(new BigDecimal("51000"));
        prev.setCandleTime(request.getFrom());

        IndicatorSnapshot currBuy = new IndicatorSnapshot();
        currBuy.setRsi14(new BigDecimal("28.00"));
        currBuy.setMa5(new BigDecimal("51500"));
        currBuy.setMa20(new BigDecimal("51000")); // Golden Cross
        currBuy.setMa50(new BigDecimal("50000"));
        currBuy.setVolumeRatio(new BigDecimal("1.5"));
        currBuy.setCandleTime(request.getFrom().plus(1, ChronoUnit.HOURS));

        // Next candle drops hard — should trigger stop-loss
        IndicatorSnapshot drop = new IndicatorSnapshot();
        drop.setRsi14(new BigDecimal("30.00"));
        drop.setMa5(new BigDecimal("47000"));
        drop.setMa20(new BigDecimal("50000"));
        drop.setCandleTime(request.getFrom().plus(2, ChronoUnit.HOURS));

        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(
                request.getInstrumentId(), request.getFrom(), request.getTo()
        )).thenReturn(Arrays.asList(prev, currBuy, drop));

        PriceCandle buyCandle = new PriceCandle();
        buyCandle.setClose(new BigDecimal("50000"));
        buyCandle.setHigh(new BigDecimal("50500"));
        buyCandle.setLow(new BigDecimal("49500"));

        PriceCandle dropCandle = new PriceCandle();
        dropCandle.setClose(new BigDecimal("47000"));
        dropCandle.setHigh(new BigDecimal("48000"));
        dropCandle.setLow(new BigDecimal("46000")); // Low enough to trigger SL (3% fallback = 48500)

        lenient().when(priceCandleRepository.findClosestCandle(request.getInstrumentId(), request.getTimeframe(), currBuy.getCandleTime()))
                .thenReturn(Optional.of(buyCandle));
        lenient().when(priceCandleRepository.findClosestCandle(request.getInstrumentId(), request.getTimeframe(), drop.getCandleTime()))
                .thenReturn(Optional.of(dropCandle));

        BacktestResultDto result = backtestEngineService.runBacktest(request);

        // Should have stop-loss trade
        boolean hasStopLoss = result.getTradeHistory().stream()
                .anyMatch(t -> "STOP_LOSS".equals(t.getType()));
        assertTrue(hasStopLoss, "Should have triggered stop-loss");

        // Loss should be limited (not catastrophic)
        BigDecimal lossPercent = request.getInitialBalance().subtract(result.getFinalBalance())
                .divide(request.getInitialBalance(), 4, java.math.RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
        assertTrue(lossPercent.compareTo(new BigDecimal("5")) < 0,
                "Loss should be < 5% thanks to stop-loss, but was " + lossPercent + "%");
    }
}
