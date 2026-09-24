package com.trading.signal.strategy;

import com.trading.signal.entity.IndicatorSnapshot;

import java.util.List;

/**
 * Interface cho mọi trading strategy.
 * Cho phép plug-in ML/DL strategy trong tương lai — chỉ cần implement interface này.
 */
public interface TradingStrategy {

    /**
     * Đánh giá chuỗi indicator snapshots và trả về danh sách signal candidates.
     * Walk-forward: chỉ dùng dữ liệu đã có tại thời điểm đánh giá (không lookahead).
     *
     * @param snapshots danh sách indicator snapshots, sắp xếp theo thời gian tăng dần
     * @return danh sách signal candidates (có thể rỗng)
     */
    List<SignalCandidate> evaluate(List<IndicatorSnapshot> snapshots);

    /** Tên strategy để log/display */
    String getName();
}
