# Technology Stack

## 🎯 Core Technologies (Apple-Grade Choices)

### **Backend Framework**
- **Spring Boot 3.2+** - Apple values enterprise stability
- **Spring WebFlux** - Non-blocking I/O for high throughput (>100K events/sec)
- **Spring Cloud Stream** - Kafka integration with backpressure support
- **Spring Data JPA + R2DBC** - Reactive database access

### **Stream Processing**  
- **Apache Kafka** - Industry standard, Apple's choice for event streaming
- **Kafka Streams** - Complex event processing, exactly-once semantics
- **Debezium** - Proven CDC solution, handles schema evolution
- **Apache Avro + Schema Registry** - Schema evolution without breaking changes

### **Data Storage**
- **Source Databases**: PostgreSQL, MySQL, MongoDB, H2 (diversity simulation)
- **Apache Iceberg** - Data lake with time travel, schema evolution
- **MinIO** - S3-compatible object storage for development
- **REST Catalog** - Iceberg metadata management

### **Testing & Quality**
- **Testcontainers** - Realistic integration testing with real databases
- **Spring Boot Test** - Comprehensive testing framework
- **Performance Testing** - Load testing at 100K+ events/sec simulation

### **Observability**
- **Micrometer + Prometheus** - Custom metrics for CDC pipeline health
- **Spring Boot Actuator** - Health checks, circuit breaker monitoring
- **Zipkin** - Distributed tracing across service boundaries
- **Structured Logging** - JSON format for centralized log aggregation

### **Infrastructure**
- **Docker + Docker Compose** - Local development environment
- **Kubernetes** - Production-like deployment patterns
- **Helm Charts** - Repeatable deployment automation

## 📊 Technology Justifications (Interview Talking Points)

### **Why Debezium over Custom CDC?**
- Proven at enterprise scale, handles edge cases Apple encounters
- Schema evolution support prevents deployment freezes
- Community support reduces maintenance overhead

### **Why Circuit Breakers?**  
- At Apple scale, cascading failures are catastrophic
- Graceful degradation protects revenue-critical services
- Automatic recovery reduces on-call burden

### **Why Avro + Schema Registry?**
- Schema evolution is Apple's biggest operational pain point
- Backward/forward compatibility prevents breaking changes
- Performance benefits over JSON at high volume

### **Why Event Filtering?**
- Not all database changes matter for downstream systems
- Intelligent filtering saves compute resources at scale
- Business logic separation from technical change detection

### **Why Spring WebFlux?**
- Non-blocking I/O essential for >100K events/sec
- Backpressure handling prevents memory issues
- Reactive Streams compatibility with Kafka 