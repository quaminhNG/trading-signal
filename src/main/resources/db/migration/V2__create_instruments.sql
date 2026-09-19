CREATE TABLE instruments (
    id        BIGSERIAL    PRIMARY KEY,
    symbol    VARCHAR(20)  NOT NULL UNIQUE,
    type      VARCHAR(20)  NOT NULL,
    exchange  VARCHAR(50)  NOT NULL,
    is_active BOOLEAN      NOT NULL DEFAULT TRUE
);
