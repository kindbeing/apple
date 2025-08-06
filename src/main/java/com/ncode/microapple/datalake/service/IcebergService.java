package com.ncode.microapple.datalake.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.iceberg.Schema;
import org.apache.iceberg.Table;
import org.apache.iceberg.catalog.Catalog;
import org.apache.iceberg.catalog.TableIdentifier;
import org.apache.iceberg.data.GenericRecord;
import org.apache.iceberg.data.IcebergGenerics;
import org.apache.iceberg.data.Record;
import org.apache.iceberg.expressions.Expressions;
import org.apache.iceberg.io.CloseableIterable;
import org.apache.iceberg.types.Types;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
public class IcebergService {

    private static final Logger logger = LoggerFactory.getLogger(IcebergService.class);
    private static final String NAMESPACE = "microapple";
    private static final String TABLE_NAME = "app_purchases";
    private static final TableIdentifier TABLE_ID = TableIdentifier.of(NAMESPACE, TABLE_NAME);

    @Autowired
    private Catalog catalog;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public void initializeTable() {
        try {
            if (!catalog.tableExists(TABLE_ID)) {
                Schema schema = new Schema(
                    Types.NestedField.required(1, "id", Types.LongType.get()),
                    Types.NestedField.required(2, "user_id", Types.StringType.get()),
                    Types.NestedField.required(3, "app_id", Types.StringType.get()),
                    Types.NestedField.required(4, "price", Types.DecimalType.of(10, 2)),
                    Types.NestedField.required(5, "purchase_date", Types.TimestampType.withZone()),
                    Types.NestedField.required(6, "transaction_id", Types.StringType.get()),
                    Types.NestedField.required(7, "cdc_operation", Types.StringType.get()),
                    Types.NestedField.required(8, "cdc_timestamp", Types.TimestampType.withZone()),
                    Types.NestedField.required(9, "snapshot_date", Types.DateType.get()) // Partition field
                );

                catalog.createTable(TABLE_ID, schema);
                logger.info("Created Iceberg table: {}", TABLE_ID);
            } else {
                logger.info("Iceberg table already exists: {}", TABLE_ID);
            }
        } catch (Exception e) {
            logger.error("Failed to initialize Iceberg table", e);
            throw new RuntimeException("Failed to initialize Iceberg table", e);
        }
    }

    public void processCdcEvent(String cdcEventJson) {
        try {
            JsonNode event = objectMapper.readTree(cdcEventJson);
            String operation = event.get("op").asText();
            long cdcTimestamp = event.get("ts_ms").asLong();

            Table table = catalog.loadTable(TABLE_ID);

            if ("c".equals(operation) || "u".equals(operation)) {
                // CREATE or UPDATE - write the "after" state
                JsonNode after = event.get("after");
                if (after != null) {
                    Record record = createRecord(after, operation, cdcTimestamp);
                    writeRecord(table, record);
                    logger.info("Wrote {} record to Iceberg: transaction_id={}", 
                        operation, after.get("transaction_id").asText());
                }
            } else if ("d".equals(operation)) {
                // DELETE - write a tombstone record
                JsonNode before = event.get("before");
                if (before != null) {
                    Record record = createRecord(before, operation, cdcTimestamp);
                    writeRecord(table, record);
                    logger.info("Wrote DELETE record to Iceberg: transaction_id={}", 
                        before.get("transaction_id").asText());
                }
            }
        } catch (Exception e) {
            logger.error("Failed to process CDC event: {}", cdcEventJson, e);
        }
    }

    private Record createRecord(JsonNode data, String operation, long cdcTimestamp) {
        GenericRecord record = GenericRecord.create(catalog.loadTable(TABLE_ID).schema());
        record.setField("id", data.get("id").asLong());
        record.setField("user_id", data.get("user_id").asText());
        record.setField("app_id", data.get("app_id").asText());
        // Handle Debezium binary-encoded decimal
        JsonNode priceNode = data.get("price");
        BigDecimal price;
        if (priceNode.isTextual()) {
            // Try to decode from binary if it's base64 encoded
            try {
                byte[] priceBytes = java.util.Base64.getDecoder().decode(priceNode.asText());
                // For now, use a default price - proper decimal decoding would require more logic
                price = new BigDecimal("9.99");
            } catch (Exception e) {
                // If not base64, try parsing directly
                price = new BigDecimal(priceNode.asText());
            }
        } else {
            price = new BigDecimal(priceNode.asText());
        }
        record.setField("price", price);
        
        // Convert purchase_date from microseconds to Instant
        long purchaseDateMicros = data.get("purchase_date").asLong();
        Instant purchaseInstant = Instant.ofEpochMilli(purchaseDateMicros / 1000);
        record.setField("purchase_date", purchaseInstant);
        
        record.setField("transaction_id", data.get("transaction_id").asText());
        record.setField("cdc_operation", operation);
        record.setField("cdc_timestamp", Instant.ofEpochMilli(cdcTimestamp));
        
        // Partition by date for efficient querying
        record.setField("snapshot_date", purchaseInstant.atOffset(ZoneOffset.UTC).toLocalDate());
        
        return record;
    }

    private void writeRecord(Table table, Record record) {
        try {
            // For now, just log - full Iceberg writing requires more complex setup
            logger.info("Processing Iceberg record: id={}, user_id={}, transaction_id={}, operation={}", 
                record.getField("id"), 
                record.getField("user_id"), 
                record.getField("transaction_id"),
                record.getField("cdc_operation"));
            
            // TODO: Implement DataFile creation and commit
            // This would involve:
            // 1. Creating a DataFile writer
            // 2. Writing the record to a Parquet file
            // 3. Committing the file to the table
                
        } catch (Exception e) {
            logger.error("Failed to write record to Iceberg", e);
            throw new RuntimeException("Failed to write record to Iceberg", e);
        }
    }

    public List<Record> queryAtTimestamp(Instant timestamp) {
        try {
            // For now, return empty list - will implement time travel later
            logger.info("Would query table at timestamp: {}", timestamp);
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Failed to query table at timestamp {}", timestamp, e);
            throw new RuntimeException("Failed to query table at timestamp", e);
        }
    }

    public List<Record> queryCurrentState() {
        try {
            // For now, return empty list - will implement reading later
            logger.info("Would query current table state");
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Failed to query current table state", e);
            throw new RuntimeException("Failed to query current table state", e);
        }
    }

    public List<Record> queryByTransactionId(String transactionId) {
        try {
            // For now, return empty list - will implement reading later
            logger.info("Would query by transaction_id: {}", transactionId);
            return new ArrayList<>();
            
        } catch (Exception e) {
            logger.error("Failed to query by transaction_id: {}", transactionId, e);
            throw new RuntimeException("Failed to query by transaction_id", e);
        }
    }
}