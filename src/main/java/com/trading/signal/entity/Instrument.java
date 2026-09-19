package com.trading.signal.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "instruments")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Instrument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String symbol;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private InstrumentType type;

    @Column(nullable = false, length = 50)
    private String exchange;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @PrePersist
    void prePersist() {
        active = true;
    }

    public enum InstrumentType { CRYPTO, STOCK }
}
