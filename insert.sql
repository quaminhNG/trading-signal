INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (1, 'BTCUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
