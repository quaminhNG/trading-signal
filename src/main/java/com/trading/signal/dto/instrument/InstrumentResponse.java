package com.trading.signal.dto.instrument;

public record InstrumentResponse(
        Long id,
        String symbol,
        String type,
        String exchange,
        boolean active
) {}
