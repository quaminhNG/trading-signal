package com.trading.signal.service;

import com.trading.signal.entity.IndicatorSnapshot;
import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.PriceCandle;
import com.trading.signal.repository.IndicatorSnapshotRepository;
import com.trading.signal.repository.InstrumentRepository;
import com.trading.signal.repository.PriceCandleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IndicatorEngineServiceTest {

    @Mock
    private PriceCandleRepository priceCandleRepository;
    @Mock
    private IndicatorSnapshotRepository indicatorSnapshotRepository;
    @Mock
    private InstrumentRepository instrumentRepository;

    @InjectMocks
    private IndicatorEngineService indicatorEngineService;

    @Captor
    private ArgumentCaptor<IndicatorSnapshot> snapshotCaptor;

    private Instrument instrument;

    @BeforeEach
    void setUp() {
        instrument = new Instrument();
        instrument.setId(1L);
        instrument.setSymbol("BTCUSDT");
    }

    @Test
    void testCalculateAndSaveIndicators() {
        // Arrange
        Instant startTime = Instant.parse("2023-01-01T00:00:00Z");
        Instant endTime = startTime.plus(50, ChronoUnit.HOURS);

        when(instrumentRepository.findById(1L)).thenReturn(Optional.of(instrument));

        List<PriceCandle> mockCandles = generateMockCandles(instrument, startTime, 50);
        when(priceCandleRepository.findCandlesBetween(eq(1L), eq("1h"), any(), any()))
                .thenReturn(mockCandles);

        // Act
        indicatorEngineService.calculateAndSaveIndicators(1L, "1h", startTime, endTime);

        // Assert
        verify(indicatorSnapshotRepository, times(50)).save(snapshotCaptor.capture());
        
        List<IndicatorSnapshot> savedSnapshots = snapshotCaptor.getAllValues();
        assertThat(savedSnapshots).hasSize(50);
        
        // At index 4 (5th candle), MA5 should be populated
        IndicatorSnapshot snapshot5 = savedSnapshots.get(4);
        assertThat(snapshot5.getMa5()).isNotNull();
        // 10, 20, 30, 40, 50 -> avg = 30
        assertThat(snapshot5.getMa5().doubleValue()).isEqualTo(30.0);

        // At index 14, RSI should be populated
        IndicatorSnapshot snapshot15 = savedSnapshots.get(14);
        assertThat(snapshot15.getRsi14()).isNotNull();
        // Since price is strictly increasing, RSI should be 100
        assertThat(snapshot15.getRsi14().doubleValue()).isEqualTo(100.0);
    }

    private List<PriceCandle> generateMockCandles(Instrument instrument, Instant startTime, int count) {
        List<PriceCandle> candles = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            PriceCandle candle = new PriceCandle();
            candle.setInstrument(instrument);
            candle.setOpenTime(startTime.plus(i, ChronoUnit.HOURS));
            candle.setTimeframe("1h");
            
            // Linear increasing price
            BigDecimal price = BigDecimal.valueOf(10 * (i + 1));
            candle.setOpen(price);
            candle.setHigh(price.add(BigDecimal.ONE));
            candle.setLow(price.subtract(BigDecimal.ONE));
            candle.setClose(price);
            candle.setVolume(BigDecimal.valueOf(100));
            
            candles.add(candle);
        }
        return candles;
    }
}
