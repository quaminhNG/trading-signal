package com.trading.signal.event;

import com.trading.signal.entity.TradingSignal;
import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;

public class SignalGeneratedEvent extends ApplicationEvent {
    
    private final TradingSignal signal;
    private final BigDecimal price;

    public SignalGeneratedEvent(Object source, TradingSignal signal, BigDecimal price) {
        super(source);
        this.signal = signal;
        this.price = price;
    }

    public TradingSignal getSignal() {
        return signal;
    }

    public BigDecimal getPrice() {
        return price;
    }
}
