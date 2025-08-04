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

    @Autowired
    private AppPurchaseRepository repository;

    private KafkaConsumer<String, String> consumer;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final List<String> testTransactionIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092");
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "test-consumer-" + UUID.randomUUID());
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        consumer = new KafkaConsumer<>(props);
        consumer.subscribe(List.of("microapple-postgres.public.app_purchases"));
    }

    @AfterEach
    void tearDown() {
        // Clean up test data
        for (String transactionId : testTransactionIds) {
            repository.findByTransactionId(transactionId)
                .ifPresent(repository::delete);
        }
        testTransactionIds.clear();
        
        if (consumer != null) {
            consumer.close();
        }
    }

    @Test
    void shouldCapturePurchaseInCDC() throws Exception {
        // Given
        String userId = "user999";
        String appId = "com.apple.numbers";
        BigDecimal price = new BigDecimal("4.99");
        String transactionId = UUID.randomUUID().toString();
        testTransactionIds.add(transactionId); // Track for cleanup
        
        AppPurchase purchase = new AppPurchase(userId, appId, price, transactionId);
        
        // When - Insert data into PostgreSQL (this will commit immediately)
        AppPurchase saved = repository.save(purchase);
        
        // Give CDC some time to process and publish the event
        Thread.sleep(2000);
        
        // Then - Check for CDC event in Kafka (retry logic)
        boolean foundMatchingEvent = false;
        int attempts = 0;
        int maxAttempts = 5;
        
        while (!foundMatchingEvent && attempts < maxAttempts) {
            attempts++;
            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
            
            for (ConsumerRecord<String, String> record : records) {
                try {
                    JsonNode event = objectMapper.readTree(record.value());
                    
                    // Look for the change event directly (not nested under "payload")
                    if (event.get("after") != null) {
                        JsonNode after = event.get("after");
                        JsonNode transactionIdNode = after.get("transaction_id");
                        
                        if (transactionIdNode != null && !transactionIdNode.isNull()) {
                            String eventTransactionId = transactionIdNode.asText();
                            
                            if (transactionId.equals(eventTransactionId)) {
                                assertThat(after.get("user_id").asText()).isEqualTo(userId);
                                assertThat(after.get("app_id").asText()).isEqualTo(appId);
                                assertThat(after.get("price")).isNotNull();
                                assertThat(event.get("op").asText()).isEqualTo("c"); // CREATE operation
                                foundMatchingEvent = true;
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Skip malformed JSON records
                    continue;
                }
            }
            
            if (!foundMatchingEvent && attempts < maxAttempts) {
                Thread.sleep(1000); // Wait before next attempt
            }
        }
        
        assertThat(foundMatchingEvent)
            .withFailMessage("CDC event for transaction %s not found in Kafka topic after %d attempts.", transactionId, attempts)
            .isTrue();
    }
}