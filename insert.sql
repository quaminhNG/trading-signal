INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (1, 'BTCUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (2, 'ETHUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (3, 'SOLUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (4, 'BNBUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (5, 'XRPUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
INSERT INTO instruments (id, symbol, type, exchange, is_active) VALUES (6, 'DOGEUSDT', 'CRYPTO', 'BINANCE', true) ON CONFLICT (id) DO NOTHING;
