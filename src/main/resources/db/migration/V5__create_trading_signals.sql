CREATE TABLE trading_signals (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL REFERENCES instruments(id),
    generated_at TIMESTAMP NOT NULL,
    signal_type VARCHAR(10) NOT NULL, -- BUY, SELL, HOLD
    confidence_score DECIMAL(5,2),
    rule_basis TEXT,
    matched_pattern_id BIGINT
);

CREATE INDEX idx_trading_signals_instrument_generated_at ON trading_signals(instrument_id, generated_at DESC);
