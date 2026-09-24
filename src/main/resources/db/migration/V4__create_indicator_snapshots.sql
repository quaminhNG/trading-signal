CREATE TABLE indicator_snapshots (
    id BIGSERIAL PRIMARY KEY,
    instrument_id BIGINT NOT NULL,
    candle_time TIMESTAMP NOT NULL,
    rsi_14 DECIMAL(10,4),
    ma_5 DECIMAL(18,8),
    ma_20 DECIMAL(18,8),
    ma_50 DECIMAL(18,8),
    macd DECIMAL(18,8),
    macd_signal DECIMAL(18,8),
    volume_ratio DECIMAL(10,4),
    CONSTRAINT fk_indicator_snapshots_instrument FOREIGN KEY (instrument_id) REFERENCES instruments (id),
    CONSTRAINT uk_indicator_snapshots_time UNIQUE (instrument_id, candle_time)
);

CREATE INDEX idx_indicator_snapshots_time ON indicator_snapshots (instrument_id, candle_time);
