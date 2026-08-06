package com.thierno.flashsaleservice.unitaires;

import com.thierno.flashsaleservice.consumer.PurchaseConfirmedConsumer;
import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.entity.MembershipLevel;
import com.thierno.flashsaleservice.event.PurchaseConfirmedEvent;
import com.thierno.flashsaleservice.idempotency.IdempotencyService;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PurchaseConfirmedConsumerTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private IdempotencyService idempotencyService;

    @InjectMocks
    private PurchaseConfirmedConsumer consumer;

    private PurchaseConfirmedEvent event(UUID eventId, String customerId) {
        return new PurchaseConfirmedEvent(eventId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                customerId, 1, BigDecimal.valueOf(19.99), Instant.now());
    }

    @Test
    void onPurchaseConfirmed_forUnregisteredCustomer_createsStandardCustomerAndIncrements() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyService.isDuplicate(eventId)).thenReturn(false);
        when(customerRepository.findById("new-customer")).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.onPurchaseConfirmed(event(eventId, "new-customer"), 0L);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getCustomerId()).isEqualTo("new-customer");
        assertThat(captor.getValue().getMembershipLevel()).isEqualTo(MembershipLevel.STANDARD);
        assertThat(captor.getValue().getPurchaseCount()).isEqualTo(1);
        verify(idempotencyService).markProcessed(eventId, "PURCHASE_CONFIRMED");
    }

    @Test
    void onPurchaseConfirmed_crossingPromotionThreshold_promotesTier() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyService.isDuplicate(eventId)).thenReturn(false);
        when(customerRepository.findById("cust-1"))
                .thenReturn(Optional.of(new Customer("cust-1", "Alice", MembershipLevel.STANDARD, 4)));
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        consumer.onPurchaseConfirmed(event(eventId, "cust-1"), 0L);

        ArgumentCaptor<Customer> captor = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(captor.capture());
        assertThat(captor.getValue().getPurchaseCount()).isEqualTo(5);
        assertThat(captor.getValue().getMembershipLevel()).isEqualTo(MembershipLevel.SILVER);
    }

    @Test
    void onPurchaseConfirmed_duplicateEvent_isSkipped() {
        UUID eventId = UUID.randomUUID();
        when(idempotencyService.isDuplicate(eventId)).thenReturn(true);

        consumer.onPurchaseConfirmed(event(eventId, "cust-1"), 0L);

        verify(customerRepository, never()).save(any());
        verify(idempotencyService, never()).markProcessed(any(), any());
    }
}
