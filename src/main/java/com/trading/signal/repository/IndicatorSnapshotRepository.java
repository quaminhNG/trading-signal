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
}
