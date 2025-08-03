`# ADR-001: Project Foundation & Build System

**Date**: 2024-12-28  
**Status**: Accepted  
**Context**: Apple Staff SWE Interview Prep - MicroApple Demo

---

## Context

We need to establish foundational technology choices for **MicroApple** - a production-ready CDC demo that showcases Apple-scale streaming architecture. This demo must handle 100K+ events/sec and demonstrate deep understanding of Apple's operational challenges.

## Decision

### Build System: **Gradle 8.5+**
- **Rationale**: Modern build tool with superior dependency management, faster builds, and better multi-module support. Gradle's flexibility essential for complex CDC pipeline dependencies.
- **Alternative Considered**: Maven (rejected - slower builds, less flexible for complex multi-module projects)

### Framework: **Spring Boot 3.5.4**
- **Core**: Spring Boot 3.5.4 with Java 21 LTS
- **Reactive**: Spring WebFlux for non-blocking I/O (>100K events/sec requirement)
- **Streaming**: Spring Cloud Stream with Kafka Binder
- **Data**: Spring Data JPA + R2DBC for reactive database access
- **Rationale**: Apple values enterprise stability. Java 21 LTS + latest Spring Boot. Reactive stack essential for volume requirements.

### Package Structure
```
com.ncode.microapple
├── cdc/                 # Change Data Capture components
├── streaming/           # Kafka/event processing 
├── datalake/           # Iceberg integration
├── testenvironment/    # Automated test environment provisioning
├── nlp/                # Natural language query interface
└── config/             # Configuration classes
```

### Database Strategy
- **Development**: Docker Compose with realistic volume simulation
- **Testing**: Testcontainers for integration tests
- **Sources**: PostgreSQL, MySQL, MongoDB, H2 (Apple's heterogeneous reality)

### Container Strategy
- **Local Development**: Docker Compose (4 databases + Kafka + MinIO)
- **CI/CD**: Testcontainers for realistic integration testing
- **Production-Like**: Kubernetes manifests for scaling demonstration

## Consequences

### ✅ Positive
- Gradle's superior build performance and multi-module support
- Spring Boot 3.2+ provides reactive capabilities for high throughput
- Clear package structure enables modular development
- Testcontainers ensures realistic testing without infrastructure complexity

### ⚠️ Negative  
- Spring WebFlux learning curve (acceptable for Staff-level role)
- Docker Compose complexity (manageable with proper documentation)

### 🔄 Mitigation
- Comprehensive README with setup instructions
- ADR documentation for complex decisions
- Demo scripts for each major component

## Next Steps
1. Initialize Spring Boot project with reactive web starter
2. Set up Docker Compose for local development
3. Create ADR-002: Database Configuration Strategy
4. Implement basic health checks and actuator endpoints

---

**Implementation Timeline**: Week 1 - Foundation Phase  
**Success Criteria**: `./gradlew bootRun` starts with all health checks green 