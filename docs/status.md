# 📊 MicroApple Project Status

**Last Updated**: `2025-08-10`  
**Current Phase**: `AppStore CRUD + CDC validation in place; Iceberg sink configured (write path stubbed)`  
**Next Session Goal**: `Implement Iceberg write path + add Actuator/metrics`

---

## 🎯 Current State

### ✅ **COMPLETED**
- [x] Documentation structure created (roadmap, ddd, system-design)
- [x] Problem definition and scope established  
- [x] Technical stack decisions documented
- [x] MicroApple ecosystem defined (4 services, volume targets)
- [x] Success criteria and interview targets set
- [x] **ADR-001: Project Foundation & Build System** ✨
- [x] **Spring Boot 3.5.4 + Java 21 Corretto project scaffolding** ✨
- [x] **Gradle 8.14.3 with auto-provisioning toolchains** ✨
- [x] **Package structure: com.ncode.microapple** ✨
- [x] **Build verification: `./gradlew bootRun` working** ✨
- [x] AppStore CRUD API (`AppPurchaseController`) implemented
- [x] Repository tests green (`AppPurchaseRepositoryTest`)
- [x] CDC integration test scaffold (`CdcIntegrationTest`) polling Debezium topic
- [x] Iceberg REST Catalog configured (`IcebergConfig`); table init endpoint exposed

### 🔄 **IN PROGRESS**
- [ ] CDC → Iceberg sink: parse Debezium event and persist (service stub in place)
- [ ] Docker Compose environment alignment (Postgres, Kafka, Debezium, MinIO, REST catalog)

### ⏳ **NEXT UP**
- [ ] Implement Iceberg write path (Parquet writer + commit)
- [ ] Add Spring Boot Actuator + Prometheus metrics
- [ ] Wire Docker Compose end-to-end and validate CDC → Kafka → Iceberg

---

## 🏗️ Potential ADR Roadmap

### **Phase 1: Foundation** (Week 1)
- [ ] **ADR-001**: Project Foundation (Gradle, Spring Boot 3.2+, basic structure)
- [ ] **ADR-002**: Database Setup (PostgreSQL, MySQL, MongoDB, H2 configuration)
- [ ] **ADR-003**: CDC Configuration (Debezium connectors)
- [ ] **Sprint Goal**: Basic CDC pipeline from one database working

### **Phase 2: Core Streaming** (Week 2)  
- [ ] **ADR-004**: Kafka Configuration (topics, partitioning strategy)
- [ ] **ADR-005**: Schema Registry & Avro (schema evolution handling)
- [ ] **ADR-006**: Stream Processing Logic (filtering, enrichment, routing)
- [ ] **Sprint Goal**: Multi-database CDC → Kafka → basic processing

### **Phase 3: Data Lake Integration** (Week 3)
- [ ] **ADR-007**: Iceberg Configuration (table structure, partitioning)
- [ ] **ADR-008**: Batch Processing Strategy (volume handling optimizations)
- [ ] **ADR-009**: Observability Setup (metrics, tracing, monitoring)
- [ ] **Sprint Goal**: End-to-end pipeline with observability

### **Phase 4: Advanced Features** (Week 4)
- [ ] **ADR-010**: Test Environment Automation
- [ ] **ADR-011**: NLP Query Interface  
- [ ] **ADR-012**: Performance Optimization & Circuit Breakers
- [ ] **Sprint Goal**: Interview-ready system with "holy shit" features

---

## ▶️ Next Steps (Execution)

1) Implement Iceberg write path
- Add Parquet writer and table commit in `IcebergService.writeRecord(...)`
- Validate with a small batch via `docs/timeline-test.md`

2) Enable observability
- Add `spring-boot-starter-actuator`; expose health, metrics
- Register Micrometer Prometheus registry

3) Wire local stack
- Ensure Docker Compose brings up Postgres, Kafka, Debezium, MinIO, REST Catalog
- Verify connector status and topic messages; run `CdcIntegrationTest`

4) Stabilize CDC → Iceberg
- Add error metrics for DLQ candidates in `CdcEventListener`
- Backpressure handling review (listener concurrency, retry policy)