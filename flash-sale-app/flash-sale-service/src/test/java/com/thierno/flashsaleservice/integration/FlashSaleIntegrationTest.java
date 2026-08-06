package com.thierno.flashsaleservice.integration;

import com.thierno.flashsaleservice.TestcontainersConfiguration;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
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
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
@EmbeddedKafka(partitions = 1, topics = {"flashsale.purchase.confirmed", "flashsale.purchase.rejected"})
@TestPropertySource(properties = {
        "spring.kafka.bootstrap-servers=${spring.embedded.kafka.brokers}",
        "flash-sale.purchase-processor.fixed-delay-ms=3000"
})
class FlashSaleIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    private UUID productId;

    @BeforeEach
    void resolveSeededProduct() {
        Map[] products = restTemplate.getForEntity("/api/products", Map[].class).getBody();
        productId = UUID.fromString((String) products[0].get("id"));
    }

    /** No real early-access window: earlyAccessStart == startTime. */
    private UUID createSale(int totalStock, Instant start, Instant end) {
        return createSale(totalStock, start, start, end);
    }

    private UUID createSale(int totalStock, Instant earlyAccessStart, Instant start, Instant end) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"productId":"%s","totalStock":%d,"earlyAccessStart":"%s","startTime":"%s","endTime":"%s"}
                """.formatted(productId, totalStock, earlyAccessStart, start, end);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/flash-sales", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private UUID createSaleWithLimit(int totalStock, int maxUnitsPerCustomer, Instant start, Instant end) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"productId":"%s","totalStock":%d,"maxUnitsPerCustomer":%d,"earlyAccessStart":"%s","startTime":"%s","endTime":"%s"}
                """.formatted(productId, totalStock, maxUnitsPerCustomer, start, start, end);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/flash-sales", new HttpEntity<>(body, headers), Map.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return UUID.fromString((String) response.getBody().get("id"));
    }

    private ResponseEntity<Map> submitPurchase(UUID saleId, String customerId, int quantity) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String body = """
                {"customerId":"%s","quantity":%d}
                """.formatted(customerId, quantity);
        return restTemplate.postForEntity(
                "/api/flash-sales/" + saleId + "/purchase-requests", new HttpEntity<>(body, headers), Map.class);
    }

    private Map<?, ?> awaitDecision(UUID saleId, UUID requestId) {
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ResponseEntity<Map> response = restTemplate.getForEntity(
                    "/api/flash-sales/" + saleId + "/purchase-requests/" + requestId, Map.class);
            assertThat(response.getBody().get("status")).isNotEqualTo("PENDING");
        });
        return restTemplate.getForEntity(
                "/api/flash-sales/" + saleId + "/purchase-requests/" + requestId, Map.class).getBody();
    }

    /**
     * The embedded broker's topics are shared across every test method in this class, so
     * with an "earliest" offset reset a fresh consumer sees every prior test's records
     * too. Scan (and keep polling) until the record whose key matches this test's
     * aggregate id shows up, rather than trusting the first record read.
     */
    private ConsumerRecord<String, String> awaitRecordForKey(
            Consumer<String, String> consumer, String topic, String key) {
        List<ConsumerRecord<String, String>> seen = new ArrayList<>();
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            KafkaTestUtils.getRecords(consumer, Duration.ofSeconds(1)).forEach(seen::add);
            assertThat(seen).anyMatch(r -> key.equals(r.key()));
        });
        return seen.stream().filter(r -> key.equals(r.key())).findFirst().orElseThrow();
    }

    @Test
    void createFlashSale_thenGetById_returnsSale() {
        Instant start = Instant.now().minus(1, ChronoUnit.MINUTES);
        Instant end = Instant.now().plus(1, ChronoUnit.HOURS);

        UUID saleId = createSale(5, start, end);

        ResponseEntity<Map> getResponse = restTemplate.getForEntity("/api/flash-sales/" + saleId, Map.class);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().get("totalStock")).isEqualTo(5);
        assertThat(getResponse.getBody().get("soldStock")).isEqualTo(0);
    }

    @Test
    void purchase_duringActiveWindow_getsConfirmed_andPublishesRealKafkaEvent() {
        UUID saleId = createSale(5, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS));

        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("flash-it-confirmed", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        consumerProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        consumerProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<String, String>(consumerProps).createConsumer()) {
            embeddedKafkaBroker.consumeFromAnEmbeddedTopic(consumer, "flashsale.purchase.confirmed");

            ResponseEntity<Map> submitResponse = submitPurchase(saleId, "cust-1", 2);
            assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
            UUID requestId = UUID.fromString((String) submitResponse.getBody().get("id"));

            Map<?, ?> decided = awaitDecision(saleId, requestId);
            assertThat(decided.get("status")).isEqualTo("CONFIRMED");

            ConsumerRecord<String, String> record = awaitRecordForKey(consumer, "flashsale.purchase.confirmed", saleId.toString());
            assertThat(record.value()).contains("cust-1").contains(saleId.toString());

            ResponseEntity<Map> saleAfter = restTemplate.getForEntity("/api/flash-sales/" + saleId, Map.class);
            assertThat(saleAfter.getBody().get("soldStock")).isEqualTo(2);
        }
    }

    @Test
    void purchase_beforeEarlyAccessStart_returns409WithProblemDetail() {
        UUID saleId = createSale(5, Instant.now().plus(1, ChronoUnit.HOURS), Instant.now().plus(2, ChronoUnit.HOURS));

        ResponseEntity<Map> response = submitPurchase(saleId, "cust-1", 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "SALE_001");
    }

    @Test
    void purchase_afterEndTime_returns409WithProblemDetail() {
        UUID saleId = createSale(5, Instant.now().minus(2, ChronoUnit.HOURS), Instant.now().minus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> response = submitPurchase(saleId, "cust-1", 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).containsEntry("errorCode", "SALE_002");
    }

    @Test
    void purchase_underStockContention_confirmsOnlyUpToRemainingStock() {
        UUID saleId = createSale(1, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> firstSubmit = submitPurchase(saleId, "cust-a", 1);
        ResponseEntity<Map> secondSubmit = submitPurchase(saleId, "cust-b", 1);
        UUID firstId = UUID.fromString((String) firstSubmit.getBody().get("id"));
        UUID secondId = UUID.fromString((String) secondSubmit.getBody().get("id"));

        Map<?, ?> firstDecided = awaitDecision(saleId, firstId);
        Map<?, ?> secondDecided = awaitDecision(saleId, secondId);

        List<String> statuses = List.of((String) firstDecided.get("status"), (String) secondDecided.get("status"));
        assertThat(statuses).containsExactlyInAnyOrder("CONFIRMED", "REJECTED_SOLD_OUT");

        ResponseEntity<Map> saleAfter = restTemplate.getForEntity("/api/flash-sales/" + saleId, Map.class);
        assertThat(saleAfter.getBody().get("soldStock")).isEqualTo(1);
    }

    @Test
    void purchase_exceedingPerCustomerLimit_returns409WithProblemDetail() {
        UUID saleId = createSaleWithLimit(10, 2, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> first = submitPurchase(saleId, "cust-capped", 2);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<Map> second = submitPurchase(saleId, "cust-capped", 1);

        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(second.getBody()).containsEntry("errorCode", "SALE_006");
    }

    @Test
    void purchase_withinPerCustomerLimit_getsConfirmed() {
        UUID saleId = createSaleWithLimit(10, 2, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> submitResponse = submitPurchase(saleId, "cust-capped", 2);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID requestId = UUID.fromString((String) submitResponse.getBody().get("id"));

        Map<?, ?> decided = awaitDecision(saleId, requestId);
        assertThat(decided.get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    void getFlashSale_whenNotFound_returns404WithProblemDetail() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/flash-sales/" + UUID.randomUUID(), Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("errorCode", "SALE_404");
    }

    @Test
    void purchase_duringEarlyAccessWindow_asStandardCustomer_returns403WithProblemDetail() {
        UUID saleId = createSale(5,
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().plus(30, ChronoUnit.MINUTES),
                Instant.now().plus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> response = submitPurchase(saleId, "cust-standard", 1);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).containsEntry("errorCode", "SALE_005");
    }

    @Test
    void purchase_duringEarlyAccessWindow_asPlatinumCustomer_getsConfirmed() {
        UUID saleId = createSale(5,
                Instant.now().minus(10, ChronoUnit.MINUTES),
                Instant.now().plus(30, ChronoUnit.MINUTES),
                Instant.now().plus(1, ChronoUnit.HOURS));

        ResponseEntity<Map> submitResponse = submitPurchase(saleId, "cust-platinum", 1);
        assertThat(submitResponse.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        UUID requestId = UUID.fromString((String) submitResponse.getBody().get("id"));

        Map<?, ?> decided = awaitDecision(saleId, requestId);
        assertThat(decided.get("status")).isEqualTo("CONFIRMED");
    }

    @Test
    void purchase_underContention_prioritizesHigherMembershipCustomer_overStandardCustomer() {
        UUID saleId = createSale(1, Instant.now().minus(1, ChronoUnit.MINUTES), Instant.now().plus(1, ChronoUnit.HOURS));

        // cust-standard submits first, cust-platinum submits a little later - priority
        // ranking should still hand the single unit of stock to the platinum member.
        ResponseEntity<Map> standardSubmit = submitPurchase(saleId, "cust-standard", 1);
        ResponseEntity<Map> platinumSubmit = submitPurchase(saleId, "cust-platinum", 1);
        UUID standardId = UUID.fromString((String) standardSubmit.getBody().get("id"));
        UUID platinumId = UUID.fromString((String) platinumSubmit.getBody().get("id"));

        Map<?, ?> standardDecided = awaitDecision(saleId, standardId);
        Map<?, ?> platinumDecided = awaitDecision(saleId, platinumId);

        assertThat(platinumDecided.get("status")).isEqualTo("CONFIRMED");
        assertThat(standardDecided.get("status")).isEqualTo("REJECTED_SOLD_OUT");
    }
}
