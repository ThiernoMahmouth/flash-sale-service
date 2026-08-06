package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.Product;
import com.thierno.flashsaleservice.entity.Purchase;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.entity.PurchaseRequestStatus;
import com.thierno.flashsaleservice.event.PurchaseConfirmedEvent;
import com.thierno.flashsaleservice.outbox.OutboxService;
import com.thierno.flashsaleservice.repository.CustomerRepository;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleAllocationService {

    private static final Customer UNKNOWN_CUSTOMER = Customer.newStandard(null);

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FlashSaleRepository flashSaleRepository;
    private final ProductRepository productRepository;
    private final PurchaseRepository purchaseRepository;
    private final CustomerRepository customerRepository;
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

        Map<String, Customer> customersById = loadCustomers(pending);
        List<PurchaseRequest> ranked = rank(pending, customersById);
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

    private Map<String, Customer> loadCustomers(List<PurchaseRequest> pending) {
        List<String> customerIds = pending.stream().map(PurchaseRequest::getCustomerId).distinct().toList();
        return customerRepository.findAllById(customerIds).stream()
                .collect(Collectors.toMap(Customer::getCustomerId, c -> c, (a, b) -> a, HashMap::new));
    }

    public List<PurchaseRequest> rank(List<PurchaseRequest> pending, Map<String, Customer> customersById) {
        Comparator<PurchaseRequest> byMembershipDesc = Comparator.comparingInt(
                (PurchaseRequest r) -> customersById.getOrDefault(r.getCustomerId(), UNKNOWN_CUSTOMER)
                        .getMembershipLevel().ordinal()).reversed();
        Comparator<PurchaseRequest> byPurchaseCountDesc = Comparator.comparingInt(
                (PurchaseRequest r) -> customersById.getOrDefault(r.getCustomerId(), UNKNOWN_CUSTOMER)
                        .getPurchaseCount()).reversed();
        Comparator<PurchaseRequest> byRequestedAtAsc = Comparator.comparing(PurchaseRequest::getRequestedAt);

        return pending.stream()
                .sorted(byMembershipDesc.thenComparing(byPurchaseCountDesc).thenComparing(byRequestedAtAsc))
                .toList();
    }

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
