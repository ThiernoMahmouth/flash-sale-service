package com.thierno.flashsaleservice.consumer;

import com.thierno.flashsaleservice.entity.Customer;
import com.thierno.flashsaleservice.event.PurchaseConfirmedEvent;
import com.thierno.flashsaleservice.idempotency.IdempotencyService;
import com.thierno.flashsaleservice.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PurchaseConfirmedConsumer {

    private final CustomerRepository customerRepository;
    private final IdempotencyService idempotencyService;

    @KafkaListener(
            topics = "${kafka.topics.purchase-confirmed}",
            groupId = "flash-sale-loyalty-group",
            containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void onPurchaseConfirmed(
            @Payload PurchaseConfirmedEvent event,
            @Header(KafkaHeaders.OFFSET) long offset) {

        if (idempotencyService.isDuplicate(event.eventId())) {
            log.debug("Skipping duplicate purchase-confirmed event id={}", event.eventId());
            return;
        }

        Customer customer = customerRepository.findById(event.customerId())
                .orElseGet(() -> Customer.newStandard(event.customerId()));
        customer.setPurchaseCount(customer.getPurchaseCount() + 1);
        boolean promoted = customer.promoteIfEligible();
        customerRepository.save(customer);

        idempotencyService.markProcessed(event.eventId(), "PURCHASE_CONFIRMED");

        log.info("customerId={} purchaseCount={} membershipLevel={} promoted={} | offset={}",
                customer.getCustomerId(), customer.getPurchaseCount(), customer.getMembershipLevel(),
                promoted, offset);
    }
}
