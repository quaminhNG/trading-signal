package com.trading.signal.dto;

import com.trading.signal.entity.TradeLog;
import java.math.BigDecimal;
import java.time.Instant;

public record TradeLogResponse(
        Long id,
        String instrumentSymbol,
        String type,
        BigDecimal quantity,
        BigDecimal price,
        BigDecimal profitLoss,
        Instant createdAt
) {
    public static TradeLogResponse from(TradeLog log) {
        return new TradeLogResponse(
                log.getId(),
                log.getInstrument() != null ? log.getInstrument().getSymbol() : null,
                log.getType().name(),
                log.getQuantity(),
                log.getPrice(),
                log.getProfitLoss(),
                log.getCreatedAt()
        );
    }
}
