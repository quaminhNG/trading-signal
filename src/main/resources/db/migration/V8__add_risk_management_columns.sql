-- Thêm chỉ báo ATR, ADX, Bollinger Bands cho chiến lược Anti-Loss
-- ATR: dùng tính stop-loss dynamic
-- ADX: phát hiện sideways market, tránh false signal (whipsaw)
-- Bollinger Bands: phát hiện volatility spike

ALTER TABLE indicator_snapshots ADD COLUMN IF NOT EXISTS atr_14 DECIMAL(18,8);
ALTER TABLE indicator_snapshots ADD COLUMN IF NOT EXISTS adx_14 DECIMAL(10,4);
ALTER TABLE indicator_snapshots ADD COLUMN IF NOT EXISTS bb_upper DECIMAL(18,8);
ALTER TABLE indicator_snapshots ADD COLUMN IF NOT EXISTS bb_middle DECIMAL(18,8);
ALTER TABLE indicator_snapshots ADD COLUMN IF NOT EXISTS bb_lower DECIMAL(18,8);

-- Thêm stop-loss / take-profit / peak tracking cho positions
ALTER TABLE trade_positions ADD COLUMN IF NOT EXISTS stop_loss_price DECIMAL(19,8);
ALTER TABLE trade_positions ADD COLUMN IF NOT EXISTS take_profit_price DECIMAL(19,8);
ALTER TABLE trade_positions ADD COLUMN IF NOT EXISTS peak_price DECIMAL(19,8);
ALTER TABLE trade_positions ADD COLUMN IF NOT EXISTS confidence DECIMAL(5,2);
