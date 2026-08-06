package com.thierno.flashsaleservice.integration;

import com.thierno.flashsaleservice.TestcontainersConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Proves the event-driven loyalty flow end to end: a confirmed purchase publishes
 * flashsale.purchase.confirmed via the outbox, PurchaseConfirmedConsumer picks it up
 * (a separate consumer group from any other listener on the same topic), grows
 * purchaseCount, and auto-promotes the membership tier once the threshold is crossed -
 * all of that happening strictly after the purchase-request itself was CONFIRMED,
 * since it depends on the async Kafka round trip rather than the write path.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = {"flashsale.purchase.confirmed", "flashsale.purchase.rejected"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "flash-sale.purchase-processor.fixed-delay-ms=200"
})
class LoyaltyIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    private UUID productId;

    @BeforeEach
    void resolveSeededProduct() {
        Map[] products = restTemplate.getForEntity("/api/products", Map[].class).getBody();
        productId = UUID.fromString((String) products[0].get("id"));
    }

    private UUID createActiveSale(int totalStock) {
        Instant start = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"productId":"%s","totalStock":%d,"earlyAccessStart":"%s","startTime":"%s","endTime":"%s"}
                """.formatted(productId, totalStock, start, start, end);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/flash-sales", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private UUID submitAndAwaitConfirmed(UUID saleId, String customerId) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"customerId":"%s","quantity":1}
                """.formatted(customerId);
        ResponseEntity<Map> submitResponse = restTemplate.postForEntity(
                "/api/flash-sales/" + saleId + "/purchase-requests", new HttpEntity<>(body, headers), Map.class);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID requestId = UUID.fromString((String) submitResponse.getBody().get("id"));

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/flash-sales/" + saleId + "/purchase-requests/" + requestId, Map.class);
            assertThat(response.getBody().get("status")).isEqualTo("CONFIRMED");
        });
        return requestId;
    }

    @Test
    void fiveConfirmedPurchases_growPurchaseHistory_andAutoPromoteToSilver() {
        String customerId = "itest-loyalty-" + UUID.randomUUID();
        UUID saleId = createActiveSale(10);

        for (int i = 0; i < 5; i++) {
            submitAndAwaitConfirmed(saleId, customerId);
        }

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<Map> customer = restTemplate.getForEntity("/api/customers/" + customerId, Map.class);
            assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(customer.getBody().get("purchaseCount")).isEqualTo(5);
            assertThat(customer.getBody().get("membershipLevel")).isEqualTo("SILVER");
        });
    }

    @Test
    void singleConfirmedPurchase_forUnregisteredCustomer_autoCreatesStandardCustomer() {
        String customerId = "itest-loyalty-" + UUID.randomUUID();
        UUID saleId = createActiveSale(5);

        submitAndAwaitConfirmed(saleId, customerId);

        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<Map> customer = restTemplate.getForEntity("/api/customers/" + customerId, Map.class);
            assertThat(customer.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(customer.getBody().get("purchaseCount")).isEqualTo(1);
            assertThat(customer.getBody().get("membershipLevel")).isEqualTo("STANDARD");
        });
    }
}
