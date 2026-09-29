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
    private final com.trading.signal.repository.PriceCandleRepository priceCandleRepository;

    @org.springframework.beans.factory.annotation.Value("${app.trading.default-timeframe:15m}")
    private String defaultTimeframe;

    // ponytail: single-user for now. Multi-tenant = extract user from JWT SecurityContext.
    private static final Long DEFAULT_USER_ID = 1L;

    @GetMapping
    public ResponseEntity<WalletResponse> getWallet() {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(wallet -> {
                    BigDecimal totalCoinValue = positionRepository.findByWalletId(wallet.getId()).stream()
                            .map(pos -> {
                                BigDecimal currentPrice = priceCandleRepository
                                        .findClosestCandle(pos.getInstrument().getId(), defaultTimeframe, java.time.Instant.now())
                                        .map(com.trading.signal.entity.PriceCandle::getClose)
                                        .orElse(BigDecimal.ZERO);
                                return pos.getQuantity().multiply(currentPrice);
                            })
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    return ResponseEntity.ok(WalletResponse.from(wallet, totalCoinValue));
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/positions")
    public ResponseEntity<List<PositionResponse>> getPositions() {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(wallet -> {
                    List<PositionResponse> positions = positionRepository.findByWalletId(wallet.getId()).stream()
                            .map(pos -> {
                                BigDecimal currentPrice = priceCandleRepository
                                        .findClosestCandle(pos.getInstrument().getId(), defaultTimeframe, java.time.Instant.now())
                                        .map(com.trading.signal.entity.PriceCandle::getClose)
                                        .orElse(BigDecimal.ZERO);
                                return PositionResponse.from(pos, currentPrice);
                            })
                            .toList();
                    return ResponseEntity.ok(positions);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/history")
    public ResponseEntity<List<TradeLogResponse>> getHistory(@org.springframework.web.bind.annotation.RequestParam(required = false) Long instrumentId) {
        return walletRepository.findByUserId(DEFAULT_USER_ID)
                .map(wallet -> {
                    var logs = instrumentId != null ?
                            tradeLogRepository.findByWalletIdAndInstrumentIdOrderByCreatedAtDesc(wallet.getId(), instrumentId) :
                            tradeLogRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId());
                    return ResponseEntity.ok(logs.stream().map(TradeLogResponse::from).toList());
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
