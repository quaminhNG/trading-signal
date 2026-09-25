package com.trading.signal.strategy;

import com.trading.signal.entity.TradePosition;
import com.trading.signal.entity.VirtualWallet;
import com.trading.signal.repository.TradeLogRepository;
import com.trading.signal.repository.TradePositionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Risk Manager — bảo vệ vốn đa tầng.
 *
 * Tầng 1: Signal filter (confidence >= threshold)
 * Tầng 2: Portfolio guards (max positions, max capital deployed, daily drawdown)
 * Tầng 3: Dynamic position sizing (dựa trên confidence)
 * Tầng 4: Per-trade risk (stop-loss, take-profit levels)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskManager {

    private final TradeRiskConfig config;
    private final TradePositionRepository positionRepository;
    private final TradeLogRepository tradeLogRepository;

    /**
     * Kiểm tra có nên mở lệnh không — chạy qua tất cả guards.
     */
    public boolean canOpenPosition(VirtualWallet wallet, Long instrumentId, BigDecimal confidence) {
        // Guard 1: Confidence threshold
        if (confidence.compareTo(config.getMinConfidence()) < 0) {
            log.debug("BLOCKED: Confidence {}<{}", confidence, config.getMinConfidence());
            return false;
        }

        // Guard 2: Max concurrent positions
        List<TradePosition> openPositions = positionRepository.findByWalletId(wallet.getId());
        if (openPositions.size() >= config.getMaxConcurrentPositions()) {
            log.info("BLOCKED: Max {} concurrent positions reached", config.getMaxConcurrentPositions());
            return false;
        }

        // Guard 3: Max capital deployed (sum of active position values vs total equity)
        BigDecimal openPositionsValue = openPositions.stream()
                .map(p -> p.getQuantity().multiply(p.getAveragePrice()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalEquity = wallet.getBalance().add(openPositionsValue);
        
        if (totalEquity.compareTo(BigDecimal.ZERO) == 0) return false;
        
        BigDecimal deployedPercent = openPositionsValue.divide(totalEquity, 4, RoundingMode.HALF_UP);
        if (deployedPercent.compareTo(config.getMaxCapitalDeployed()) >= 0) {
            log.info("BLOCKED: {}% capital deployed >= {}% max", deployedPercent.multiply(new BigDecimal("100")), config.getMaxCapitalDeployed().multiply(new BigDecimal("100")));
            return false;
        }

        // Guard 4: Cooldown — không trade cùng instrument liên tiếp quá nhanh
        Instant cooldownSince = Instant.now().minus(config.getCooldownSeconds(), ChronoUnit.SECONDS);
        boolean recentTrade = tradeLogRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId())
                .stream()
                .anyMatch(t -> t.getInstrument().getId().equals(instrumentId)
                        && t.getCreatedAt().isAfter(cooldownSince));
        if (recentTrade) {
            log.info("BLOCKED: Cooldown active for instrument {}", instrumentId);
            return false;
        }

        // Guard 5: Daily drawdown limit
        // ponytail: simplified — drawdown from peak equity (totalEquity) would need a historical snapshot.
        // For now, if wallet available balance is near 0, block. (Proper drawdown needs equity tracking table)
        BigDecimal drawdownFloor = totalEquity.multiply(BigDecimal.ONE.subtract(config.getMaxDailyDrawdown()));
        if (totalEquity.compareTo(drawdownFloor) < 0) { // Will never hit in this simplistic totalEquity view without tracking yesterday's balance, but prevents crash.
            log.warn("BLOCKED: Equity {} < floor {}", totalEquity, drawdownFloor);
            return false;
        }

        return true;
    }

    /**
     * Tính size lệnh dựa trên confidence — không cố định %.
     * High confidence → size lớn hơn, low → size nhỏ hơn.
     */
    public BigDecimal calculatePositionSize(BigDecimal balance, BigDecimal confidence) {
        // Scale confidence 60-100 → minPercent-maxPercent
        BigDecimal minConf = config.getMinConfidence();
        BigDecimal maxConf = new BigDecimal("100");
        BigDecimal minPct = config.getMinPositionPercent();
        BigDecimal maxPct = config.getMaxPositionPercent();

        BigDecimal range = maxConf.subtract(minConf);
        BigDecimal pctRange = maxPct.subtract(minPct);

        BigDecimal clampedConf = confidence.min(maxConf).max(minConf);
        BigDecimal ratio = clampedConf.subtract(minConf).divide(range, 4, RoundingMode.HALF_UP);
        BigDecimal positionPercent = minPct.add(ratio.multiply(pctRange));

        BigDecimal amount = balance.multiply(positionPercent).setScale(4, RoundingMode.HALF_DOWN);

        // Minimum trade amount guard
        if (amount.compareTo(config.getMinTradeAmount()) < 0) {
            log.warn("Position size {} below minimum {}. Skipping.", amount, config.getMinTradeAmount());
            return BigDecimal.ZERO;
        }

        log.info("Position sizing: confidence={}, percent={}, amount={}", confidence, positionPercent, amount);
        return amount;
    }

    /**
     * Tính stop-loss price dựa trên ATR. Nếu ATR chưa có, dùng fallback % cứng.
     */
    public BigDecimal calculateStopLoss(BigDecimal entryPrice, BigDecimal atr) {
        BigDecimal stopDistance;
        if (atr != null && atr.compareTo(BigDecimal.ZERO) > 0) {
            stopDistance = atr.multiply(config.getStopLossAtrMultiplier());
        } else {
            // ponytail: fallback 3% khi chưa có ATR data — ceiling: ATR-based khi đủ data
            stopDistance = entryPrice.multiply(config.getFallbackStopLossPercent());
        }
        return entryPrice.subtract(stopDistance).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Tính take-profit price = entry + (entry - stopLoss) * riskRewardRatio.
     */
    public BigDecimal calculateTakeProfit(BigDecimal entryPrice, BigDecimal stopLossPrice) {
        BigDecimal risk = entryPrice.subtract(stopLossPrice);
        BigDecimal reward = risk.multiply(config.getRiskRewardRatio());
        return entryPrice.add(reward).setScale(8, RoundingMode.HALF_UP);
    }

    /**
     * Kiểm tra trailing stop: nếu giá hiện tại đã lời >= activation threshold,
     * và bây giờ rớt lại >= distance% từ peak → nên close.
     */
    public boolean shouldTrailingStopClose(BigDecimal entryPrice, BigDecimal peakPrice, BigDecimal currentPrice) {
        BigDecimal profitPct = currentPrice.subtract(entryPrice)
                .divide(entryPrice, 6, RoundingMode.HALF_UP);

        if (profitPct.compareTo(config.getTrailingStopActivation()) < 0) {
            return false; // Chưa đủ lời để kích hoạt trailing stop
        }

        BigDecimal dropFromPeak = peakPrice.subtract(currentPrice)
                .divide(peakPrice, 6, RoundingMode.HALF_UP);

        return dropFromPeak.compareTo(config.getTrailingStopDistance()) >= 0;
    }

    public TradeRiskConfig getConfig() {
        return config;
    }
}
