package com.trading.signal.dto;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class BacktestResultDto {
    private BigDecimal initialBalance;
    private BigDecimal finalBalance;
    private BigDecimal totalPnL;
    private BigDecimal winRate;
    private int totalTrades;
    private int winningTrades;
    private int losingTrades;
    private BigDecimal maxDrawdown;
    private BigDecimal profitFactor;
    private List<TradeSnapshot> tradeHistory;

    @Data
    @Builder
    public static class TradeSnapshot {
        private String type; // BUY or SELL
        private String time;
        private BigDecimal price;
        private BigDecimal pnl;
        private BigDecimal balanceAfter;
    }
}
