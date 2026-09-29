package com.trading.signal.repository;

import com.trading.signal.entity.IndicatorSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface IndicatorSnapshotRepository extends JpaRepository<IndicatorSnapshot, Long> {
    
    List<IndicatorSnapshot> findByInstrumentIdAndCandleTimeBetweenOrderByCandleTimeAsc(Long instrumentId, Instant start, Instant end);

    boolean existsByInstrumentIdAndCandleTime(Long instrumentId, Instant candleTime);

    @org.springframework.data.jpa.repository.Query(value = "SELECT s FROM IndicatorSnapshot s WHERE s.instrument.id = :instrumentId AND s.atr14 IS NOT NULL ORDER BY s.candleTime DESC LIMIT 1")
    java.util.Optional<IndicatorSnapshot> findLatestAtr(@org.springframework.data.repository.query.Param("instrumentId") Long instrumentId);
}
