package com.trading.signal.controller;

import com.trading.signal.dto.ChartDataDto;
import com.trading.signal.service.IndicatorEngineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/charts")
@RequiredArgsConstructor
public class ChartController {

    private final IndicatorEngineService indicatorEngineService;

    @GetMapping("/{instrumentId}")
    public ResponseEntity<List<ChartDataDto>> getChartData(
            @PathVariable Long instrumentId,
            @RequestParam(defaultValue = "1d") String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        List<ChartDataDto> data = indicatorEngineService.getChartData(instrumentId, timeframe, from, to);
        return ResponseEntity.ok(data);
    }
    
    // Helper endpoint to trigger indicator calculation manually for testing
    @PostMapping("/{instrumentId}/calculate")
    public ResponseEntity<Void> calculateIndicators(
            @PathVariable Long instrumentId,
            @RequestParam(defaultValue = "1d") String timeframe,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
            
        indicatorEngineService.calculateAndSaveIndicators(instrumentId, timeframe, from, to);
        return ResponseEntity.ok().build();
    }
}
