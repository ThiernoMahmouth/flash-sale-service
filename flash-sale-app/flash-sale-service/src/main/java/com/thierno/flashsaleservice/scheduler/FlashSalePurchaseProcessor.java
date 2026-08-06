package com.thierno.flashsaleservice.scheduler;

import com.thierno.flashsaleservice.entity.PurchaseRequestStatus;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import com.thierno.flashsaleservice.service.FlashSaleAllocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Periodically drains PENDING purchase requests for every flash sale with pending
 * demand. The actual (transactional) stock allocation lives in
 * {@link FlashSaleAllocationService} - requests arriving in the same public window are
 * batched together on purpose, since it makes "priority" meaningful (stock is awarded
 * by rank, not by network latency) and keeps allocation deterministic/testable instead
 * of racing raw concurrent HTTP requests.
 */
@Component
@RequiredArgsConstructor
public class FlashSalePurchaseProcessor {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FlashSaleAllocationService allocationService;

    @Scheduled(fixedDelayString = "${flash-sale.purchase-processor.fixed-delay-ms:500}")
    public void drainPendingRequests() {
        for (UUID flashSaleId : purchaseRequestRepository.findFlashSaleIdsWithStatus(PurchaseRequestStatus.PENDING)) {
            allocationService.processSale(flashSaleId);
        }
    }
}
