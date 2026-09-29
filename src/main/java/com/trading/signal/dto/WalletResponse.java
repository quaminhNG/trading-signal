package com.trading.signal.dto;

import com.trading.signal.entity.VirtualWallet;
import java.math.BigDecimal;
import java.time.Instant;

public record WalletResponse(
        Long id,
        BigDecimal balance,
        BigDecimal totalCoinValue,
        BigDecimal totalNetWorth,
        Instant createdAt,
        Instant updatedAt
) {
    public static WalletResponse from(VirtualWallet wallet, BigDecimal totalCoinValue) {
        BigDecimal balance = wallet.getBalance() != null ? wallet.getBalance() : BigDecimal.ZERO;
        BigDecimal coinValue = totalCoinValue != null ? totalCoinValue : BigDecimal.ZERO;
        return new WalletResponse(
                wallet.getId(),
                balance,
                coinValue,
                balance.add(coinValue),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }
}
