package com.trading.signal.controller;

import com.trading.signal.entity.TradingSignal;
import com.trading.signal.repository.TradingSignalRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/signals")
@RequiredArgsConstructor
public class SignalController {

    private final TradingSignalRepository tradingSignalRepository;

    @GetMapping
    public ResponseEntity<List<TradingSignal>> getSignals(
            @RequestParam(required = false) Long instrumentId) {
        if (instrumentId != null) {
            return ResponseEntity.ok(tradingSignalRepository.findByInstrumentIdOrderByGeneratedAtDesc(instrumentId));
        }
        return ResponseEntity.ok(tradingSignalRepository.findAll(Sort.by(Sort.Direction.DESC, "generatedAt")));
    }
}
