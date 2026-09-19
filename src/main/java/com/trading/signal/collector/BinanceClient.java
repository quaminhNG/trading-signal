package com.trading.signal.collector;

import com.fasterxml.jackson.databind.JsonNode;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Binance public API adapter.
 * Endpoint: GET /api/v3/klines — không cần API key.
 * Response: mảng JSON, mỗi phần tử là array [openTime, open, high, low, close, volume, ...].
 */
@Component
@ConditionalOnProperty(name = "app.collector.binance.enabled", havingValue = "true")
public class BinanceClient implements ExchangeClient {

    private static final Logger log = LoggerFactory.getLogger(BinanceClient.class);

    /** Map timeframe nội bộ (1h, 4h, 1d) sang interval Binance */
    private static final Map<String, String> INTERVAL_MAP = Map.of(
            "1h", "1h",
            "4h", "4h",
            "1d", "1d"
    );

    private final String baseUrl;
    private final RestTemplate restTemplate;

    public BinanceClient(@Value("${app.collector.binance.base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean supports(Instrument.InstrumentType type) {
        return type == Instrument.InstrumentType.CRYPTO;
    }

    @Override
    public List<PriceCandle> fetchCandles(String symbol, String timeframe, Instant from, Instant to) {
        String interval = INTERVAL_MAP.getOrDefault(timeframe, timeframe);

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/v3/klines")
                .queryParam("symbol", symbol)
                .queryParam("interval", interval)
                .queryParam("startTime", from.toEpochMilli())
                .queryParam("endTime", to.toEpochMilli())
                .queryParam("limit", 1000)
                .toUriString();

        // ponytail: simple retry loop (max 3), avoids spring-retry dependency
        JsonNode response = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                response = restTemplate.getForObject(url, JsonNode.class);
                break;
            } catch (Exception e) {
                log.warn("Binance API attempt {}/3 failed for {}: {}", attempt, symbol, e.getMessage());
                if (attempt == 3) {
                    throw new ExternalApiException("Binance API failed after 3 retries for " + symbol, e);
                }
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new ExternalApiException("Interrupted during retry", ie);
                }
            }
        }

        if (response == null || !response.isArray()) {
            return List.of();
        }

        List<PriceCandle> candles = new ArrayList<>();
        for (JsonNode row : response) {
            // Binance klines format: [openTime, open, high, low, close, volume, closeTime, ...]
            candles.add(PriceCandle.builder()
                    .timeframe(timeframe)
                    .openTime(Instant.ofEpochMilli(row.get(0).asLong()))
                    .open(new BigDecimal(row.get(1).asText()))
                    .high(new BigDecimal(row.get(2).asText()))
                    .low(new BigDecimal(row.get(3).asText()))
                    .close(new BigDecimal(row.get(4).asText()))
                    .volume(new BigDecimal(row.get(5).asText()))
                    .build());
        }

        log.info("Fetched {} candles from Binance for {} [{}]", candles.size(), symbol, timeframe);
        return candles;
    }
}
