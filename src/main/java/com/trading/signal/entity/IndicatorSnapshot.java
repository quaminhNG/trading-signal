package com.trading.signal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "indicator_snapshots",
       uniqueConstraints = @UniqueConstraint(columnNames = {"instrument_id", "candle_time"}),
       indexes = @Index(name = "idx_indicator_snapshots_time", columnList = "instrument_id, candle_time"))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class IndicatorSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "candle_time", nullable = false)
    private Instant candleTime;

    @Column(name = "rsi_14", precision = 10, scale = 4)
    private BigDecimal rsi14;

    @Column(name = "ma_5", precision = 18, scale = 8)
    private BigDecimal ma5;

    @Column(name = "ma_20", precision = 18, scale = 8)
    private BigDecimal ma20;

    @Column(name = "ma_50", precision = 18, scale = 8)
    private BigDecimal ma50;

    @Column(name = "macd", precision = 18, scale = 8)
    private BigDecimal macd;

    @Column(name = "macd_signal", precision = 18, scale = 8)
    private BigDecimal macdSignal;

    @Column(name = "volume_ratio", precision = 10, scale = 4)
    private BigDecimal volumeRatio;

    @Column(name = "atr_14", precision = 18, scale = 8)
    private BigDecimal atr14;

    @Column(name = "adx_14", precision = 10, scale = 4)
    private BigDecimal adx14;

    @Column(name = "bb_upper", precision = 18, scale = 8)
    private BigDecimal bbUpper;

    @Column(name = "bb_middle", precision = 18, scale = 8)
    private BigDecimal bbMiddle;

    @Column(name = "bb_lower", precision = 18, scale = 8)
    private BigDecimal bbLower;
}
