package com.trading.signal.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class BacktestRequestDto {
    private Long instrumentId;
    private String timeframe;
    private Instant from;
    private Instant to;
    private BigDecimal initialBalance;
}
