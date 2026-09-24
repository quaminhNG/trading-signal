package com.trading.signal.service;

import com.trading.signal.dto.BacktestRequestDto;
import com.trading.signal.dto.BacktestResultDto;
import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.strategy.SignalCandidate;
import com.trading.signal.strategy.TradeRiskConfig;
import com.trading.signal.strategy.TradingStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Backtest Engine — dùng CHUNG TradingStrategy với SignalEngine (DRY).
 * Mô phỏng SL/TP/trailing stop giống hệ thống live.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BacktestEngineService {

    private final IndicatorSnapshotRepository indicatorSnapshotRepository;
    private final PriceCandleRepository priceCandleRepository;
    private final List<TradingStrategy> strategies;
    private final TradeRiskConfig riskConfig;

    @Transactional(readOnly = true)
    public BacktestResultDto runBacktest(BacktestRequestDto request) {
        List<IndicatorSnapshot> snapshots = indicatorSnapshotRepository
                .findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(
                        request.getInstrumentId(), request.getFrom(), request.getTo());

        BigDecimal currentBalance = request.getInitialBalance();
        BigDecimal peakBalance = currentBalance;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        // Position state
        BigDecimal positionQty = BigDecimal.ZERO;
        BigDecimal positionAvgPrice = BigDecimal.ZERO;
        BigDecimal stopLossPrice = null;
        BigDecimal takeProfitPrice = null;
        BigDecimal peakPrice = BigDecimal.ZERO;

        int totalTrades = 0;
        int winningTrades = 0;
        int losingTrades = 0;
        BigDecimal totalProfit = BigDecimal.ZERO;
        BigDecimal totalLoss = BigDecimal.ZERO;

        List<BacktestResultDto.TradeSnapshot> tradeHistory = new ArrayList<>();

        if (snapshots.size() < 2) {
            return buildResult(request.getInitialBalance(), currentBalance, winningTrades,
                    losingTrades, totalTrades, maxDrawdown, totalProfit, totalLoss, tradeHistory);
        }

        // Generate signals using the same strategies as live trading
        List<SignalCandidate> allSignals = new ArrayList<>();
        for (TradingStrategy strategy : strategies) {
            allSignals.addAll(strategy.evaluate(snapshots));
        }
        // Sort by candleTime for walk-forward order
        allSignals.sort((a, b) -> a.candleTime().compareTo(b.candleTime()));
        
        java.util.Map<java.time.Instant, SignalCandidate> signalMap = new java.util.HashMap<>();
        for (SignalCandidate s : allSignals) {
            if (s.isActionable()) {
                signalMap.put(s.candleTime(), s);
            }
        }

        for (IndicatorSnapshot snapshot : snapshots) {
            Optional<PriceCandle> candleOpt = priceCandleRepository.findClosestCandle(
                    request.getInstrumentId(), request.getTimeframe(), snapshot.getCandleTime());
            if (candleOpt.isEmpty()) continue;

            BigDecimal closePrice = candleOpt.get().getClose();
            BigDecimal highPrice = candleOpt.get().getHigh();
            BigDecimal lowPrice = candleOpt.get().getLow();

            // === CHECK SL/TP BEFORE PROCESSING NEW SIGNAL ===
            if (positionQty.compareTo(BigDecimal.ZERO) > 0) {
                // Check stop-loss (use candle low)
                if (stopLossPrice != null && lowPrice.compareTo(stopLossPrice) <= 0) {
                    BigDecimal revenue = positionQty.multiply(stopLossPrice);
                    BigDecimal cost = positionQty.multiply(positionAvgPrice);
                    BigDecimal pnl = revenue.subtract(cost);

                    currentBalance = currentBalance.add(revenue);
                    totalTrades++;
                    if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                        winningTrades++;
                        totalProfit = totalProfit.add(pnl);
                    } else {
                        losingTrades++;
                        totalLoss = totalLoss.add(pnl.abs());
                    }

                    tradeHistory.add(BacktestResultDto.TradeSnapshot.builder()
                            .type("STOP_LOSS")
                            .time(snapshot.getCandleTime().toString())
                            .price(stopLossPrice)
                            .pnl(pnl)
                            .balanceAfter(currentBalance)
                            .build());

                    positionQty = BigDecimal.ZERO;
                    positionAvgPrice = BigDecimal.ZERO;
                    stopLossPrice = null;
                    takeProfitPrice = null;
                    peakPrice = BigDecimal.ZERO;
                    updateDrawdown(currentBalance, peakBalance);
                    if (currentBalance.compareTo(peakBalance) > 0) peakBalance = currentBalance;
                    continue;
                }

                // Check take-profit (use candle high)
                if (takeProfitPrice != null && highPrice.compareTo(takeProfitPrice) >= 0) {
                    BigDecimal revenue = positionQty.multiply(takeProfitPrice);
                    BigDecimal cost = positionQty.multiply(positionAvgPrice);
                    BigDecimal pnl = revenue.subtract(cost);

                    currentBalance = currentBalance.add(revenue);
                    totalTrades++;
                    winningTrades++;
                    totalProfit = totalProfit.add(pnl);

                    tradeHistory.add(BacktestResultDto.TradeSnapshot.builder()
                            .type("TAKE_PROFIT")
                            .time(snapshot.getCandleTime().toString())
                            .price(takeProfitPrice)
                            .pnl(pnl)
                            .balanceAfter(currentBalance)
                            .build());

                    positionQty = BigDecimal.ZERO;
                    positionAvgPrice = BigDecimal.ZERO;
                    stopLossPrice = null;
                    takeProfitPrice = null;
                    peakPrice = BigDecimal.ZERO;
                    if (currentBalance.compareTo(peakBalance) > 0) peakBalance = currentBalance;
                    continue;
                }

                // Update peak for trailing stop simulation
                if (highPrice.compareTo(peakPrice) > 0) peakPrice = highPrice;
            }

            // === PROCESS SIGNAL ===
            SignalCandidate signal = signalMap.get(snapshot.getCandleTime());
            if (signal == null) continue;

            if ("BUY".equals(signal.type()) && positionQty.compareTo(BigDecimal.ZERO) == 0) {
                // Dynamic position sizing based on confidence
                BigDecimal positionPercent = calculatePositionPercent(signal.confidence());
                BigDecimal tradeAmount = currentBalance.multiply(positionPercent);
                if (tradeAmount.compareTo(riskConfig.getMinTradeAmount()) < 0) continue;

                BigDecimal qty = tradeAmount.divide(closePrice, 8, RoundingMode.HALF_DOWN);
                currentBalance = currentBalance.subtract(tradeAmount);
                positionQty = qty;
                positionAvgPrice = closePrice;
                peakPrice = closePrice;

                // Calculate SL/TP
                BigDecimal atr = signal.indicators().get("atr14");
                stopLossPrice = calculateStopLoss(closePrice, atr);
                takeProfitPrice = calculateTakeProfit(closePrice, stopLossPrice);

                tradeHistory.add(BacktestResultDto.TradeSnapshot.builder()
                        .type("BUY")
                        .time(signal.candleTime().toString())
                        .price(closePrice)
                        .balanceAfter(currentBalance)
                        .build());
            }

            if ("SELL".equals(signal.type()) && positionQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal revenue = positionQty.multiply(closePrice);
                BigDecimal cost = positionQty.multiply(positionAvgPrice);
                BigDecimal pnl = revenue.subtract(cost);

                currentBalance = currentBalance.add(revenue);
                totalTrades++;
                if (pnl.compareTo(BigDecimal.ZERO) > 0) {
                    winningTrades++;
                    totalProfit = totalProfit.add(pnl);
                } else {
                    losingTrades++;
                    totalLoss = totalLoss.add(pnl.abs());
                }

                tradeHistory.add(BacktestResultDto.TradeSnapshot.builder()
                        .type("SELL")
                        .time(signal.candleTime().toString())
                        .price(closePrice)
                        .pnl(pnl)
                        .balanceAfter(currentBalance)
                        .build());

                positionQty = BigDecimal.ZERO;
                positionAvgPrice = BigDecimal.ZERO;
                stopLossPrice = null;
                takeProfitPrice = null;
                peakPrice = BigDecimal.ZERO;

                if (currentBalance.compareTo(peakBalance) > 0) peakBalance = currentBalance;
                BigDecimal dd = updateDrawdown(currentBalance, peakBalance);
                if (dd.compareTo(maxDrawdown) > 0) maxDrawdown = dd;
            }
        }

        return buildResult(request.getInitialBalance(), currentBalance, winningTrades,
                losingTrades, totalTrades, maxDrawdown, totalProfit, totalLoss, tradeHistory);
    }

    private BigDecimal calculatePositionPercent(BigDecimal confidence) {
        BigDecimal minConf = riskConfig.getMinConfidence();
        BigDecimal range = new BigDecimal("100").subtract(minConf);
        BigDecimal pctRange = riskConfig.getMaxPositionPercent().subtract(riskConfig.getMinPositionPercent());
        BigDecimal clampedConf = confidence.min(new BigDecimal("100")).max(minConf);
        BigDecimal ratio = clampedConf.subtract(minConf).divide(range, 4, RoundingMode.HALF_UP);
        return riskConfig.getMinPositionPercent().add(ratio.multiply(pctRange));
    }

    private BigDecimal calculateStopLoss(BigDecimal entryPrice, BigDecimal atr) {
        BigDecimal stopDistance;
        if (atr != null && atr.compareTo(BigDecimal.ZERO) > 0) {
            stopDistance = atr.multiply(riskConfig.getStopLossAtrMultiplier());
        } else {
            stopDistance = entryPrice.multiply(riskConfig.getFallbackStopLossPercent());
        }
        return entryPrice.subtract(stopDistance).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateTakeProfit(BigDecimal entryPrice, BigDecimal stopLossPrice) {
        BigDecimal risk = entryPrice.subtract(stopLossPrice);
        BigDecimal reward = risk.multiply(riskConfig.getRiskRewardRatio());
        return entryPrice.add(reward).setScale(8, RoundingMode.HALF_UP);
    }

    private BigDecimal updateDrawdown(BigDecimal current, BigDecimal peak) {
        if (peak.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return peak.subtract(current).divide(peak, 4, RoundingMode.HALF_DOWN)
                .multiply(new BigDecimal("100"));
    }

    private BacktestResultDto buildResult(BigDecimal initial, BigDecimal current, int wins, int losses,
                                          int total, BigDecimal drawdown, BigDecimal totalProfit,
                                          BigDecimal totalLoss, List<BacktestResultDto.TradeSnapshot> history) {
        BigDecimal totalPnL = current.subtract(initial);
        BigDecimal winRate = total > 0
                ? new BigDecimal(wins).divide(new BigDecimal(total), 4, RoundingMode.HALF_DOWN)
                .multiply(new BigDecimal("100"))
                : BigDecimal.ZERO;
        BigDecimal profitFactor = totalLoss.compareTo(BigDecimal.ZERO) > 0
                ? totalProfit.divide(totalLoss, 4, RoundingMode.HALF_DOWN)
                : totalProfit.compareTo(BigDecimal.ZERO) > 0 ? new BigDecimal("999") : BigDecimal.ZERO;

        return BacktestResultDto.builder()
                .initialBalance(initial)
                .finalBalance(current)
                .totalPnL(totalPnL)
                .winRate(winRate)
                .totalTrades(total)
                .winningTrades(wins)
                .losingTrades(losses)
                .maxDrawdown(drawdown)
                .profitFactor(profitFactor)
                .tradeHistory(history)
                .build();
    }
}
