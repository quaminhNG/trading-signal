CREATE TABLE price_candles (
    id            BIGSERIAL      PRIMARY KEY,
    instrument_id BIGINT         NOT NULL REFERENCES instruments(id),
    timeframe     VARCHAR(10)    NOT NULL,
    open_time     TIMESTAMP      NOT NULL,
    open          DECIMAL(18,8)  NOT NULL,
    high          DECIMAL(18,8)  NOT NULL,
    low           DECIMAL(18,8)  NOT NULL,
    close         DECIMAL(18,8)  NOT NULL,
    volume        DECIMAL(18,8)  NOT NULL,
    UNIQUE (instrument_id, timeframe, open_time)
);

CREATE INDEX idx_candles_lookup
    ON price_candles (instrument_id, timeframe, open_time DESC);
