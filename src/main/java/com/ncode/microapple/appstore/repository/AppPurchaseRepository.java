package com.ncode.microapple.appstore.repository;

import com.ncode.microapple.appstore.entity.AppPurchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppPurchaseRepository extends JpaRepository<AppPurchase, Long> {

    Optional<AppPurchase> findByTransactionId(String transactionId);

}