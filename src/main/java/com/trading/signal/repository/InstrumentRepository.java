package com.trading.signal.repository;

import com.trading.signal.entity.Instrument;
import com.trading.signal.entity.Instrument.InstrumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface InstrumentRepository extends JpaRepository<Instrument, Long> {
    Optional<Instrument> findBySymbol(String symbol);
    boolean existsBySymbol(String symbol);
    List<Instrument> findByActiveTrue();
    List<Instrument> findByType(InstrumentType type);
    List<Instrument> findByActiveTrueAndType(InstrumentType type);
}
