package com.trading.signal.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class ChartDataDto {
    private Instant time;
    
    // Price
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    
    // Indicators
    private BigDecimal rsi14;
    private BigDecimal ma5;
    private BigDecimal ma20;
    private BigDecimal ma50;
    private BigDecimal macd;
    private BigDecimal macdSignal;
    private BigDecimal volumeRatio;
}
