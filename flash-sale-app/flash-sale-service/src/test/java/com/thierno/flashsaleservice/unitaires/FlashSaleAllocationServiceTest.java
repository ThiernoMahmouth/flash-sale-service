package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.entity.PurchaseRequestStatus;
import com.thierno.flashsaleservice.outbox.OutboxService;
import com.thierno.flashsaleservice.repository.FlashSaleRepository;
import com.thierno.flashsaleservice.repository.ProductRepository;
import com.thierno.flashsaleservice.repository.PurchaseRepository;
import com.thierno.flashsaleservice.repository.PurchaseRequestRepository;
import com.thierno.flashsaleservice.service.FlashSaleAllocationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class FlashSaleAllocationServiceTest {

    private FlashSaleAllocationService allocationService;

    @BeforeEach
    void setUp() {
        allocationService = new FlashSaleAllocationService(
                mock(PurchaseRequestRepository.class),
                mock(FlashSaleRepository.class),
                mock(ProductRepository.class),
                mock(PurchaseRepository.class),
                mock(OutboxService.class));
    }

    private FlashSale sale(int totalStock, int soldStock) {
        FlashSale sale = new FlashSale();
        sale.setId(UUID.randomUUID());
        sale.setProductId(UUID.randomUUID());
        sale.setTotalStock(totalStock);
        sale.setSoldStock(soldStock);
        sale.setStartTime(Instant.now().minus(1, ChronoUnit.HOURS));
        sale.setEndTime(Instant.now().plus(1, ChronoUnit.HOURS));
        return sale;
    }

    private PurchaseRequest request(String customerId, int quantity, Instant requestedAt) {
        PurchaseRequest request = new PurchaseRequest();
        request.setId(UUID.randomUUID());
        request.setCustomerId(customerId);
        request.setQuantity(quantity);
        request.setRequestedAt(requestedAt);
        request.setStatus(PurchaseRequestStatus.PENDING);
        return request;
    }

    @Test
    void rank_ordersByRequestedAt_ascending() {
        Instant t0 = Instant.now();
        PurchaseRequest second = request("later", 1, t0.plusSeconds(5));
        PurchaseRequest first = request("earlier", 1, t0);

        List<PurchaseRequest> ranked = allocationService.rank(List.of(second, first));

        assertThat(ranked).containsExactly(first, second);
    }

    @Test
    void allocate_confirmsRequestsUntilStockExhausted_thenRejectsRest() {
        FlashSale sale = sale(3, 0);
        PurchaseRequest a = request("a", 2, Instant.now());
        PurchaseRequest b = request("b", 2, Instant.now().plusSeconds(1));

        List<PurchaseRequest> confirmed = allocationService.allocate(sale, List.of(a, b), false);

        assertThat(confirmed).containsExactly(a);
        assertThat(a.getStatus()).isEqualTo(PurchaseRequestStatus.CONFIRMED);
        assertThat(b.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED_SOLD_OUT);
        assertThat(sale.getSoldStock()).isEqualTo(2);
        assertThat(a.getDecidedAt()).isNotNull();
        assertThat(b.getDecidedAt()).isNotNull();
    }

    @Test
    void allocate_exactStockExhaustion_confirmsExactlyToTheLimit() {
        FlashSale sale = sale(4, 0);
        PurchaseRequest a = request("a", 4, Instant.now());
        PurchaseRequest b = request("b", 1, Instant.now().plusSeconds(1));

        List<PurchaseRequest> confirmed = allocationService.allocate(sale, List.of(a, b), false);

        assertThat(confirmed).containsExactly(a);
        assertThat(sale.remainingStock()).isZero();
        assertThat(b.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED_SOLD_OUT);
    }

    @Test
    void allocate_whenSaleEnded_rejectsEveryPendingRequest() {
        FlashSale sale = sale(10, 0);
        PurchaseRequest a = request("a", 1, Instant.now());

        List<PurchaseRequest> confirmed = allocationService.allocate(sale, List.of(a), true);

        assertThat(confirmed).isEmpty();
        assertThat(a.getStatus()).isEqualTo(PurchaseRequestStatus.REJECTED_SALE_ENDED);
        assertThat(sale.getSoldStock()).isZero();
    }
}
