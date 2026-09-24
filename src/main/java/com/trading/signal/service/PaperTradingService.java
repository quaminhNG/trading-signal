package com.trading.signal.service;

import com.trading.signal.entity.*;
import com.trading.signal.event.SignalGeneratedEvent;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.TradeLogRepository;
import com.trading.signal.repository.TradePositionRepository;
import com.trading.signal.repository.VirtualWalletRepository;
import com.trading.signal.strategy.RiskManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Paper Trading Service — mô phỏng giao dịch với risk management đa tầng.
 *
 * Khi nhận signal:
 *   1. RiskManager kiểm tra guards (confidence, max positions, drawdown, cooldown)
 *   2. RiskManager tính position size (dynamic theo confidence)
 *   3. RiskManager tính stop-loss (ATR-based) và take-profit
 *   4. Mở position với SL/TP levels
 *
 * Cron check mỗi phút:
 *   - Kiểm tra stop-loss / take-profit / trailing stop cho tất cả position đang mở
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaperTradingService {

    private final VirtualWalletRepository walletRepository;
    private final TradePositionRepository positionRepository;
    private final TradeLogRepository tradeLogRepository;
    private final RiskManager riskManager;
    private final IndicatorSnapshotRepository indicatorSnapshotRepository;
    private final com.trading.signal.repository.PriceCandleRepository priceCandleRepository;

    @EventListener
    @Transactional
    public void handleSignalGenerated(SignalGeneratedEvent event) {
        TradingSignal signal = event.getSignal();
        BigDecimal price = event.getPrice();

        log.info("📡 Received {} signal for {} at price {}",
                signal.getSignalType(), signal.getInstrument().getSymbol(), price);

        Iterable<VirtualWallet> wallets = walletRepository.findAll();
        for (VirtualWallet wallet : wallets) {
            try {
                if ("BUY".equals(signal.getSignalType())) {
                    executeBuy(wallet, signal, price);
                } else if ("SELL".equals(signal.getSignalType())) {
                    executeSell(wallet, signal, price);
                }
            } catch (Exception e) {
                log.error("Trade execution failed for wallet {}: {}", wallet.getId(), e.getMessage());
            }
        }
    }

    private void executeBuy(VirtualWallet wallet, TradingSignal signal, BigDecimal price) {
        Long instrumentId = signal.getInstrument().getId();
        BigDecimal confidence = signal.getConfidenceScore() != null
                ? signal.getConfidenceScore() : BigDecimal.ZERO;

        // === RISK GUARDS ===
        if (!riskManager.canOpenPosition(wallet, instrumentId, confidence)) {
            return; // Risk manager đã log lý do block
        }

        // === DYNAMIC POSITION SIZING ===
        BigDecimal tradeAmount = riskManager.calculatePositionSize(wallet.getBalance(), confidence);
        if (tradeAmount.compareTo(BigDecimal.ZERO) <= 0) return;

        BigDecimal quantity = tradeAmount.divide(price, 8, RoundingMode.HALF_DOWN);

        // === STOP-LOSS & TAKE-PROFIT ===
        BigDecimal atr = getLatestAtr(instrumentId);
        BigDecimal stopLoss = riskManager.calculateStopLoss(price, atr);
        BigDecimal takeProfit = riskManager.calculateTakeProfit(price, stopLoss);

        // Deduct from wallet
        wallet.setBalance(wallet.getBalance().subtract(tradeAmount));
        walletRepository.save(wallet);

        // Update or Create Position
        Optional<TradePosition> optPosition = positionRepository
                .findByWalletIdAndInstrumentId(wallet.getId(), instrumentId);

        TradePosition position;
        if (optPosition.isPresent()) {
            position = optPosition.get();
            BigDecimal totalValue = position.getQuantity().multiply(position.getAveragePrice()).add(tradeAmount);
            BigDecimal newQuantity = position.getQuantity().add(quantity);
            position.setAveragePrice(totalValue.divide(newQuantity, 8, RoundingMode.HALF_DOWN));
            position.setQuantity(newQuantity);
            // Cập nhật SL/TP lấy mức bảo thủ hơn (stop loss cao hơn = bảo vệ hơn)
            if (position.getStopLossPrice() == null || stopLoss.compareTo(position.getStopLossPrice()) > 0) {
                position.setStopLossPrice(stopLoss);
            }
            position.setTakeProfitPrice(takeProfit);
        } else {
            position = new TradePosition();
            position.setWallet(wallet);
            position.setInstrument(signal.getInstrument());
            position.setQuantity(quantity);
            position.setAveragePrice(price);
            position.setStopLossPrice(stopLoss);
            position.setTakeProfitPrice(takeProfit);
            position.setPeakPrice(price);
            position.setConfidence(confidence);
        }
        positionRepository.save(position);

        // Trade Log
        TradeLog tradeLog = new TradeLog();
        tradeLog.setWallet(wallet);
        tradeLog.setInstrument(signal.getInstrument());
        tradeLog.setType(TradeLog.TradeType.BUY);
        tradeLog.setPrice(price);
        tradeLog.setQuantity(quantity);
        tradeLogRepository.save(tradeLog);

        log.info("✅ BUY {} {} at {} (SL={}, TP={}, confidence={}, amount={}). Balance: {}",
                quantity, signal.getInstrument().getSymbol(), price,
                stopLoss, takeProfit, confidence, tradeAmount, wallet.getBalance());
    }

    private void executeSell(VirtualWallet wallet, TradingSignal signal, BigDecimal sellPrice) {
        Optional<TradePosition> optPosition = positionRepository
                .findByWalletIdAndInstrumentId(wallet.getId(), signal.getInstrument().getId());

        if (optPosition.isEmpty() || optPosition.get().getQuantity().compareTo(BigDecimal.ZERO) <= 0) {
            log.debug("No position to sell for {} in wallet {}", signal.getInstrument().getSymbol(), wallet.getId());
            return;
        }

        closePosition(wallet, optPosition.get(), sellPrice, "SELL_SIGNAL");
    }

    /**
     * Cron chạy mỗi phút — kiểm tra SL/TP/trailing cho mọi position đang mở.
     */
    @Scheduled(cron = "${app.risk.monitoring-cron:0 * * * * *}")
    @Transactional
    public void monitorOpenPositions() {
        List<TradePosition> allPositions = positionRepository.findAll();
        if (allPositions.isEmpty()) return;

        for (TradePosition position : allPositions) {
            try {
                BigDecimal currentPrice = getCurrentPrice(position.getInstrument());
                if (currentPrice == null || currentPrice.compareTo(BigDecimal.ZERO) <= 0) continue;

                // Update peak price for trailing stop
                position.updatePeakPrice(currentPrice);
                positionRepository.save(position);

                // Check stop-loss
                if (position.getStopLossPrice() != null
                        && currentPrice.compareTo(position.getStopLossPrice()) <= 0) {
                    log.warn("🛑 STOP-LOSS hit for {} at {} (SL={})",
                            position.getInstrument().getSymbol(), currentPrice, position.getStopLossPrice());
                    closePosition(position.getWallet(), position, currentPrice, "STOP_LOSS");
                    continue;
                }

                // Check take-profit
                if (position.getTakeProfitPrice() != null
                        && currentPrice.compareTo(position.getTakeProfitPrice()) >= 0) {
                    log.info("🎯 TAKE-PROFIT hit for {} at {} (TP={})",
                            position.getInstrument().getSymbol(), currentPrice, position.getTakeProfitPrice());
                    closePosition(position.getWallet(), position, currentPrice, "TAKE_PROFIT");
                    continue;
                }

                // Check trailing stop
                if (position.getPeakPrice() != null
                        && riskManager.shouldTrailingStopClose(
                        position.getAveragePrice(), position.getPeakPrice(), currentPrice)) {
                    log.info("📉 TRAILING STOP for {} at {} (peak={}, entry={})",
                            position.getInstrument().getSymbol(), currentPrice,
                            position.getPeakPrice(), position.getAveragePrice());
                    closePosition(position.getWallet(), position, currentPrice, "TRAILING_STOP");
                }
            } catch (Exception e) {
                log.error("Error monitoring position {}: {}", position.getId(), e.getMessage());
            }
        }
    }

    /**
     * Đóng position — logic chung cho sell signal, stop-loss, take-profit, trailing stop.
     */
    private void closePosition(VirtualWallet wallet, TradePosition position, BigDecimal sellPrice, String reason) {
        BigDecimal quantity = position.getQuantity();
        BigDecimal revenue = quantity.multiply(sellPrice);
        BigDecimal cost = quantity.multiply(position.getAveragePrice());
        BigDecimal pnl = revenue.subtract(cost);

        // Refresh wallet to avoid stale data
        wallet = walletRepository.findById(wallet.getId()).orElse(wallet);
        wallet.setBalance(wallet.getBalance().add(revenue));
        walletRepository.save(wallet);

        positionRepository.delete(position);

        TradeLog tradeLog = new TradeLog();
        tradeLog.setWallet(wallet);
        tradeLog.setInstrument(position.getInstrument());
        tradeLog.setType(TradeLog.TradeType.SELL);
        tradeLog.setPrice(sellPrice);
        tradeLog.setQuantity(quantity);
        tradeLog.setProfitLoss(pnl);
        tradeLogRepository.save(tradeLog);

        String emoji = pnl.compareTo(BigDecimal.ZERO) >= 0 ? "💰" : "📉";
        log.info("{} CLOSED [{}] {} {} at {}. PnL: {} USDT. Balance: {}",
                emoji, reason, quantity, position.getInstrument().getSymbol(),
                sellPrice, pnl, wallet.getBalance());
    }

    /** Lấy ATR mới nhất cho instrument (dùng để tính stop-loss) */
    private BigDecimal getLatestAtr(Long instrumentId) {
        return indicatorSnapshotRepository
                .findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(
                        instrumentId,
                        Instant.now().minusSeconds(86400),
                        Instant.now())
                .stream()
                .filter(s -> s.getAtr14() != null)
                .reduce((a, b) -> b) // Lấy cái cuối (mới nhất)
                .map(IndicatorSnapshot::getAtr14)
                .orElse(null);
    }

    /** Lấy giá hiện tại từ candle gần nhất */
    private BigDecimal getCurrentPrice(Instrument instrument) {
        return priceCandleRepository
                .findClosestCandle(instrument.getId(), "1h", Instant.now())
                .map(PriceCandle::getClose)
                .orElse(null);
    }
}
