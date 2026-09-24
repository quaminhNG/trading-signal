package com.trading.signal.repository;

import com.trading.signal.entity.VirtualWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VirtualWalletRepository extends JpaRepository<VirtualWallet, Long> {
    Optional<VirtualWallet> findByUserId(Long userId);
}
