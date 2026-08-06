package com.thierno.flashsaleservice.repository;

import com.thierno.flashsaleservice.entity.FlashSale;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface FlashSaleRepository extends JpaRepository<FlashSale, UUID> {

    /**
     * Pessimistic write lock so the scheduled purchase processor can safely allocate
     * remaining stock even if two processing runs ever overlap.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FlashSale f where f.id = :id")
    Optional<FlashSale> findByIdForUpdate(@Param("id") UUID id);
}
