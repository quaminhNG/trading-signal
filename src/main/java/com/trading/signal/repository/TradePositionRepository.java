package com.trading.signal.repository;

import com.trading.signal.entity.TradePosition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TradePositionRepository extends JpaRepository<TradePosition, Long> {
    List<TradePosition> findByWalletId(Long walletId);
    Optional<TradePosition> findByWalletIdAndInstrumentId(Long walletId, Long instrumentId);
}
