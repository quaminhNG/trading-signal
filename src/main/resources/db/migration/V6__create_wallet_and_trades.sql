CREATE TABLE virtual_wallets (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    balance          DECIMAL(19,4) NOT NULL DEFAULT 10000.0000,
    version          BIGINT       NOT NULL DEFAULT 0,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE TABLE trade_positions (
    id               BIGSERIAL    PRIMARY KEY,
    wallet_id        BIGINT       NOT NULL REFERENCES virtual_wallets(id) ON DELETE CASCADE,
    instrument_id    BIGINT       NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    quantity         DECIMAL(19,8) NOT NULL,
    average_price    DECIMAL(19,8) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMP    NOT NULL DEFAULT NOW(),
    UNIQUE(wallet_id, instrument_id)
);

CREATE TABLE trade_logs (
    id               BIGSERIAL    PRIMARY KEY,
    wallet_id        BIGINT       NOT NULL REFERENCES virtual_wallets(id) ON DELETE CASCADE,
    instrument_id    BIGINT       NOT NULL REFERENCES instruments(id) ON DELETE CASCADE,
    type             VARCHAR(10)  NOT NULL, -- 'BUY' or 'SELL'
    quantity         DECIMAL(19,8) NOT NULL,
    price            DECIMAL(19,8) NOT NULL,
    profit_loss      DECIMAL(19,4), -- Nullable, only populated on SELL
    created_at       TIMESTAMP    NOT NULL DEFAULT NOW()
);
