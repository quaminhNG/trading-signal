-- Seed default admin user for paper trading
INSERT INTO users (email, password_hash, full_name, role, created_at)
VALUES ('admin@trading.local', '$2a$10$dummyhashnotforlogin000000000000000000000000000000', 'Paper Trader', 'ADMIN', NOW())
ON CONFLICT (email) DO NOTHING;

-- Seed virtual wallet with 10,000 USDT for the admin user
INSERT INTO virtual_wallets (user_id, balance, version, created_at, updated_at)
SELECT id, 10000.0000, 0, NOW(), NOW()
FROM users WHERE email = 'admin@trading.local'
ON CONFLICT (user_id) DO NOTHING;
