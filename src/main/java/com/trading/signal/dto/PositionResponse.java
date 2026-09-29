package com.trading.signal.dto;

import com.trading.signal.entity.TradePosition;
import java.math.BigDecimal;
import java.time.Instant;

public record PositionResponse(
        Long id,
        String instrumentSymbol,
        BigDecimal quantity,
        BigDecimal averagePrice,
        BigDecimal stopLossPrice,
        BigDecimal takeProfitPrice,
        BigDecimal peakPrice,
        BigDecimal confidence,
        BigDecimal currentPrice,
        BigDecimal unrealizedPnl,
        Instant createdAt,
        Instant updatedAt
) {
    public static PositionResponse from(TradePosition pos, BigDecimal currentPrice) {
        BigDecimal unrealizedPnl = BigDecimal.ZERO;
        if (currentPrice != null && pos.getAveragePrice() != null && pos.getAveragePrice().compareTo(BigDecimal.ZERO) > 0) {
            unrealizedPnl = currentPrice.subtract(pos.getAveragePrice())
                    .divide(pos.getAveragePrice(), 6, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("100"));
        }
        return new PositionResponse(
                pos.getId(),
                pos.getInstrument() != null ? pos.getInstrument().getSymbol() : null,
                pos.getQuantity(),
                pos.getAveragePrice(),
                pos.getStopLossPrice(),
                pos.getTakeProfitPrice(),
                pos.getPeakPrice(),
                pos.getConfidence(),
                currentPrice != null ? currentPrice : BigDecimal.ZERO,
                unrealizedPnl,
                pos.getCreatedAt(),
                pos.getUpdatedAt()
        );
    }
}
