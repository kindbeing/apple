package com.ncode.microapple.appstore;

import com.ncode.microapple.appstore.entity.AppPurchase;
import com.ncode.microapple.appstore.repository.AppPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AppPurchaseRepositoryTest {

    @Autowired
    private AppPurchaseRepository repository;

    @Test
    void shouldPersistAppPurchase() {
        // Given
        String userId = "user123";
        String appId = "com.apple.pages";
        BigDecimal price = new BigDecimal("9.99");
        String transactionId = UUID.randomUUID().toString();
        
        AppPurchase purchase = new AppPurchase(userId, appId, price, transactionId);
        
        // When
        AppPurchase saved = repository.save(purchase);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getAppId()).isEqualTo(appId);
        assertThat(saved.getPrice()).isEqualTo(price);
        assertThat(saved.getTransactionId()).isEqualTo(transactionId);
        assertThat(saved.getPurchaseDate()).isNotNull();
    }

    @Test
    void shouldFindByTransactionId() {
        // Given
        String transactionId = UUID.randomUUID().toString();
        AppPurchase purchase = new AppPurchase("user456", "com.apple.keynote", new BigDecimal("19.99"), transactionId);
        repository.save(purchase);
        
        // When
        var found = repository.findByTransactionId(transactionId);
        
        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getTransactionId()).isEqualTo(transactionId);
    }
}