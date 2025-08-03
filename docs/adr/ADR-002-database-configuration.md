# ADR-002: Database Configuration Strategy

**Date**: 2024-12-28  
**Status**: Accepted  
**Context**: Apple Staff SWE Interview Prep - MicroApple CDC Demo

---

## Context

MicroApple needs to simulate Apple's heterogeneous database environment for realistic CDC demonstration. We must support multiple database types while maintaining production-grade configuration patterns that scale to Apple's operational requirements.

## Decision

### Phase 1: PostgreSQL Foundation
- **Primary Database**: PostgreSQL 15 (Apple's enterprise standard)
- **Connection Strategy**: Spring Data JPA with HikariCP connection pooling
- **Schema Management**: Hibernate DDL with create-drop for development
- **Configuration**: Environment-specific profiles (local, test, prod)

### Docker Strategy
- **Development**: Docker Compose with health checks
- **Networking**: Standard ports exposed for local development
- **Persistence**: Named volumes for data durability
- **Health Monitoring**: Built-in PostgreSQL health checks

### Observability
- **Health Endpoints**: Spring Boot Actuator with detailed database status
- **Metrics**: Connection pool metrics exposed via `/actuator/metrics`
- **Logging**: Hibernate SQL logging for development debugging

### Progressive Database Addition
```
Week 1: PostgreSQL (OLTP transactions)
Week 2: + MySQL (legacy system simulation)  
Week 3: + MongoDB (document store)
Week 4: + H2 (in-memory testing)
```

## Rationale

### Why PostgreSQL First?
- **Apple Alignment**: PostgreSQL is Apple's preferred enterprise RDBMS
- **CDC Maturity**: Excellent Debezium connector support
- **JSON Support**: Hybrid relational/document capabilities
- **Performance**: Handles high-volume CDC scenarios efficiently

### Why Docker Compose?
- **Realistic Environment**: Mirrors Apple's containerized infrastructure
- **Developer Experience**: One-command environment setup
- **CI/CD Ready**: Easy integration with Testcontainers for testing

### Why Spring Data JPA?
- **Enterprise Pattern**: Standard Apple/Spring enterprise approach
- **Reactive Ready**: Easy migration to R2DBC for reactive streams
- **Connection Pooling**: HikariCP provides production-grade connection management

## Consequences

### ✅ Positive
- Single database reduces initial complexity
- PostgreSQL provides rich CDC event types for demonstration
- Health checks enable early problem detection
- Configuration pattern scales to multi-database setup

### ⚠️ Considerations
- JPA overhead for high-volume CDC (mitigated by planned R2DBC migration)
- Docker dependency for local development (standard in Apple environment)

### 🔄 Next Steps
1. Verify PostgreSQL connectivity and health endpoints
2. Create sample AppStore tables for realistic CDC events
3. Add MySQL for heterogeneous database demonstration
4. Implement Debezium connectors for PostgreSQL

---

## Implementation Verification

**Success Criteria**:
- `docker-compose up postgres` starts PostgreSQL in <10 seconds
- `./gradlew bootRun` connects with green health checks
- `/actuator/health` shows database connectivity details
- Ready for CDC connector integration

**Apple Interview Context**:
*"I started with PostgreSQL because it mirrors Apple's enterprise database standards and provides the most mature CDC capabilities. This foundation scales to support your heterogeneous data environment while maintaining production-grade observability from day one."*