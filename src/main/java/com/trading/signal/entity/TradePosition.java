package com.trading.signal.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@Entity
@Table(name = "trade_positions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"wallet_id", "instrument_id"})
})
public class TradePosition {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    private VirtualWallet wallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "instrument_id", nullable = false)
    private Instrument instrument;

    @Column(name = "quantity", nullable = false, precision = 19, scale = 8)
    private BigDecimal quantity;

    @Column(name = "average_price", nullable = false, precision = 19, scale = 8)
    private BigDecimal averagePrice;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @Column(name = "stop_loss_price", precision = 19, scale = 8)
    private BigDecimal stopLossPrice;

    @Column(name = "take_profit_price", precision = 19, scale = 8)
    private BigDecimal takeProfitPrice;

    @Column(name = "peak_price", precision = 19, scale = 8)
    private BigDecimal peakPrice;

    @Column(name = "confidence", precision = 5, scale = 2)
    private BigDecimal confidence;

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public VirtualWallet getWallet() { return wallet; }
    public void setWallet(VirtualWallet wallet) { this.wallet = wallet; }
    public Instrument getInstrument() { return instrument; }
    public void setInstrument(Instrument instrument) { this.instrument = instrument; }
    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
    public BigDecimal getAveragePrice() { return averagePrice; }
    public void setAveragePrice(BigDecimal averagePrice) { this.averagePrice = averagePrice; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public BigDecimal getStopLossPrice() { return stopLossPrice; }
    public void setStopLossPrice(BigDecimal stopLossPrice) { this.stopLossPrice = stopLossPrice; }
    public BigDecimal getTakeProfitPrice() { return takeProfitPrice; }
    public void setTakeProfitPrice(BigDecimal takeProfitPrice) { this.takeProfitPrice = takeProfitPrice; }
    public BigDecimal getPeakPrice() { return peakPrice; }
    public void setPeakPrice(BigDecimal peakPrice) { this.peakPrice = peakPrice; }
    public BigDecimal getConfidence() { return confidence; }
    public void setConfidence(BigDecimal confidence) { this.confidence = confidence; }

    /** Cập nhật peak price nếu giá hiện tại cao hơn */
    public void updatePeakPrice(BigDecimal currentPrice) {
        if (this.peakPrice == null || currentPrice.compareTo(this.peakPrice) > 0) {
            this.peakPrice = currentPrice;
        }
    }

    /** Tính PnL % chưa chốt */
    public BigDecimal getUnrealizedPnlPercent(BigDecimal currentPrice) {
        if (averagePrice.compareTo(BigDecimal.ZERO) == 0) return BigDecimal.ZERO;
        return currentPrice.subtract(averagePrice)
                .divide(averagePrice, 6, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("100"));
    }
}
