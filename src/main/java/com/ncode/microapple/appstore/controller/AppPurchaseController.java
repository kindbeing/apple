package com.ncode.microapple.appstore.controller;

import com.ncode.microapple.appstore.entity.AppPurchase;
import com.ncode.microapple.appstore.repository.AppPurchaseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchases")
public class AppPurchaseController {

    @Autowired
    private AppPurchaseRepository repository;

    // GET all purchases
    @GetMapping
    public ResponseEntity<List<AppPurchase>> getAllPurchases() {
        List<AppPurchase> purchases = repository.findAll();
        return ResponseEntity.ok(purchases);
    }

    // GET purchase by transaction ID
    @GetMapping("/{transactionId}")
    public ResponseEntity<AppPurchase> getPurchase(@PathVariable String transactionId) {
        Optional<AppPurchase> purchase = repository.findByTransactionId(transactionId);
        return purchase.map(ResponseEntity::ok)
                      .orElse(ResponseEntity.notFound().build());
    }

    // POST create purchases (batch creation)
    @PostMapping
    public ResponseEntity<BatchCreateResponse> createPurchases(
            @RequestParam(defaultValue = "1") int count,
            @RequestParam(defaultValue = "user999") String userId,
            @RequestParam(defaultValue = "com.apple.numbers") String appId,
            @RequestParam(defaultValue = "4.99") String price) {
        
        List<AppPurchase> createdPurchases = new ArrayList<>();
        BigDecimal purchasePrice = new BigDecimal(price);
        
        for (int i = 0; i < count; i++) {
            String transactionId = UUID.randomUUID().toString();
            String userIdWithSuffix = count > 1 ? userId + "-" + (i + 1) : userId;
            
            AppPurchase purchase = new AppPurchase(userIdWithSuffix, appId, purchasePrice, transactionId);
            AppPurchase saved = repository.save(purchase);
            createdPurchases.add(saved);
        }
        
        return ResponseEntity.ok(new BatchCreateResponse(
            createdPurchases.size(), 
            createdPurchases.stream().map(AppPurchase::getTransactionId).toList(),
            "Successfully created " + count + " purchase(s)"
        ));
    }

    // PUT update purchase
    @PutMapping("/{transactionId}")
    public ResponseEntity<AppPurchase> updatePurchase(
            @PathVariable String transactionId,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String appId,
            @RequestParam(required = false) String price) {
        
        Optional<AppPurchase> existingPurchaseOpt = repository.findByTransactionId(transactionId);
        if (existingPurchaseOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        AppPurchase existingPurchase = existingPurchaseOpt.get();
        
        // Create new purchase with updated values (since AppPurchase fields are final)
        String newUserId = userId != null ? userId : existingPurchase.getUserId();
        String newAppId = appId != null ? appId : existingPurchase.getAppId();
        BigDecimal newPrice = price != null ? new BigDecimal(price) : existingPurchase.getPrice();
        
        // Delete old record and create new one (to trigger CDC delete + create)
        repository.delete(existingPurchase);
        AppPurchase updatedPurchase = new AppPurchase(newUserId, newAppId, newPrice, transactionId);
        AppPurchase saved = repository.save(updatedPurchase);
        
        return ResponseEntity.ok(saved);
    }

    // DELETE specific purchase
    @DeleteMapping("/{transactionId}")
    public ResponseEntity<DeleteResponse> deletePurchase(@PathVariable String transactionId) {
        Optional<AppPurchase> purchase = repository.findByTransactionId(transactionId);
        if (purchase.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        
        repository.delete(purchase.get());
        return ResponseEntity.ok(new DeleteResponse(
            1, 
            List.of(transactionId),
            "Successfully deleted purchase with transaction ID: " + transactionId
        ));
    }

    // DELETE all purchases (cleanup)
    @DeleteMapping
    public ResponseEntity<DeleteResponse> deleteAllPurchases() {
        List<AppPurchase> allPurchases = repository.findAll();
        List<String> transactionIds = allPurchases.stream()
                                                  .map(AppPurchase::getTransactionId)
                                                  .toList();
        
        repository.deleteAll();
        
        return ResponseEntity.ok(new DeleteResponse(
            allPurchases.size(),
            transactionIds,
            "Successfully deleted " + allPurchases.size() + " purchase(s)"
        ));
    }

    // Response DTOs
    public static class BatchCreateResponse {
        public final int count;
        public final List<String> transactionIds;
        public final String message;

        public BatchCreateResponse(int count, List<String> transactionIds, String message) {
            this.count = count;
            this.transactionIds = transactionIds;
            this.message = message;
        }
    }

    public static class DeleteResponse {
        public final int count;
        public final List<String> transactionIds;
        public final String message;

        public DeleteResponse(int count, List<String> transactionIds, String message) {
            this.count = count;
            this.transactionIds = transactionIds;
            this.message = message;
        }
    }
}