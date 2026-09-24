package com.trading.signal.dto;

import com.trading.signal.entity.VirtualWallet;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        Instant createdAt,
        Instant updatedAt
) {
    public static WalletResponse from(VirtualWallet wallet) {
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
