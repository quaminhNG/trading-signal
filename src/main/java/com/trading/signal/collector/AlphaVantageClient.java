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
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Alpha Vantage adapter cho stock data.
 * Mặc định disable — bật qua config app.collector.alpha-vantage.enabled=true.
 * Free tier: 25 requests/ngày.
 */
@Component
@ConditionalOnProperty(name = "app.collector.alpha-vantage.enabled", havingValue = "true")
public class AlphaVantageClient implements ExchangeClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    private final String baseUrl;
    private final String apiKey;
    private final RestTemplate restTemplate;

    public AlphaVantageClient(
            @Value("${app.collector.alpha-vantage.base-url}") String baseUrl,
            @Value("${app.collector.alpha-vantage.api-key}") String apiKey) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean supports(Instrument.InstrumentType type) {
        return type == Instrument.InstrumentType.STOCK;
    }

    @Override
    public List<PriceCandle> fetchCandles(String symbol, String timeframe, Instant from, Instant to) {
        // ponytail: Alpha Vantage chỉ hỗ trợ daily cho free tier, map tất cả timeframe → DAILY
        String function = "TIME_SERIES_DAILY";

        String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/query")
                .queryParam("function", function)
                .queryParam("symbol", symbol)
                .queryParam("outputsize", "full")
                .queryParam("apikey", apiKey)
                .toUriString();

        JsonNode response;
        try {
            response = restTemplate.getForObject(url, JsonNode.class);
        } catch (Exception e) {
            throw new ExternalApiException("Alpha Vantage API failed for " + symbol, e);
        }

        if (response == null) return List.of();

        JsonNode timeSeries = response.get("Time Series (Daily)");
        if (timeSeries == null) {
            log.warn("Alpha Vantage returned no time series for {}. Response: {}",
                    symbol, response.has("Note") ? response.get("Note").asText() : "unknown");
            return List.of();
        }

        List<PriceCandle> candles = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = timeSeries.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            Instant date = LocalDate.parse(entry.getKey(), DateTimeFormatter.ISO_LOCAL_DATE)
                    .atStartOfDay().toInstant(ZoneOffset.UTC);

            // Lọc theo khoảng thời gian yêu cầu
            if (date.isBefore(from) || date.isAfter(to)) continue;

            JsonNode data = entry.getValue();
            candles.add(PriceCandle.builder()
                    .timeframe("1d")
                    .openTime(date)
                    .open(new BigDecimal(data.get("1. open").asText()))
                    .high(new BigDecimal(data.get("2. high").asText()))
                    .low(new BigDecimal(data.get("3. low").asText()))
                    .close(new BigDecimal(data.get("4. close").asText()))
                    .volume(new BigDecimal(data.get("5. volume").asText()))
                    .build());
        }

        log.info("Fetched {} candles from Alpha Vantage for {} [{}]", candles.size(), symbol, timeframe);
        return candles;
    }
}
