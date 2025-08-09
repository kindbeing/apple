# Domain-Driven Design

## 🏗️ MicroApple Domain Model

### Core Bounded Contexts

#### 1. **AppStore Context** (PostgreSQL)
```java
// Domain Events
AppPurchaseEvent, UserReviewEvent, DeveloperAccountEvent

// Aggregates  
Purchase(userId, appId, amount, timestamp)
Review(userId, appId, rating, content, timestamp)
Developer(developerId, accountStatus, apps[])
```

#### 2. **iCloud Context** (MySQL)  
```java
// Domain Events
StorageUsageEvent, SyncEvent, SubscriptionChangeEvent

// Aggregates
StorageUsage(userId, bytesUsed, quotaLimit, timestamp)
SyncEvent(userId, deviceId, dataType, syncStatus, timestamp) 
Subscription(userId, plan, status, renewalDate)
```

#### 3. **Music Context** (MongoDB)
```java
// Domain Events
PlaylistChangeEvent, UserPreferenceEvent, PlayEvent

// Aggregates
Playlist(userId, playlistId, tracks[], lastModified)
UserPreference(userId, genres[], artists[], settings)
PlayEvent(userId, trackId, deviceId, duration, timestamp)
```

#### 4. **Device Context** (H2)
```java
// Domain Events  
DeviceRegistrationEvent, OSUpdateEvent

// Aggregates
Device(deviceId, userId, model, osVersion, registrationDate)
OSUpdate(deviceId, fromVersion, toVersion, status, timestamp)
```

### Cross-Context Integration

#### **CDC Event Router**
- Routes events based on business priority
- Filters irrelevant changes (not all data mutations matter)
- Applies Apple-style event enrichment

#### **Data Lake Unified Schema**
```sql
-- Iceberg table structure
CREATE TABLE unified_events (
  event_type STRING,
  source_context STRING,
  user_id BIGINT,
  event_data JSON,
  event_timestamp TIMESTAMP,
  processed_timestamp TIMESTAMP
) PARTITIONED BY (event_type, source_context, DAY(event_timestamp));
```

### Volume-Based Domain Rules
- **Extreme Volume** (>1M/sec): Batch processing, async handling
- **High Volume** (>100K/sec): Circuit breakers, rate limiting  
- **Medium/Low Volume** (<10K/sec): Real-time processing, immediate consistency 

---

## 🔗 CDC Pipeline Blueprint

### Phase 1: AppStore Service CDC Pipeline
```
┌─────────────────┐    ┌──────────────┐    ┌─────────────┐    ┌─────────────┐
│  Spring Boot    │───▶│ PostgreSQL   │───▶│  Debezium   │───▶│    Kafka    │
│  Application    │    │   Database   │    │  Connect    │    │   Broker    │
│                 │    │              │    │             │    │             │
│ AppPurchase     │    │ app_purchases│    │ CDC Events  │    │ Topic:      │
│ Entity/Repo     │    │ (WAL=logical)│    │ Processor   │    │ microapple- │
│                 │    │              │    │             │    │ postgres... │
└─────────────────┘    └──────────────┘    └─────────────┘    └─────────────┘
     :8080                  :5432               :8083             :9092
```

### Component Details (Phase 1)
- Spring Boot App: AppStore simulator; JPA/Hibernate; Actuator health
- PostgreSQL: WAL=logical; table `app_purchases`
- Debezium: `microapple-postgres-connector`; monitors `public.app_purchases`
- Kafka: topic `microapple-postgres.public.app_purchases`; Debezium JSON

### Data Flow Sequence
1. Write purchase via repository
2. PostgreSQL WAL records change
3. Debezium reads WAL via replication slot
4. Event emitted to Kafka

### Sample CDC Event
```json
{
  "before": null,
  "after": {
    "id": 123,
    "user_id": "user456",
    "app_id": "com.apple.pages",
    "price": "AMc=",
    "transaction_id": "txn-789",
    "purchase_date": 1754260138301609
  },
  "source": {
    "connector": "postgresql",
    "name": "microapple-postgres",
    "schema": "public",
    "table": "app_purchases"
  },
  "op": "c",
  "ts_ms": 1754260138764
}
```

---

## 🧪 Verification & Testing

### Repository Test (AppPurchaseRepositoryTest)
- Verifies data persistence to PostgreSQL

### CDC Integration Test (CdcIntegrationTest)
- Inserts purchase → polls Kafka topic → asserts event contents

### How to Run
- See `docs/timeline-test.md` for end-to-end steps and HTTP requests

---

For infrastructure, observability, and technology choices, see `docs/system-design.md#technology-stack` and `docs/system-design.md#observability-stack`.