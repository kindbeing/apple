# Timeline Test Guide for CDC Pipeline

## 🚀 Quick Start

### 1. Start Infrastructure
```bash
# Start all services (Postgres, Kafka, Debezium, Kafka UI)
docker-compose up -d

# Wait for services to be healthy
docker-compose ps

# Start Spring Boot application
./gradlew bootRun
```

### 2. Setup CDC Connector (if not already done)
```bash
curl -X POST -H "Content-Type: application/json" \
  --data @docker/debezium/postgres-connector.json \
  http://localhost:8083/connectors
```

### 3. Open Kafka UI for Real-time Monitoring
Open browser: **http://localhost:8080**
- Navigate to Topics → `microapple-postgres.public.app_purchases`
- Click "Live" to see real-time message flow

---

## 📋 IntelliJ HTTP Client Requests

### CREATE Operations

#### Create Single Purchase
```http
POST http://localhost:8090/api/purchases
Content-Type: application/json

### Creates 1 purchase with default values
```

#### Create Multiple Purchases (Batch)
```http
POST http://localhost:8090/api/purchases?count=5&userId=testUser&appId=com.apple.testapp&price=9.99
Content-Type: application/json

### Creates 5 purchases: testUser-1, testUser-2, etc.
```

#### Create High-Volume Test Data
```http
POST http://localhost:8090/api/purchases?count=100&userId=loadTest&appId=com.apple.performance&price=0.99
Content-Type: application/json

### Creates 100 purchases for volume testing
```

---

### READ Operations

#### Get All Purchases
```http
GET http://localhost:8090/api/purchases
Accept: application/json

### Returns all purchases in database
```

#### Get Specific Purchase
```http
GET http://localhost:8090/api/purchases/{{transactionId}}
Accept: application/json

### Replace {{transactionId}} with actual transaction ID from create response
```

---

### UPDATE Operations

#### Update Purchase (triggers DELETE + CREATE in CDC)
```http
PUT http://localhost:8090/api/purchases/{{transactionId}}?userId=updatedUser&price=19.99
Content-Type: application/json

### Updates user and price - watch for DELETE then CREATE events in Kafka UI
```

---

### DELETE Operations

#### Delete Specific Purchase
```http
DELETE http://localhost:8090/api/purchases/{{transactionId}}
Accept: application/json

### Deletes single purchase - watch for DELETE event in Kafka UI
```

#### 🧹 Delete All Purchases (Cleanup)
```http
DELETE http://localhost:8090/api/purchases
Accept: application/json

### CLEANS UP ALL DATA - watch mass DELETE events in Kafka UI
```

---

## 🔍 Real-time CDC Monitoring

### Kafka UI Dashboard (http://localhost:8080)

1. **Navigate to Topics**
   - Click "Topics" in sidebar
   - Find `microapple-postgres.public.app_purchases`

2. **Live Message Monitoring**
   - Click topic name → "Messages" tab
   - Click "Live" button (top right)
   - Leave this open while testing

3. **What to Watch For**
   - **CREATE**: `"op":"c"` with `"after"` containing new data
   - **UPDATE**: `"op":"d"` (delete) followed by `"op":"c"` (create)
   - **DELETE**: `"op":"d"` with `"before"` containing deleted data

### Sample CDC Event Structure
```json
{
  "before": null,
  "after": {
    "id": 1,
    "user_id": "testUser-1",
    "app_id": "com.apple.testapp",
    "price": "A+c=",
    "purchase_date": 1754445123456789,
    "transaction_id": "uuid-here"
  },
  "source": {...},
  "op": "c",
  "ts_ms": 1754445123567
}
```

---

## 🧪 Testing Workflow

### Recommended Testing Sequence

1. **Start Monitoring**
   ```
   Open Kafka UI → Topics → app_purchases → Messages → Click "Live"
   ```

2. **Create Test Data**
   ```http
   POST /api/purchases?count=3&userId=demo&appId=com.apple.demo&price=1.99
   ```
   *Watch 3 CREATE events appear in Kafka UI*

3. **Update a Record**
   ```http
   PUT /api/purchases/{transactionId}?price=2.99
   ```
   *Watch DELETE then CREATE events*

4. **Delete Specific Record**
   ```http
   DELETE /api/purchases/{transactionId}
   ```
   *Watch DELETE event*

5. **Cleanup**
   ```http
   DELETE /api/purchases
   ```
   *Watch mass DELETE events*

---

## 🎯 Performance Testing

### High-Volume Create Test
```http
POST http://localhost:8090/api/purchases?count=1000&userId=perfTest&appId=com.apple.volume&price=0.01

### Monitor Kafka UI for throughput and processing speed
```

### Rapid Fire Updates
1. Create purchase
2. Update same purchase multiple times rapidly
3. Watch CDC keep up with changes in real-time

---

## 🔧 Troubleshooting

### If No CDC Events Appear:
1. Check connector status: `curl http://localhost:8083/connectors/microapple-postgres-connector/status`
2. Verify table exists: `docker exec microapple-postgres psql -U microapple -d microapple -c "\d app_purchases"`
3. Check Spring Boot logs for database connection issues

### If Kafka UI Not Loading:
1. Verify container is running: `docker ps | grep kafka-ui`
2. Check health: `curl http://localhost:8080`
3. Restart if needed: `docker-compose restart kafka-ui`

---

## 🎯 Next Steps

After PostgreSQL CDC is working smoothly:
- Extend controller to support Oracle (app reviews)
- Add MongoDB operations (content metadata)
- Multi-database CDC demonstration
- Cross-database transaction scenarios

---

## 📈 Intelligence Verification (Iceberg-backed)

After creating data:

```http
GET http://localhost:8090/api/datalake/stats/revenue-by-app
```

Then check count by user:

```http
GET http://localhost:8090/api/datalake/stats/count-by-user?userId=testUser-1
```

Expect non-zero totals and counts.