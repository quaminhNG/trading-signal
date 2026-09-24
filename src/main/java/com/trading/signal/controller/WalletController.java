package com.trading.signal.controller;

import com.trading.signal.dto.PositionResponse;
import com.trading.signal.dto.TradeLogResponse;
import com.trading.signal.dto.WalletResponse;
import com.trading.signal.repository.TradeLogRepository;
import com.trading.signal.repository.TradePositionRepository;
import com.trading.signal.repository.VirtualWalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final VirtualWalletRepository walletRepository;
    private final TradePositionRepository positionRepository;
    private final TradeLogRepository tradeLogRepository;

    // ponytail: single-user for now. Multi-tenant = extract user from JWT SecurityContext.
    private static final Long DEFAULT_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(w -> ResponseEntity.ok(WalletResponse.from(w)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> getPositions() {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(wallet -> ResponseEntity.ok(
                        positionRepository.findByWalletId(wallet.getId()).stream()
                                .map(PositionResponse::from)
                                .toList()))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TradeLogResponse>> getHistory() {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(wallet -> ResponseEntity.ok(
                        tradeLogRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()).stream()
                                .map(TradeLogResponse::from)
                                .toList()))
                .orElse(ResponseEntity.notFound().build());
    }
}
