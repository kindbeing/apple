package com.ncode.microapple.appstore;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ncode.microapple.appstore.entity.AppPurchase;
import com.ncode.microapple.appstore.repository.AppPurchaseRepository;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class CdcIntegrationTest {

    private static final String CDC_TOPIC = "microapple-postgres.public.app_purchases";
    private static final Duration POLL_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration CDC_PROCESSING_DELAY = Duration.ofMillis(3000);
    private static final int MAX_RETRY_ATTEMPTS = 5;

    @Autowired
    private AppPurchaseRepository repository;

    private KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> testTransactionIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        consumer = createKafkaConsumer();
        consumer.subscribe(List.of(CDC_TOPIC));
    }

    private KafkaConsumer<String, String> createKafkaConsumer() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest"); // Process all messages for this consumer group
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");
        
        return new KafkaConsumer<>(props);
    }

    @AfterEach
    void tearDown() {
        cleanupTestData();
        closeConsumer();
    }

    private void cleanupTestData() {
        for (String transactionId : testTransactionIds) {
            repository.findByTransactionId(transactionId)
                .ifPresent(repository::delete);
        }
        testTransactionIds.clear();
    }

    private void closeConsumer() {
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldCapturePurchaseInCDC() throws Exception {
        // Given: Create a test purchase
        TestPurchase testPurchase = createTestPurchase();
        
        // When: Save purchase to trigger CDC event
        repository.save(testPurchase.getAppPurchase());
        
        // Allow time for CDC processing
        sleep(CDC_PROCESSING_DELAY.toMillis());
        
        // Then: Verify CDC event was published to Kafka
        assertCdcEventWasPublished(testPurchase);
    }

    private TestPurchase createTestPurchase() {
        String userId = "user999";
        String appId = "com.apple.numbers";
        BigDecimal price = new BigDecimal("4.99");
        String transactionId = UUID.randomUUID().toString();
        
        testTransactionIds.add(transactionId); // Track for cleanup
        
        AppPurchase purchase = new AppPurchase(userId, appId, price, transactionId);
        return new TestPurchase(purchase, userId, appId, price, transactionId);
    }

    private void assertCdcEventWasPublished(TestPurchase testPurchase) {
        boolean eventFound = false;
        int attempts = 0;
        
        while (!eventFound && attempts < MAX_RETRY_ATTEMPTS) {
            attempts++;
            ConsumerRecords<String, String> records = consumer.poll(POLL_TIMEOUT);
            
            for (ConsumerRecord<String, String> record : records) {
                if (isCdcEventForPurchase(record, testPurchase)) {
                    validateCdcEventContent(record, testPurchase);
                    eventFound = true;
                    break;
                }
            }
            
            if (!eventFound && attempts < MAX_RETRY_ATTEMPTS) {
                sleep(500); // Wait before next poll attempt
            }
        }
        
        assertThat(eventFound)
            .withFailMessage("CDC event for transaction %s not found after %d attempts", 
                testPurchase.getTransactionId(), attempts)
            .isTrue();
    }

    private boolean isCdcEventForPurchase(ConsumerRecord<String, String> record, TestPurchase testPurchase) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            
            if (event.get("after") != null) {
                JsonNode after = event.get("after");
                JsonNode transactionIdNode = after.get("transaction_id");
                
                return transactionIdNode != null && 
                       !transactionIdNode.isNull() && 
                       testPurchase.getTransactionId().equals(transactionIdNode.asText());
            }
        } catch (Exception e) {
            // Skip malformed JSON records
        }
        return false;
    }

    private void validateCdcEventContent(ConsumerRecord<String, String> record, TestPurchase testPurchase) {
        try {
            JsonNode event = objectMapper.readTree(record.value());
            JsonNode after = event.get("after");
            
            assertThat(after.get("user_id").asText()).isEqualTo(testPurchase.getUserId());
            assertThat(after.get("app_id").asText()).isEqualTo(testPurchase.getAppId());
            assertThat(after.get("price")).isNotNull();
            assertThat(event.get("op").asText()).isEqualTo("c"); // CREATE operation
        } catch (Exception e) {
            throw new AssertionError("Failed to validate CDC event content", e);
        }
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Test interrupted", e);
        }
    }

    // Helper class to encapsulate test purchase data
    private static class TestPurchase {
        private final AppPurchase appPurchase;
        private final String userId;
        private final String appId;
        private final BigDecimal price;
        private final String transactionId;

        public TestPurchase(AppPurchase appPurchase, String userId, String appId, BigDecimal price, String transactionId) {
            this.appPurchase = appPurchase;
            this.userId = userId;
            this.appId = appId;
            this.price = price;
            this.transactionId = transactionId;
        }

        public AppPurchase getAppPurchase() { return appPurchase; }
        public String getUserId() { return userId; }
        public String getAppId() { return appId; }
        public BigDecimal getPrice() { return price; }
        public String getTransactionId() { return transactionId; }
    }
}