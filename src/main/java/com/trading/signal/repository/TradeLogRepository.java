package com.trading.signal.repository;

import com.trading.signal.entity.TradeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TradeLogRepository extends JpaRepository<TradeLog, Long> {
    List<TradeLog> findByWalletIdOrderByCreatedAtDesc(Long walletId);
}
