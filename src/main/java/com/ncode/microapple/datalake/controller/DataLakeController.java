package com.ncode.microapple.datalake.controller;

import com.ncode.microapple.datalake.service.IcebergService;
import org.apache.iceberg.data.Record;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/datalake")
public class DataLakeController {

    private static final Logger logger = LoggerFactory.getLogger(DataLakeController.class);

    @Autowired
    private IcebergService icebergService;

    // Initialize Iceberg table
    @PostMapping("/init")
    public ResponseEntity<String> initializeTable() {
        try {
            icebergService.initializeTable();
            return ResponseEntity.ok("Iceberg table initialized successfully");
        } catch (Exception e) {
            logger.error("Failed to initialize table", e);
            return ResponseEntity.internalServerError()
                .body("Failed to initialize table: " + e.getMessage());
        }
    }

    // Query current state of all purchases
    @GetMapping("/purchases")
    public ResponseEntity<List<PurchaseRecord>> getCurrentPurchases() {
        try {
            List<Record> records = icebergService.queryCurrentState();
            List<PurchaseRecord> purchases = records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(purchases);
        } catch (Exception e) {
            logger.error("Failed to query current purchases", e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Time travel query - show purchases as they were at a specific time
    @GetMapping("/purchases/at/{timestamp}")
    public ResponseEntity<TimeTravelResponse> getPurchasesAtTimestamp(@PathVariable String timestamp) {
        try {
            // Parse timestamp (ISO format: 2024-12-28T15:30:00Z)
            Instant instant = Instant.parse(timestamp);
            
            List<Record> records = icebergService.queryAtTimestamp(instant);
            List<PurchaseRecord> purchases = records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(new TimeTravelResponse(
                timestamp,
                purchases.size(),
                purchases,
                "Data as it existed at " + timestamp
            ));
        } catch (Exception e) {
            logger.error("Failed to query purchases at timestamp: {}", timestamp, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Query specific transaction across all time
    @GetMapping("/purchases/transaction/{transactionId}")
    public ResponseEntity<List<PurchaseRecord>> getTransactionHistory(@PathVariable String transactionId) {
        try {
            List<Record> records = icebergService.queryByTransactionId(transactionId);
            List<PurchaseRecord> history = records.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
            
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            logger.error("Failed to query transaction history for: {}", transactionId, e);
            return ResponseEntity.internalServerError().build();
        }
    }

    // Helper endpoint to get current timestamp for time travel testing
    @GetMapping("/timestamp/now")
    public ResponseEntity<String> getCurrentTimestamp() {
        return ResponseEntity.ok(Instant.now().toString());
    }

    // Helper endpoint to get timestamp from minutes ago
    @GetMapping("/timestamp/minutes-ago/{minutes}")
    public ResponseEntity<String> getTimestampMinutesAgo(@PathVariable int minutes) {
        Instant past = Instant.now().minusSeconds(minutes * 60L);
        return ResponseEntity.ok(past.toString());
    }

    private PurchaseRecord convertToDto(Record record) {
        return new PurchaseRecord(
            (Long) record.getField("id"),
            (String) record.getField("user_id"),
            (String) record.getField("app_id"),
            record.getField("price").toString(),
            record.getField("purchase_date").toString(),
            (String) record.getField("transaction_id"),
            (String) record.getField("cdc_operation"),
            record.getField("cdc_timestamp").toString()
        );
    }

    // DTOs
    public static class PurchaseRecord {
        public final Long id;
        public final String userId;
        public final String appId;
        public final String price;
        public final String purchaseDate;
        public final String transactionId;
        public final String cdcOperation;
        public final String cdcTimestamp;

        public PurchaseRecord(Long id, String userId, String appId, String price, 
                            String purchaseDate, String transactionId, 
                            String cdcOperation, String cdcTimestamp) {
            this.id = id;
            this.userId = userId;
            this.appId = appId;
            this.price = price;
            this.purchaseDate = purchaseDate;
            this.transactionId = transactionId;
            this.cdcOperation = cdcOperation;
            this.cdcTimestamp = cdcTimestamp;
        }
    }

    public static class TimeTravelResponse {
        public final String queriedTimestamp;
        public final int recordCount;
        public final List<PurchaseRecord> records;
        public final String description;

        public TimeTravelResponse(String queriedTimestamp, int recordCount, 
                                List<PurchaseRecord> records, String description) {
            this.queriedTimestamp = queriedTimestamp;
            this.recordCount = recordCount;
            this.records = records;
            this.description = description;
        }
    }
}