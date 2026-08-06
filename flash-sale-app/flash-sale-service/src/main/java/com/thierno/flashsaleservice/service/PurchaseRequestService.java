package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.dto.CreatePurchaseRequestDto;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.exception.PurchaseRequestNotFoundException;
import com.thierno.flashsaleservice.exception.SaleEndedException;
import com.thierno.flashsaleservice.exception.SaleNotStartedException;
import com.thierno.flashsaleservice.exception.SaleSoldOutException;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestRepository purchaseRequestRepository;
    private final FlashSaleService flashSaleService;

    @Transactional
    public PurchaseRequest submit(UUID flashSaleId, CreatePurchaseRequestDto dto) {
        FlashSale sale = flashSaleService.findById(flashSaleId);
        Instant now = Instant.now();

        validateWindow(sale, now);

        PurchaseRequest request = new PurchaseRequest();
        request.setFlashSaleId(flashSaleId);
        request.setCustomerId(dto.customerId());
        request.setQuantity(dto.quantity());

        PurchaseRequest saved = purchaseRequestRepository.save(request);
        log.info("Enqueued purchase request id={} flashSaleId={} customerId={} quantity={}",
                saved.getId(), flashSaleId, dto.customerId(), dto.quantity());
        return saved;
    }

    private void validateWindow(FlashSale sale, Instant now) {
        switch (sale.statusAt(now)) {
            case SCHEDULED -> throw new SaleNotStartedException(sale.getId(), sale.getStartTime());
            case ENDED -> throw new SaleEndedException(sale.getId());
            case SOLD_OUT -> throw new SaleSoldOutException(sale.getId());
            case ACTIVE -> { /* allowed */ }
        }
    }

    public PurchaseRequest findById(UUID requestId) {
        return purchaseRequestRepository.findById(requestId)
                .orElseThrow(() -> new PurchaseRequestNotFoundException(requestId));
    }
}
