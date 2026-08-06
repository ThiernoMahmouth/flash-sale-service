package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.dto.CreatePurchaseRequestDto;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.exception.SaleEndedException;
import com.thierno.flashsaleservice.exception.SaleNotStartedException;
import com.thierno.flashsaleservice.exception.SaleSoldOutException;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import com.thierno.flashsaleservice.service.FlashSaleService;
import com.thierno.flashsaleservice.service.PurchaseRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private FlashSaleService flashSaleService;

    @InjectMocks
    private PurchaseRequestService purchaseRequestService;

    private UUID flashSaleId;

    @BeforeEach
    void setUp() {
        flashSaleId = UUID.randomUUID();
        lenient().when(purchaseRequestRepository.save(any(PurchaseRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FlashSale activeSale() {
        FlashSale sale = new FlashSale();
        sale.setId(flashSaleId);
        sale.setTotalStock(10);
        sale.setSoldStock(0);
        sale.setStartTime(Instant.now().minus(1, ChronoUnit.HOURS));
        sale.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        return sale;
    }

    @Test
    void submit_duringActiveWindow_enqueuesPendingRequest() {
        when(flashSaleService.findById(flashSaleId)).thenReturn(activeSale());

        PurchaseRequest saved = purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 2));

        assertThat(saved.getCustomerId()).isEqualTo("cust-1");
        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void submit_beforeStartTime_throwsSaleNotStarted() {
        FlashSale sale = activeSale();
        sale.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(SaleNotStartedException.class);
    }

    @Test
    void submit_afterEndTime_throwsSaleEnded() {
        FlashSale sale = activeSale();
        sale.setStartTime(Instant.now().minus(2, ChronoUnit.HOURS));
        sale.setEndTime(Instant.now().minus(1, ChronoUnit.HOURS));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(SaleEndedException.class);
    }

    @Test
    void submit_whenAlreadySoldOut_throwsSaleSoldOut() {
        FlashSale sale = activeSale();
        sale.setSoldStock(sale.getTotalStock());
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(SaleSoldOutException.class);
    }
}
