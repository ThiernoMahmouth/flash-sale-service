package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.dto.CreatePurchaseRequestDto;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.MembershipLevel;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.exception.EarlyAccessDeniedException;
import com.thierno.flashsaleservice.exception.PurchaseRequestNotFoundException;
import com.thierno.flashsaleservice.exception.SaleEndedException;
import com.thierno.flashsaleservice.exception.SaleNotStartedException;
import com.thierno.flashsaleservice.exception.SaleSoldOutException;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final CustomerRepository customerRepository;
    private final FlashSaleService flashSaleService;

    @Value("${flash-sale.priority.early-access-min-level}")
    private MembershipLevel earlyAccessMinLevel;

    @Transactional
    public PurchaseRequest submit(UUID flashSaleId, CreatePurchaseRequestDto dto) {
        FlashSale sale = flashSaleService.findById(flashSaleId);
        Instant now = Instant.now();

        validateWindow(sale, dto.customerId(), now);

        PurchaseRequest request = new PurchaseRequest();
        request.setFlashSaleId(flashSaleId);
        request.setCustomerId(dto.customerId());
        request.setQuantity(dto.quantity());

        PurchaseRequest saved = purchaseRequestRepository.save(request);
        log.info("Enqueued purchase request id={} flashSaleId={} customerId={} quantity={}",
                saved.getId(), flashSaleId, dto.customerId(), dto.quantity());
        return saved;
    }

    private void validateWindow(FlashSale sale, String customerId, Instant now) {
        switch (sale.statusAt(now)) {
            case SOLD_OUT -> throw new SaleSoldOutException(sale.getId());
            case ENDED -> throw new SaleEndedException(sale.getId());
            case ACTIVE -> { /* public window, anyone allowed */ }
            case SCHEDULED -> validateEarlyAccess(sale, customerId, now);
        }
    }

    /**
     * Between earlyAccessStart and startTime, only customers at/above the configured
     * membership threshold may purchase; everyone else has to wait for the public
     * window. An unregistered customerId is treated as STANDARD (i.e. denied).
     */
    private void validateEarlyAccess(FlashSale sale, String customerId, Instant now) {
        if (now.isBefore(sale.getEarlyAccessStart())) {
            throw new SaleNotStartedException(sale.getId(), sale.getEarlyAccessStart());
        }

        MembershipLevel level = customerRepository.findById(customerId)
                .map(Customer::getMembershipLevel)
                .orElse(MembershipLevel.STANDARD);

        if (level.ordinal() < earlyAccessMinLevel.ordinal()) {
            throw new EarlyAccessDeniedException(sale.getId(), customerId, earlyAccessMinLevel);
        }
    }

    public PurchaseRequest findById(UUID requestId) {
        return purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(requestId));
    }
}
