package com.thierno.flashsaleservice.service;

import com.thierno.flashsaleservice.dto.CreateFlashSaleRequest;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.exception.FlashSaleNotFoundException;
import com.thierno.flashsaleservice.exception.InvalidSaleWindowException;
import com.thierno.flashsaleservice.repository.FlashSaleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlashSaleService {

    private final FlashSaleRepository flashSaleRepository;

    @Transactional
    public FlashSale create(CreateFlashSaleRequest request) {
        boolean validWindow = !request.earlyAccessStart().isAfter(request.startTime())
                && request.startTime().isBefore(request.endTime());
        if (!validWindow) {
            throw new InvalidSaleWindowException();
        }

        FlashSale sale = new FlashSale();
        sale.setProductId(request.productId());
        sale.setTotalStock(request.totalStock());
        sale.setSoldStock(0);
        sale.setEarlyAccessStart(request.earlyAccessStart());
        sale.setStartTime(request.startTime());
        sale.setEndTime(request.endTime());
        sale.setMaxUnitsPerCustomer(request.maxUnitsPerCustomer());

        FlashSale saved = flashSaleRepository.save(sale);
        log.info("Created flash sale id={} productId={} totalStock={} earlyAccess={} window=[{}, {})",
                saved.getId(), saved.getProductId(), saved.getTotalStock(),
                saved.getEarlyAccessStart(), saved.getStartTime(), saved.getEndTime());
        return saved;
    }

    public FlashSale findById(UUID id) {
        return flashSaleRepository.findById(id)
                .orElseThrow(() -> new FlashSaleNotFoundException(id));
    }

    public List<FlashSale> findAll() {
        return flashSaleRepository.findAll();
    }
}
