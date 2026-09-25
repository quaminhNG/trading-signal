package com.trading.signal.service;

import com.trading.signal.entity.*;
import com.trading.signal.event.SignalGeneratedEvent;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.TradeLogRepository;
import com.trading.signal.repository.TradePositionRepository;
import com.trading.signal.repository.VirtualWalletRepository;
import com.trading.signal.strategy.RiskManager;
import com.trading.signal.strategy.TradeRiskConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PaperTradingServiceTest {

    @Mock private VirtualWalletRepository walletRepository;
    @Mock private TradePositionRepository positionRepository;
    @Mock private TradeLogRepository tradeLogRepository;
    @Mock private IndicatorSnapshotRepository indicatorSnapshotRepository;
    @Mock private com.trading.signal.repository.PriceCandleRepository priceCandleRepository;
    @Mock private TelegramService telegramService;

    private PaperTradingService paperTradingService;
    private RiskManager riskManager;

    private VirtualWallet wallet;
    private Instrument instrument;
    private TradingSignal buySignal;
    private TradingSignal sellSignal;

    @BeforeEach
    void setUp() {
        TradeRiskConfig config = new TradeRiskConfig();
        // Use 10% for simplified test math
        config.setMaxPositionPercent(new BigDecimal("0.10"));
        config.setMinPositionPercent(new BigDecimal("0.10"));
        config.setMinConfidence(new BigDecimal("60"));
        config.setMinTradeAmount(new BigDecimal("10"));
        config.setMaxConcurrentPositions(3);
        config.setMaxCapitalDeployed(new BigDecimal("0.40"));
        config.setMaxDailyDrawdown(new BigDecimal("0.05"));
        config.setCooldownSeconds(0); // No cooldown in tests
        config.setFallbackStopLossPercent(new BigDecimal("0.03"));
        config.setStopLossAtrMultiplier(new BigDecimal("1.5"));
        config.setRiskRewardRatio(new BigDecimal("2.0"));

        riskManager = new RiskManager(config, positionRepository, tradeLogRepository);
        paperTradingService = new PaperTradingService(
                walletRepository, positionRepository, tradeLogRepository,
                riskManager, indicatorSnapshotRepository, priceCandleRepository,
                telegramService
        );

        wallet = new VirtualWallet();
        wallet.setId(1L);
        wallet.setBalance(new BigDecimal("10000.0000"));

        instrument = new Instrument();
        instrument.setId(1L);
        instrument.setSymbol("BTCUSDT");

        buySignal = new TradingSignal();
        buySignal.setInstrument(instrument);
        buySignal.setSignalType("BUY");
        buySignal.setConfidenceScore(new BigDecimal("75.00"));
        buySignal.setGeneratedAt(LocalDateTime.now());

        sellSignal = new TradingSignal();
        sellSignal.setInstrument(instrument);
        sellSignal.setSignalType("SELL");
        sellSignal.setConfidenceScore(new BigDecimal("80.00"));
        sellSignal.setGeneratedAt(LocalDateTime.now());
    }

    @Test
    void testExecuteBuy_WithRiskManagement() {
        when(walletRepository.findAll()).thenReturn(List.of(wallet));
        when(positionRepository.findByWalletId(wallet.getId())).thenReturn(Collections.emptyList());
        when(positionRepository.findByWalletIdAndInstrumentId(wallet.getId(), instrument.getId()))
                .thenReturn(Optional.empty());
        when(tradeLogRepository.findByWalletIdOrderByCreatedAtDesc(wallet.getId()))
                .thenReturn(Collections.emptyList());
        when(indicatorSnapshotRepository.findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(any(), any(), any()))
                .thenReturn(Collections.emptyList());

        SignalGeneratedEvent event = new SignalGeneratedEvent(this, buySignal, new BigDecimal("60000.0000"));
        paperTradingService.handleSignalGenerated(event);

        // 10% of 10000 = 1000 USDT → balance = 9000
        assertEquals(0, wallet.getBalance().compareTo(new BigDecimal("9000.0000")));
        verify(walletRepository, times(1)).save(wallet);

        // Position should be created with SL and TP
        ArgumentCaptor<TradePosition> posCaptor = ArgumentCaptor.forClass(TradePosition.class);
        verify(positionRepository, times(1)).save(posCaptor.capture());
        TradePosition savedPos = posCaptor.getValue();

        assertNotNull(savedPos.getStopLossPrice(), "Stop-loss should be set");
        assertNotNull(savedPos.getTakeProfitPrice(), "Take-profit should be set");
        assertTrue(savedPos.getStopLossPrice().compareTo(new BigDecimal("60000")) < 0, "SL should be below entry");
        assertTrue(savedPos.getTakeProfitPrice().compareTo(new BigDecimal("60000")) > 0, "TP should be above entry");
    }

    @Test
    void testExecuteSell_ClosesPosition() {
        when(walletRepository.findAll()).thenReturn(List.of(wallet));
        when(walletRepository.findById(wallet.getId())).thenReturn(Optional.of(wallet));

        TradePosition position = new TradePosition();
        position.setWallet(wallet);
        position.setInstrument(instrument);
        position.setQuantity(new BigDecimal("0.01666666")); // ~1000 / 60000
        position.setAveragePrice(new BigDecimal("60000.0000"));

        when(positionRepository.findByWalletIdAndInstrumentId(wallet.getId(), instrument.getId()))
                .thenReturn(Optional.of(position));

        SignalGeneratedEvent event = new SignalGeneratedEvent(this, sellSignal, new BigDecimal("66000.0000"));
        paperTradingService.handleSignalGenerated(event);

        // Revenue = 0.01666666 * 66000 = 1099.99956
        // PnL = 1099.99956 - (0.01666666 * 60000 = 999.9996) = 99.99996
        ArgumentCaptor<TradeLog> logCaptor = ArgumentCaptor.forClass(TradeLog.class);
        verify(tradeLogRepository, times(1)).save(logCaptor.capture());
        assertEquals(TradeLog.TradeType.SELL, logCaptor.getValue().getType());
        assertTrue(logCaptor.getValue().getProfitLoss().compareTo(BigDecimal.ZERO) > 0, "Should be profitable");

        // Position should be deleted
        verify(positionRepository, times(1)).delete(position);
    }

    @Test
    void testBuy_BlockedByLowConfidence() {
        buySignal.setConfidenceScore(new BigDecimal("40.00")); // Below threshold

        when(walletRepository.findAll()).thenReturn(List.of(wallet));

        SignalGeneratedEvent event = new SignalGeneratedEvent(this, buySignal, new BigDecimal("60000.0000"));
        paperTradingService.handleSignalGenerated(event);

        // Should NOT execute any trade
        verify(positionRepository, never()).save(any());
        verify(tradeLogRepository, never()).save(any());
    }
}
