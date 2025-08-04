# Debezium CDC Setup - Complete Technical Breakdown

**CRITICAL**: This document explains what Debezium actually does to your PostgreSQL database when you create a connector.

---

## 🚨 **What We Did vs What Debezium Did Automatically**

### **1. What WE Configured**

**Docker Compose Changes**:
```yaml
postgres:
  command: ["postgres", "-c", "wal_level=logical"]  # We added this
```

**Connector Configuration** (`docker/debezium/postgres-connector.json`):
```json
{
  "name": "microapple-postgres-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432", 
    "database.user": "microapple",
    "database.password": "microapple123",
    "database.dbname": "microapple",
    "topic.prefix": "microapple-postgres",
    "table.include.list": "public.app_purchases",
    "plugin.name": "pgoutput",
    "slot.name": "microapple_slot",                    # We specified this name
    "publication.name": "microapple_publication"       # We specified this name
  }
}
```

**Deployment Command**:
```bash
curl -X POST -H "Content-Type: application/json" \
  --data @docker/debezium/postgres-connector.json \
  http://localhost:8083/connectors
```

### **2. What DEBEZIUM Did Automatically (Behind the Scenes)**

When we posted the connector config, **Debezium executed these PostgreSQL commands**:

```sql
-- 1. Created a replication slot (bookmark in WAL stream)
SELECT pg_create_logical_replication_slot('microapple_slot', 'pgoutput');

-- 2. Created a publication (defines which tables to replicate)  
CREATE PUBLICATION microapple_publication FOR TABLE public.app_purchases;

-- 3. Started reading WAL from the replication slot
START_REPLICATION SLOT microapple_slot LOGICAL 0/0;
```

**RESULT**: PostgreSQL now streams WAL changes to Debezium automatically.

---

## 📊 **How to Verify What Debezium Created**

### **Check Replication Slot**:
```bash
docker exec microapple-postgres psql -U microapple -d microapple -c \
"SELECT slot_name, plugin, slot_type, active FROM pg_replication_slots;"
```

**Expected Output**:
```
    slot_name    |  plugin  | slot_type | active 
-----------------+----------+-----------+--------
 microapple_slot | pgoutput | logical   | t
```

### **Check Publication**:
```bash
docker exec microapple-postgres psql -U microapple -d microapple -c \
"SELECT pubname, schemaname, tablename FROM pg_publication_tables WHERE pubname = 'microapple_publication';"
```

**Expected Output**:
```
        pubname         | schemaname |   tablename   
------------------------+------------+---------------
 microapple_publication | public     | app_purchases
```

### **Check WAL Level**:
```bash
docker exec microapple-postgres psql -U microapple -d microapple -c "SHOW wal_level;"
```

**Expected Output**:
```
 wal_level 
-----------
 logical
```

---

## 🔧 **Complete CDC Data Flow**

### **Step-by-Step Process**:

**1. Application Writes Data**:
```java
AppPurchase purchase = new AppPurchase("user123", "com.apple.pages", 9.99, "txn-456");
repository.save(purchase);  // JPA saves to PostgreSQL
```

**2. PostgreSQL Writes to WAL**:
```sql
-- PostgreSQL automatically writes this transaction to WAL
-- (wal_level=logical includes extra metadata for logical decoding)
INSERT INTO app_purchases (...) VALUES (...);
COMMIT;
```

**3. Debezium Reads WAL via Replication Slot**:
```
- PostgreSQL pgoutput plugin decodes WAL entries into logical changes
- Debezium receives decoded changes through replication slot connection
- Replication slot ensures no changes are lost (even during Debezium restarts)
```

**4. Debezium Publishes to Kafka**:
```json
{
  "before": null,
  "after": {
    "id": 123,
    "user_id": "user123", 
    "app_id": "com.apple.pages",
    "price": "AMc=",
    "transaction_id": "txn-456"
  },
  "source": {
    "connector": "postgresql",
    "name": "microapple-postgres",
    "table": "app_purchases"
  },
  "op": "c",
  "ts_ms": 1754260138764
}
```

**5. Our Test Verifies**:
```java
// Kafka consumer polls topic and finds the event
ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(3));
// Asserts event contains our transaction data
```

---

## 💡 **Interview Talking Points**

### **Q: "How does your CDC system work?"**

**Answer**: 
*"When I deploy the Debezium connector, it creates a PostgreSQL replication slot called 'microapple_slot' and a publication for the app_purchases table. The replication slot acts as a bookmark in PostgreSQL's Write-Ahead Log, ensuring exactly-once delivery. When my Spring Boot app writes data, PostgreSQL's logical decoding plugin converts WAL entries to structured change events, which Debezium streams to Kafka in real-time."*

### **Q: "What's the performance impact?"**

**Answer**:
*"Minimal. PostgreSQL already writes to WAL for ACID compliance. Setting wal_level=logical adds ~1-2% overhead by including extra metadata. The logical decoding happens asynchronously and doesn't block transactions. We can monitor replication slot lag to ensure Debezium keeps up with change volume."*

### **Q: "How do you handle failures?"**

**Answer**:
*"The replication slot guarantees durability. If Debezium goes down, PostgreSQL retains WAL entries until Debezium reconnects and catches up. The slot tracks the last processed WAL position, so we get exactly-once delivery with no data loss."*

---

## 🚨 **Critical Points for Interview**

1. **Debezium automatically configures PostgreSQL** - it's not magic
2. **Replication slots prevent data loss** - key reliability feature  
3. **WAL level must be 'logical'** - required for change data capture
4. **Performance impact is minimal** - WAL overhead is negligible
5. **This scales to Apple-level traffic** - replication slots are enterprise-grade

**DO NOT** say "it just works" or "CDC captures changes automatically" without explaining the replication slot mechanism.

---

## 🛠️ **Cleanup Commands** (if needed)

**Remove Connector**:
```bash
curl -X DELETE http://localhost:8083/connectors/microapple-postgres-connector
```

**Remove PostgreSQL Objects** (Debezium usually cleans up automatically):
```sql
SELECT pg_drop_replication_slot('microapple_slot');
DROP PUBLICATION microapple_publication;
```

---

**Bottom Line**: You now understand exactly what Debezium does to PostgreSQL and can explain the complete technical architecture in an interview.