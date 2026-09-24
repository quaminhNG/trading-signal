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
        Instant createdAt,
        Instant updatedAt
) {
    public static PositionResponse from(TradePosition pos) {
        return new PositionResponse(
                pos.getId(),
                pos.getInstrument() != null ? pos.getInstrument().getSymbol() : null,
                pos.getQuantity(),
                pos.getAveragePrice(),
                pos.getStopLossPrice(),
                pos.getTakeProfitPrice(),
                pos.getPeakPrice(),
                pos.getConfidence(),
                pos.getCreatedAt(),
                pos.getUpdatedAt()
        );
    }
}
