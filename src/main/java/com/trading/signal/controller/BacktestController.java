package com.trading.signal.controller;

import com.trading.signal.dto.BacktestRequestDto;
import com.trading.signal.dto.BacktestResultDto;
import com.trading.signal.service.BacktestEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/backtest")
@RequiredArgsConstructor
public class BacktestController {

    private final BacktestEngineService backtestEngineService;

    @PostMapping
    public ResponseEntity<BacktestResultDto> runBacktest(@RequestBody BacktestRequestDto request) {
        BacktestResultDto result = backtestEngineService.runBacktest(request);
        return ResponseEntity.ok(result);
    }
}
