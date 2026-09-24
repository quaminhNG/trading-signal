package com.trading.signal.strategy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/**
 * Kết quả đánh giá từ strategy — chứa đầy đủ metadata để RiskManager quyết định.
 */
public record SignalCandidate(
        /** BUY hoặc SELL */
        String type,
        /** Thời điểm của nến tạo signal */
        Instant candleTime,
        /** Điểm tự tin 0-100, tính toán thực sự (không hardcode) */
        BigDecimal confidence,
        /** Tên pattern/rule đã kích hoạt */
        String pattern,
        /** Lý do chi tiết */
        String reason,
        /** Metadata chỉ báo tại thời điểm signal (RSI, MA, MACD...) */
        Map<String, BigDecimal> indicators
) {
    /**
     * Signal có đủ mạnh để trade không? Threshold mặc định 60.
     */
    public boolean isActionable() {
        return confidence.compareTo(new BigDecimal("60")) >= 0;
    }
}
