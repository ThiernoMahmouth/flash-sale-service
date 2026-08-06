package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.dto.CreatePurchaseRequestDto;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.MembershipLevel;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.exception.EarlyAccessDeniedException;
import com.thierno.flashsaleservice.exception.PurchaseLimitExceededException;
import com.thierno.flashsaleservice.exception.SaleEndedException;
import com.thierno.flashsaleservice.exception.SaleNotStartedException;
import com.thierno.flashsaleservice.exception.SaleSoldOutException;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import com.thierno.flashsaleservice.service.FlashSaleService;
import com.thierno.flashsaleservice.service.PurchaseRequestService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseRequestServiceTest {

    @Mock
    private PurchaseRequestRepository purchaseRequestRepository;

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FlashSaleService flashSaleService;

    @InjectMocks
    private PurchaseRequestService purchaseRequestService;

    private UUID flashSaleId;

    @BeforeEach
    void setUp() {
        flashSaleId = UUID.randomUUID();
        ReflectionTestUtils.setField(purchaseRequestService, "earlyAccessMinLevel", MembershipLevel.GOLD);
        lenient().when(purchaseRequestRepository.save(any(PurchaseRequest.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    private FlashSale activeSale() {
        FlashSale sale = new FlashSale();
        sale.setId(flashSaleId);
        sale.setTotalStock(10);
        sale.setSoldStock(0);
        sale.setEarlyAccessStart(Instant.now().minus(2, ChronoUnit.HOURS));
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
    void submit_beforeEarlyAccessStart_throwsSaleNotStarted() {
        FlashSale sale = activeSale();
        sale.setEarlyAccessStart(Instant.now().plus(30, ChronoUnit.MINUTES));
        sale.setStartTime(Instant.now().plus(1, ChronoUnit.HOURS));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(SaleNotStartedException.class);
    }

    @Test
    void submit_duringEarlyAccessWindow_asUnregisteredCustomer_throwsEarlyAccessDenied() {
        FlashSale sale = activeSale();
        sale.setEarlyAccessStart(Instant.now().minus(10, ChronoUnit.MINUTES));
        sale.setStartTime(Instant.now().plus(30, ChronoUnit.MINUTES));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);
        when(customerRepository.findById("cust-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(EarlyAccessDeniedException.class);
    }

    @Test
    void submit_duringEarlyAccessWindow_asStandardCustomer_throwsEarlyAccessDenied() {
        FlashSale sale = activeSale();
        sale.setEarlyAccessStart(Instant.now().minus(10, ChronoUnit.MINUTES));
        sale.setStartTime(Instant.now().plus(30, ChronoUnit.MINUTES));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);
        when(customerRepository.findById("cust-1"))
                .thenReturn(Optional.of(new Customer("cust-1", "Alice", MembershipLevel.STANDARD, 0)));

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 1)))
                .isInstanceOf(EarlyAccessDeniedException.class);
    }

    @Test
    void submit_duringEarlyAccessWindow_asGoldCustomer_enqueuesPendingRequest() {
        FlashSale sale = activeSale();
        sale.setEarlyAccessStart(Instant.now().minus(10, ChronoUnit.MINUTES));
        sale.setStartTime(Instant.now().plus(30, ChronoUnit.MINUTES));
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);
        when(customerRepository.findById("cust-vip"))
                .thenReturn(Optional.of(new Customer("cust-vip", "Bob", MembershipLevel.GOLD, 4)));

        PurchaseRequest saved = purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-vip", 1));

        assertThat(saved.getCustomerId()).isEqualTo("cust-vip");
    }

    @Test
    void submit_afterEndTime_throwsSaleEnded() {
        FlashSale sale = activeSale();
        sale.setEarlyAccessStart(Instant.now().minus(3, ChronoUnit.HOURS));
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

    @Test
    void submit_withinPerCustomerLimit_enqueuesRequest() {
        FlashSale sale = activeSale();
        sale.setMaxUnitsPerCustomer(3);
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);
        when(purchaseRequestRepository.sumQuantityByFlashSaleIdAndCustomerIdAndStatusIn(
                eq(flashSaleId), eq("cust-1"), any())).thenReturn(1);

        PurchaseRequest saved = purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 2));

        assertThat(saved.getQuantity()).isEqualTo(2);
    }

    @Test
    void submit_whenExceedingPerCustomerLimit_throwsPurchaseLimitExceeded() {
        FlashSale sale = activeSale();
        sale.setMaxUnitsPerCustomer(3);
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);
        when(purchaseRequestRepository.sumQuantityByFlashSaleIdAndCustomerIdAndStatusIn(
                eq(flashSaleId), eq("cust-1"), any())).thenReturn(2);

        assertThatThrownBy(() -> purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 2)))
                .isInstanceOf(PurchaseLimitExceededException.class);
    }

    @Test
    void submit_whenNoLimitSet_allowsAnyQuantity() {
        FlashSale sale = activeSale();
        sale.setMaxUnitsPerCustomer(null);
        when(flashSaleService.findById(flashSaleId)).thenReturn(sale);

        PurchaseRequest saved = purchaseRequestService.submit(flashSaleId, new CreatePurchaseRequestDto("cust-1", 100));

        assertThat(saved.getQuantity()).isEqualTo(100);
    }
}
