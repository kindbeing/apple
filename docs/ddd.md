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