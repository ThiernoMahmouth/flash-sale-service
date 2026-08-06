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
        if (!request.startTime().isBefore(request.endTime())) {
            throw new InvalidSaleWindowException();
        }

        FlashSale sale = new FlashSale();
        sale.setProductId(request.productId());
        sale.setTotalStock(request.totalStock());
        sale.setSoldStock(0);
        sale.setStartTime(request.startTime());
        sale.setEndTime(request.endTime());

        FlashSale saved = flashSaleRepository.save(sale);
        log.info("Created flash sale id={} productId={} totalStock={} window=[{}, {})",
                saved.getId(), saved.getProductId(), saved.getTotalStock(),
                saved.getStartTime(), saved.getEndTime());
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
