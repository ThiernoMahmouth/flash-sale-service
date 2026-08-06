package com.thierno.flashsaleservice.repository;

import com.thierno.flashsaleservice.entity.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PurchaseRepository extends JpaRepository<Purchase, UUID> {
    List<Purchase> findByCustomerId(String customerId);
}
