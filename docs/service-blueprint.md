# MicroApple Service Blueprint

**Status**: Phase 1 Implementation Complete  
**Last Updated**: 2024-12-28  
**Components**: PostgreSQL CDC → Kafka Pipeline

---

## 🎯 **Current Implementation Overview**

MicroApple demonstrates Apple-scale Change Data Capture (CDC) architecture with a complete PostgreSQL → Debezium → Kafka pipeline, simulating AppStore purchase event streaming.

## 🏗️ **Service Architecture**

### **Phase 1: AppStore Service CDC Pipeline**
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

## 📊 **Component Details**

### **1. Spring Boot Application**
- **Role**: AppStore Service Simulator
- **Port**: 8080
- **Database**: PostgreSQL via JPA/Hibernate
- **Key Components**:
  - `AppPurchase` entity (id, userId, appId, price, transactionId)
  - `AppPurchaseRepository` for CRUD operations
  - Health checks via Spring Actuator (`/actuator/health`)

### **2. PostgreSQL Database**
- **Role**: Transactional Data Store
- **Port**: 5432
- **Configuration**: 
  - WAL level: `logical` (enables CDC)
  - Database: `microapple`
  - Table: `app_purchases`
- **CDC Source**: All INSERT/UPDATE/DELETE operations

### **3. Debezium Connect**
- **Role**: CDC Event Processor
- **Port**: 8083
- **Connector**: `microapple-postgres-connector`
- **Monitors**: `public.app_purchases` table
- **Output**: JSON change events to Kafka

### **4. Kafka Infrastructure**
- **Zookeeper**: Port 2181 (coordination)
- **Kafka Broker**: Port 9092 (messaging)
- **Topic**: `microapple-postgres.public.app_purchases`
- **Event Format**: Debezium JSON with `before`/`after` states

## 🔄 **Data Flow Sequence**

### **Purchase Event Pipeline**
```
1. POST /api/purchases (Future) │ Test inserts AppPurchase
   ↓
2. Spring Boot App saves to PostgreSQL
   ↓
3. PostgreSQL writes to WAL (Write-Ahead Log)
   ↓
4. Debezium reads WAL changes
   ↓
5. Debezium publishes CDC event to Kafka
   ↓
6. Event available for downstream consumers
```

### **Sample CDC Event**
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

## 🧪 **Verification & Testing**

### **Repository Test** (`AppPurchaseRepositoryTest`)
- **Purpose**: Verify data persistence to PostgreSQL
- **Method**: JPA save/find operations
- **Result**: ✅ Data correctly stored and retrieved

### **CDC Integration Test** (`CdcIntegrationTest`)
- **Purpose**: End-to-end CDC pipeline verification
- **Method**: 
  1. Insert AppPurchase via repository
  2. Poll Kafka topic for CDC event
  3. Assert event contains expected data
- **Result**: ✅ CDC events successfully captured

## 🚀 **Apple Interview Relevance**

### **Scale Preparation**
- **Current Capacity**: Handles individual transactions
- **Apple Scale Target**: 100K+ events/sec simulation ready
- **Scaling Strategy**: Kafka partitioning + horizontal Debezium connectors

### **Production Patterns**
- **Schema Evolution**: Debezium handles PostgreSQL schema changes
- **Event Ordering**: Kafka partitioning preserves order per user
- **Failure Recovery**: PostgreSQL WAL ensures no lost events
- **Monitoring**: Health checks + Actuator metrics

## 📋 **Infrastructure Commands**

### **Start Environment**
```bash
docker-compose up -d
curl http://localhost:8083/connectors  # Verify Debezium
./gradlew bootRun                      # Start Spring Boot
```

### **Test CDC Pipeline**
```bash
./gradlew test --tests "CdcIntegrationTest"
./gradlew test --tests "AppPurchaseRepositoryTest"
```

### **Monitor Events**
```bash
# View Kafka topics
docker exec microapple-kafka kafka-topics --bootstrap-server localhost:9092 --list

# Consume CDC events
docker exec microapple-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic microapple-postgres.public.app_purchases \
  --from-beginning
```

## 📈 **Next Phase: Multi-Database CDC**

### **Planned Expansion**
- **MySQL**: iCloud Service simulation (user data sync)
- **MongoDB**: Music Service simulation (playlist changes)
- **H2**: Device Service simulation (registration events)

### **Advanced Features**
- **Event Filtering**: Business logic-based filtering
- **Schema Registry**: Avro schema evolution
- **Stream Processing**: Kafka Streams aggregations
- **Data Lake**: Apache Iceberg integration

---

**Current Status**: ✅ **Production-Ready PostgreSQL CDC Pipeline**  
**Interview Demo**: Ready to show end-to-end change capture with verification  
**Next Milestone**: Add second database (MySQL) for heterogeneous CDC demonstration