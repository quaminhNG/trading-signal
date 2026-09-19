package com.trading.signal.dto.instrument;

import com.trading.signal.entity.Instrument.InstrumentType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record InstrumentRequest(
        @NotBlank(message = "Symbol is required")
        @Size(max = 20, message = "Symbol max 20 characters")
        String symbol,

        @NotNull(message = "Type is required (CRYPTO or STOCK)")
        InstrumentType type,

        @NotBlank(message = "Exchange is required")
        @Size(max = 50, message = "Exchange max 50 characters")
        String exchange
) {}
