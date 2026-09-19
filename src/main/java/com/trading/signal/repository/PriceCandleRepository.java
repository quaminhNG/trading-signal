package com.trading.signal.repository;

import com.trading.signal.entity.PriceCandle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface PriceCandleRepository extends JpaRepository<PriceCandle, Long> {

    /** Keyset pagination: lấy N nến gần nhất trước cursor, sắp xếp mới → cũ */
    @Query("""
        SELECT c FROM PriceCandle c
        WHERE c.instrument.id = :instrumentId
          AND c.timeframe = :timeframe
          AND c.openTime < :before
        ORDER BY c.openTime DESC
        """)
    List<PriceCandle> findCandlesBefore(
            @Param("instrumentId") Long instrumentId,
            @Param("timeframe") String timeframe,
            @Param("before") Instant before,
            org.springframework.data.domain.Pageable pageable);

    /** Lấy nến trong khoảng thời gian */
    @Query("""
        SELECT c FROM PriceCandle c
        WHERE c.instrument.id = :instrumentId
          AND c.timeframe = :timeframe
          AND c.openTime BETWEEN :from AND :to
        ORDER BY c.openTime ASC
        """)
    List<PriceCandle> findCandlesBetween(
            @Param("instrumentId") Long instrumentId,
            @Param("timeframe") String timeframe,
            @Param("from") Instant from,
            @Param("to") Instant to);

    /** Tìm nến gần nhất tại hoặc trước thời điểm T — dùng cho trade_logs tra giá */
    @Query("""
        SELECT c FROM PriceCandle c
        WHERE c.instrument.id = :instrumentId
          AND c.timeframe = :timeframe
          AND c.openTime <= :time
        ORDER BY c.openTime DESC
        LIMIT 1
        """)
    Optional<PriceCandle> findClosestCandle(
            @Param("instrumentId") Long instrumentId,
            @Param("timeframe") String timeframe,
            @Param("time") Instant time);

    boolean existsByInstrumentIdAndTimeframeAndOpenTime(Long instrumentId, String timeframe, Instant openTime);
}
