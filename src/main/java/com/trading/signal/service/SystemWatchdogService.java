package com.trading.signal.service;

import com.trading.signal.entity.TradePosition;
import com.trading.signal.entity.VirtualWallet;
import com.trading.signal.repository.TradePositionRepository;
import com.trading.signal.repository.VirtualWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Cầu chì tử thần (Kill Switch). 
 * Chạy ngầm độc lập kiểm tra tài sản. Nếu rớt dưới mốc sinh tử, hủy diệt hệ thống để bảo toàn vốn.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SystemWatchdogService {

    private final VirtualWalletRepository walletRepository;
    private final TradePositionRepository positionRepository;
    private final PaperTradingService paperTradingService;
    private final TelegramService telegramService;

    // Default: Nếu tổng tài sản rớt xuống dưới 180$, kích hoạt kill switch.
    @Value("${app.risk.kill-switch-equity:180}")
    private BigDecimal killSwitchEquity;

    @Scheduled(fixedDelay = 60000) // 1 phút check 1 lần
    public void monitorTotalEquity() {
        Iterable<VirtualWallet> wallets = walletRepository.findAll();
        for (VirtualWallet wallet : wallets) {
            BigDecimal cash = wallet.getBalance();
            List<TradePosition> openPositions = positionRepository.findByWalletId(wallet.getId());
            
            BigDecimal positionsValue = openPositions.stream()
                    .map(p -> p.getQuantity().multiply(p.getAveragePrice()))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
                    
            BigDecimal totalEquity = cash.add(positionsValue);
            
            if (totalEquity.compareTo(killSwitchEquity) < 0 && totalEquity.compareTo(BigDecimal.ZERO) > 0) {
                String alert = String.format("TOTAL EQUITY DROPPED BELOW KILL SWITCH THRESHOLD! Equity: %s, Threshold: %s. ACTIVATING KILL SWITCH NOW!", 
                        totalEquity, killSwitchEquity);
                log.error(alert);
                telegramService.sendEmergencyAlert(alert);
                
                // Panic sell everything
                paperTradingService.forceCloseAllPositions(wallet);
                
                telegramService.sendEmergencyAlert("SYSTEM SHUTDOWN INITIATED. WAKE UP!");
                
                // ponytail: The ultimate lazy safeguard - pull the plug.
                log.error("SHUTTING DOWN SYSTEM...");
                System.exit(1);
            }
        }
    }
}
