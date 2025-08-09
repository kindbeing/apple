# System Design

## 🏗️ MicroApple Architecture

### Core Components

#### **CDC Pipeline**
```
Source DBs → Debezium → Kafka → Stream Processing → Iceberg Data Lake
    ↓           ↓        ↓            ↓              ↓
PostgreSQL   Schema   Event      Circuit         REST
MySQL        Registry  Routing    Breakers       Catalog  
MongoDB                Filtering  Rate Limits
H2
```

#### **Event Processing Topology**
```
Kafka Topic: source-events
    ├── Filter Transform (remove irrelevant changes)
    ├── Enrich Transform (add metadata, user context)  
    ├── Route Transform (business priority routing)
    └── Batch Transform (optimize for volume)
         └── Sink: Iceberg Tables
```

### Volume Handling Strategy

| Volume Tier | Events/Sec | Strategy |
|-------------|------------|----------|
| Extreme | >1M | Batch processing, partitioning by user_id hash |
| High | >100K | Circuit breakers, backpressure management |
| Medium | >10K | Real-time processing with rate limiting |
| Low | <1K | Immediate processing, strong consistency |

### Infrastructure Layout

#### **Local Development** (Docker Compose)
- 4x Source Databases (PostgreSQL, MySQL, MongoDB, H2)
- Kafka Cluster (3 brokers) + Schema Registry
- MinIO (S3-compatible storage)
- Spring Boot Applications (CDC processors)

#### **Production-Like** (Kubernetes)
```yaml
# Horizontal scaling targets
CDC Processors: 3-10 pods (auto-scaling)
Kafka Brokers: 3 replicas 
Database Connections: Pool per service
Iceberg REST Catalog: HA deployment
```

### API Endpoints

#### **Test Environment API**
```http
POST /test-environments
  body: { "criteria": "users_with_recent_purchases", "retention": "2h" }
  response: { "environmentId": "test-123", "provisionTime": "90s" }

GET /test-environments/{id}/status
DELETE /test-environments/{id}
```

#### **NLP Query Interface**  
```http
POST /query/natural-language
  body: { "query": "Show users who bought apps after push notifications" }
  response: { "sql": "SELECT ...", "results": [...], "executionTime": "250ms" }
```

### Observability Stack
- **Metrics**: Micrometer → Prometheus
- **Tracing**: Spring Sleuth → Zipkin  
- **Logs**: Structured JSON → ELK Stack
- **Alerts**: Custom SLA violations, circuit breaker states 

---

## ⚙️ Technology Stack

### Backend Framework
- Spring Boot 3.2+
- Spring WebFlux (non-blocking I/O for high throughput)
- Spring Cloud Stream (Kafka integration with backpressure)
- Spring Data JPA + R2DBC (reactive database access)

### Stream Processing
- Apache Kafka (event streaming)
- Kafka Streams (complex event processing, exactly-once semantics)
- Debezium (CDC, schema evolution)
- Apache Avro + Schema Registry (compatibility without breaking changes)

### Data Storage
- Source Databases: PostgreSQL, MySQL, MongoDB, H2
- Apache Iceberg (data lake with time travel, schema evolution)
- MinIO (S3-compatible object storage)
- Iceberg REST Catalog (metadata management)

### Testing & Quality
- Testcontainers (realistic integration testing)
- Spring Boot Test
- Performance testing (100K+ events/sec simulation)

### Observability
- Micrometer + Prometheus (CDC pipeline health)
- Spring Boot Actuator (health checks, circuit breaker monitoring)
- Zipkin (distributed tracing)
- Structured logging (JSON for aggregation)

### Infrastructure
- Docker + Docker Compose (local development)
- Kubernetes (production-like deployment)
- Helm Charts (repeatable deployment automation)

---

## 📊 Technology Justifications

### Why Debezium over Custom CDC?
- Proven at enterprise scale; mature connector ecosystem
- Schema evolution support prevents breaking downstream systems
- Community support lowers maintenance overhead

### Why Circuit Breakers?
- At Apple scale, cascading failures are catastrophic
- Graceful degradation protects revenue-critical services
- Automatic recovery reduces on-call burden

### Why Avro + Schema Registry?
- Apple’s biggest operational pain point is schema evolution
- Backward/forward compatibility prevents breaking changes
- Performance benefits over JSON at high volume

### Why Event Filtering?
- Not all database changes matter for downstream systems
- Intelligent filtering saves compute at scale
- Business logic separation from technical change detection

### Why Spring WebFlux?
- Non-blocking I/O essential for >100K events/sec
- Backpressure prevents memory issues
- Reactive Streams compatibility with Kafka