package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.Product;
import com.thierno.flashsaleservice.entity.Purchase;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.entity.PurchaseRequestStatus;
import com.thierno.flashsaleservice.event.PurchaseConfirmedEvent;
import com.thierno.flashsaleservice.outbox.OutboxService;
import com.thierno.flashsaleservice.repository.FlashSaleRepository;
import com.thierno.flashsaleservice.repository.ProductRepository;
import com.thierno.flashsaleservice.repository.PurchaseRepository;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Owns the transactional stock-allocation work for a single flash sale. Kept as its own
 * Spring bean (rather than inline in the {@code @Scheduled} method) so the scheduler can
 * call {@link #processSale(UUID)} through a real proxy - a direct self-invocation from
 * inside the same class would silently skip the {@code @Transactional} advice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleAllocationService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final OutboxService outboxService;

    @Value("${kafka.topics.purchase-confirmed}")
    private String purchaseConfirmedTopic;

    @Transactional
    public void processSale(UUID flashSaleId) {
        FlashSale sale = flashSaleRepository.findByIdForUpdate(flashSaleId).orElse(null);
        if (sale == null) return;

        List<PurchaseRequest> pending =
                purchaseRequestRepository.findByFlashSaleIdAndStatus(flashSaleId, PurchaseRequestStatus.PENDING);
        if (pending.isEmpty()) return;

        List<PurchaseRequest> ranked = rank(pending);
        boolean saleEnded = !Instant.now().isBefore(sale.getEndTime());
        List<PurchaseRequest> confirmed = allocate(sale, ranked, saleEnded);

        BigDecimal unitPrice = productRepository.findById(sale.getProductId())
                .map(Product::getPrice)
                .orElse(BigDecimal.ZERO);

        for (PurchaseRequest request : confirmed) {
            recordPurchaseAndPublish(sale, request, unitPrice);
        }

        purchaseRequestRepository.saveAll(ranked);
        flashSaleRepository.save(sale);
    }

    /**
     * US1 ranking: first-come, first-served. Kept as a seam the priority-access story
     * can replace with a membership/purchase-history comparator without touching the
     * allocation logic below.
     */
    public List<PurchaseRequest> rank(List<PurchaseRequest> pending) {
        return pending.stream()
                .sorted(Comparator.comparing(PurchaseRequest::getRequestedAt))
                .toList();
    }

    /**
     * Pure allocation: walks the already-ranked requests and awards remaining stock
     * until it runs out, mutating request statuses and the sale's soldStock in place.
     * No repositories touched here so it is directly unit-testable.
     */
    public List<PurchaseRequest> allocate(FlashSale sale, List<PurchaseRequest> ranked, boolean saleEnded) {
        Instant now = Instant.now();
        List<PurchaseRequest> confirmed = new ArrayList<>();

        for (PurchaseRequest request : ranked) {
            if (saleEnded) {
                request.setStatus(PurchaseRequestStatus.REJECTED_SALE_ENDED);
            } else if (request.getQuantity() > sale.remainingStock()) {
                request.setStatus(PurchaseRequestStatus.REJECTED_SOLD_OUT);
            } else {
                sale.setSoldStock(sale.getSoldStock() + request.getQuantity());
                request.setStatus(PurchaseRequestStatus.CONFIRMED);
                confirmed.add(request);
            }
            request.setDecidedAt(now);
        }
        return confirmed;
    }

    private void recordPurchaseAndPublish(FlashSale sale, PurchaseRequest request, BigDecimal unitPrice) {
        Purchase purchase = new Purchase();
        purchase.setFlashSaleId(sale.getId());
        purchase.setPurchaseRequestId(request.getId());
        purchase.setCustomerId(request.getCustomerId());
        purchase.setQuantity(request.getQuantity());
        purchase.setUnitPrice(unitPrice);
        Purchase saved = purchaseRepository.save(purchase);

        log.info("Confirmed purchase id={} flashSaleId={} customerId={} quantity={}",
                saved.getId(), sale.getId(), request.getCustomerId(), request.getQuantity());

        outboxService.save(purchaseConfirmedTopic, sale.getId().toString(), new PurchaseConfirmedEvent(
                UUID.randomUUID(), saved.getId(), sale.getId(), request.getId(), request.getCustomerId(),
                request.getQuantity(), unitPrice, saved.getPurchasedAt()));
    }
}
