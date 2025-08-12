# MicroApple Debugger Playbook

Terse, copy-pasteable checks. Use binary search: isolate the broken hop between components.

## Decision tree (binary search the pipeline)
1) Infra up? → 2) App DB writes? → 3) Debezium emitting? → 4) Kafka topic has events? → 5) App listener firing? → 6) Iceberg catalog reachable? → 7) Iceberg table init? → 8) Iceberg write path ok?

If a step fails, fix there before proceeding. Never skip ahead.

---

## 0) Preflight
```bash
# Start stack
docker-compose up -d
# Check containers
docker-compose ps

# App
./gradlew bootRun
```
Expected:
- Containers healthy (postgres, kafka, debezium, kafka-ui, minio, iceberg-rest)
- App starts without errors

---

## 1) App → DB (CRUD)
IntelliJ HTTP (or curl):
```http
POST http://localhost:8090/api/purchases?count=3&userId=debug&appId=com.apple.debug&price=1.23
GET  http://localhost:8090/api/purchases
```
Verify DB (IntelliJ DB client):
```sql
SELECT id, user_id, app_id, price, purchase_date, transaction_id
FROM public.app_purchases ORDER BY id DESC LIMIT 10;
```
Fail patterns:
- DB empty → app not saving: check app logs, datasource URL/creds

---

## 2) Debezium connector health
Create/verify connector:
```bash
curl -s http://localhost:8083/connectors | jq
curl -s http://localhost:8083/connectors/microapple-postgres-connector/status | jq
```
PostgreSQL CDC prereqs:
```bash
docker exec microapple-postgres psql -U microapple -d microapple -c "SHOW wal_level;"
docker exec microapple-postgres psql -U microapple -d microapple -c \
  "SELECT slot_name, active FROM pg_replication_slots;"
docker exec microapple-postgres psql -U microapple -d microapple -c \
  "SELECT pubname, schemaname, tablename FROM pg_publication_tables;"
```
Fail patterns:
- No slot/publication → connector misconfigured (`table.include.list`, `slot.name`, `publication.name`)

---

## 3) Kafka topic has CDC events
Kafka UI: http://localhost:8080 → topic `microapple-postgres.public.app_purchases` → Messages → Live

CLI (optional):
```bash
docker exec microapple-kafka kafka-topics --bootstrap-server localhost:9092 --list
docker exec microapple-kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic microapple-postgres.public.app_purchases \
  --from-beginning --max-messages 5 | sed -n '1,5p'
```
Fail patterns:
- Topic missing/empty → Debezium not reading WAL or app not writing

---

## 4) App listener firing (Kafka → Spring)
Trigger events (create/update). Check app logs for:
- "Received CDC event for Iceberg processing"
- "Successfully processed CDC event for Iceberg"

If not firing:
- Check `spring.kafka` bootstrap servers, group id, topic name
- Ensure consumer can reach `localhost:9092` (see docker compose advertised listeners)

---

## 5) Iceberg catalog reachable
```bash
curl -s http://localhost:8181/v1/config | jq
```
MinIO bucket present:
```bash
docker exec microapple-minio mc alias set myminio http://localhost:9000 minioadmin minioadmin123
docker exec microapple-minio mc ls myminio/ | grep iceberg-warehouse || \
  docker exec microapple-minio mc mb myminio/iceberg-warehouse
docker exec microapple-minio mc ls myminio/iceberg-warehouse
```
Fail patterns:
- 8181 down → container or env vars; MinIO endpoint URL must resolve inside container (`minio:9000`)

---

## 6) Iceberg table init
```http
POST http://localhost:8090/api/datalake/init
```
Expected app logs:
- "Created Iceberg table: microapple.app_purchases" (or "already exists")

---

## 7) Iceberg write path (current state: stub)
Current expected (after implementation):
- App logs show commit lines like "Committed Iceberg data file"
- API reads return non-empty when events are present
```http
GET http://localhost:8090/api/datalake/purchases
GET http://localhost:8090/api/datalake/purchases/transaction/{transactionId}
```
When write path is implemented:
- Records appear via API and in MinIO under `iceberg-warehouse/...`
```bash
docker exec microapple-minio mc find myminio/iceberg-warehouse -name "*.parquet" | head -n 10
```

---

## Binary search strategy (apply at each failure)
- Break the path into halves:
  - [App→DB] [DB→WAL→Debezium] [Debezium→Kafka] [Kafka→Listener] [Listener→Iceberg]
- Prove/Disprove each half with the fastest probe (above). Move inward on failure.
- Never change >1 variable between probes.

---

## One-liner cheat sheet
```bash
# Debezium status
curl -s http://localhost:8083/connectors/microapple-postgres-connector/status | jq
# WAL level
docker exec microapple-postgres psql -U microapple -d microapple -c "SHOW wal_level;"
# Slot list
docker exec microapple-postgres psql -U microapple -d microapple -c "SELECT slot_name, active FROM pg_replication_slots;"
# Topic list
docker exec microapple-kafka kafka-topics --bootstrap-server localhost:9092 --list | grep microapple-postgres
# Consume few events
docker exec microapple-kafka kafka-console-consumer --bootstrap-server localhost:9092 --topic microapple-postgres.public.app_purchases --from-beginning --max-messages 3 | cat
# Iceberg REST config
curl -s http://localhost:8181/v1/config | jq
# Init table
curl -s -X POST http://localhost:8090/api/datalake/init
# App health/metrics
curl -s http://localhost:8090/actuator/health | jq
curl -s http://localhost:8090/actuator/metrics | jq
```

---

## Common failure signatures → fastest fix
- No CDC events in Kafka: confirm table exists + connector `table.include.list`; check replication slot active
- Consumer not receiving: fix Kafka advertised listeners (`PLAINTEXT_HOST://localhost:9092` exposed)
- Catalog 500s: verify MinIO credentials/endpoint and bucket exists
- App cannot reach catalog: `iceberg.catalog.uri` and S3 path-style access must match docker compose

---

References
- Pipeline steps: `docs/timeline.md`
- Test requests: `docs/timeline-test.md`
- Status: `docs/status.md`
