package com.trading.signal.repository;

import com.trading.signal.entity.TradingSignal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradingSignalRepository extends JpaRepository<TradingSignal, Long> {
    List<TradingSignal> findByInstrumentIdOrderByGeneratedAtDesc(Long instrumentId);
}
