package com.trading.signal.controller;

import com.trading.signal.collector.DataCollectorService;
import com.trading.signal.repository.InstrumentRepository;
import com.trading.signal.repository.PriceCandleRepository;
import com.trading.signal.repository.TradingSignalRepository;
import com.trading.signal.repository.TradeLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/status")
@RequiredArgsConstructor
public class StatusController {

    private final DataCollectorService dataCollectorService;
    private final PriceCandleRepository priceCandleRepository;
    private final TradingSignalRepository tradingSignalRepository;
    private final TradeLogRepository tradeLogRepository;
    private final InstrumentRepository instrumentRepository;

    @Value("${app.collector.schedule.crypto-cron}")
    private String cryptoCron;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getStatus() {
        Instant lastCollected = dataCollectorService.getLastCollectedAt();
        return ResponseEntity.ok(Map.of(
                "lastCollectedAt", lastCollected != null ? lastCollected.toString() : "Never",
                "totalCandles", priceCandleRepository.count(),
                "totalSignals", tradingSignalRepository.count(),
                "totalTrades", tradeLogRepository.count(),
                "collectorCron", cryptoCron,
                "instrumentsActive", instrumentRepository.findByActiveTrueAndType(
                        com.trading.signal.entity.Instrument.InstrumentType.CRYPTO).size()
        ));
    }
}
