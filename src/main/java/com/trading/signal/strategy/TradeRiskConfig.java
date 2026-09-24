package com.trading.signal.strategy;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * Cấu hình risk management — externalize để điều chỉnh qua application.yml.
 * Không hardcode giá trị nào.
 */
@Component
@ConfigurationProperties(prefix = "app.risk")
public class TradeRiskConfig {

    /** % vốn tối đa cho 1 lệnh (high confidence). Default 15% */
    private BigDecimal maxPositionPercent = new BigDecimal("0.15");

    /** % vốn tối thiểu cho 1 lệnh (low confidence). Default 5% */
    private BigDecimal minPositionPercent = new BigDecimal("0.05");

    /** Hệ số ATR cho stop-loss. Default 1.5x ATR */
    private BigDecimal stopLossAtrMultiplier = new BigDecimal("1.5");

    /** Risk:Reward ratio tối thiểu. Default 1:2 */
    private BigDecimal riskRewardRatio = new BigDecimal("2.0");

    /** Trailing stop kích hoạt khi lời >= x%. Default 1.5% */
    private BigDecimal trailingStopActivation = new BigDecimal("0.015");

    /** Trailing stop distance từ đỉnh (%). Default 1% */
    private BigDecimal trailingStopDistance = new BigDecimal("0.01");

    /** Số vị thế đồng thời tối đa. Default 3 */
    private int maxConcurrentPositions = 3;

    /** % vốn tối đa deployed. Default 40% */
    private BigDecimal maxCapitalDeployed = new BigDecimal("0.40");

    /** Drawdown tối đa trong ngày (%). Default 5% */
    private BigDecimal maxDailyDrawdown = new BigDecimal("0.05");

    /** Cooldown giữa 2 lệnh cùng instrument (giây). Default 7200 (2h) */
    private long cooldownSeconds = 7200;

    /** Confidence tối thiểu để trade. Default 60 */
    private BigDecimal minConfidence = new BigDecimal("60");

    /** Số tiền tối thiểu cho 1 lệnh (USDT). Default 10 */
    private BigDecimal minTradeAmount = new BigDecimal("10");

    /** Stop-loss cứng fallback khi không có ATR (%). Default 3% */
    private BigDecimal fallbackStopLossPercent = new BigDecimal("0.03");

    // --- Getters & Setters ---
    public BigDecimal getMaxPositionPercent() { return maxPositionPercent; }
    public void setMaxPositionPercent(BigDecimal v) { this.maxPositionPercent = v; }
    public BigDecimal getMinPositionPercent() { return minPositionPercent; }
    public void setMinPositionPercent(BigDecimal v) { this.minPositionPercent = v; }
    public BigDecimal getStopLossAtrMultiplier() { return stopLossAtrMultiplier; }
    public void setStopLossAtrMultiplier(BigDecimal v) { this.stopLossAtrMultiplier = v; }
    public BigDecimal getRiskRewardRatio() { return riskRewardRatio; }
    public void setRiskRewardRatio(BigDecimal v) { this.riskRewardRatio = v; }
    public BigDecimal getTrailingStopActivation() { return trailingStopActivation; }
    public void setTrailingStopActivation(BigDecimal v) { this.trailingStopActivation = v; }
    public BigDecimal getTrailingStopDistance() { return trailingStopDistance; }
    public void setTrailingStopDistance(BigDecimal v) { this.trailingStopDistance = v; }
    public int getMaxConcurrentPositions() { return maxConcurrentPositions; }
    public void setMaxConcurrentPositions(int v) { this.maxConcurrentPositions = v; }
    public BigDecimal getMaxCapitalDeployed() { return maxCapitalDeployed; }
    public void setMaxCapitalDeployed(BigDecimal v) { this.maxCapitalDeployed = v; }
    public BigDecimal getMaxDailyDrawdown() { return maxDailyDrawdown; }
    public void setMaxDailyDrawdown(BigDecimal v) { this.maxDailyDrawdown = v; }
    public long getCooldownSeconds() { return cooldownSeconds; }
    public void setCooldownSeconds(long v) { this.cooldownSeconds = v; }
    public BigDecimal getMinConfidence() { return minConfidence; }
    public void setMinConfidence(BigDecimal v) { this.minConfidence = v; }
    public BigDecimal getMinTradeAmount() { return minTradeAmount; }
    public void setMinTradeAmount(BigDecimal v) { this.minTradeAmount = v; }
    public BigDecimal getFallbackStopLossPercent() { return fallbackStopLossPercent; }
    public void setFallbackStopLossPercent(BigDecimal v) { this.fallbackStopLossPercent = v; }
}
