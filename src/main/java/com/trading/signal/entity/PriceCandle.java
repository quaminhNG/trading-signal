package com.trading.signal.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "price_candles",
       uniqueConstraints = @UniqueConstraint(columnNames = {"instrument_id", "timeframe", "open_time"}))
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class PriceCandle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(nullable = false, length = 10)
    private String timeframe;

    @Column(name = "open_time", nullable = false)
    private Instant openTime;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal open;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal high;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal low;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal close;

    @Column(nullable = false, precision = 18, scale = 8)
    private BigDecimal volume;
}
