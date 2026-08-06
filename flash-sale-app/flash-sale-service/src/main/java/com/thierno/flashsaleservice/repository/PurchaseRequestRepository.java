package com.thierno.flashsaleservice.repository;

import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.entity.PurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PurchaseRequestRepository extends JpaRepository<PurchaseRequest, UUID> {

    List<PurchaseRequest> findByFlashSaleIdAndStatus(UUID flashSaleId, PurchaseRequestStatus status);

    @Query("select distinct r.flashSaleId from PurchaseRequest r where r.status = :status")
    List<UUID> findFlashSaleIdsWithStatus(@Param("status") PurchaseRequestStatus status);
}
