# Supply Chain Event Processing System

Complete microservices-based supply chain event processing system with Kafka, MongoDB, and React UIs.

## 📁 Project Structure

```
supply-chain-complete/
├── pom.xml                          # Parent Maven POM
├── docker-compose.yml               # Full deployment
├── docker-compose-infrastructure.yml # Development mode
│
├── consumer-service/                # Kafka consumer with 4-level dedup
├── producer-service/                # Event creation REST API
├── loader-service/                  # Event router
├── item-service/                    # Item processing + OSP integration
├── trade-item-service/              # GTIN management
├── supplier-supply-service/          # Supplier supply tracking
├── shipment-service/                # Shipment tracking
├── osp-mock-api/                    # Mock external API
│
├── producer-ui/                     # React - Event creation UI
└── monitoring-ui/                   # React - Error monitoring UI
```

## 🚀 Quick Start

### Option 1: Full Docker Deployment

```bash
# Build all services
mvn clean package -DskipTests

# Start everything
docker-compose up -d

# Access UIs
# Producer UI: http://localhost:3000
# Monitoring UI: http://localhost:3001
```

### Option 2: Development Mode (Recommended for Learning)

```bash
# Start infrastructure only
docker-compose -f docker-compose-infrastructure.yml up -d

# Import to IntelliJ
# 1. Open project root pom.xml
# 2. Wait for Maven sync
# 3. Run individual services from IntelliJ
```

## 📊 Services & Ports

| Service | Port | Purpose |
|---------|------|---------|
| **Consumer Service** | 8081 | Kafka consumer with deduplication |
| **Producer Service** | 8087 | Create events via REST API |
| **Loader Service** | 8082 | Route events to processors |
| **Item Service** | 8083 | Process items → OSP API |
| **Trade Item Service** | 8084 | GTIN validation |
| **Supplier Supply Service** | 8085 | Supply level management |
| **Shipment Service** | 8086 | Tracking updates |
| **OSP Mock API** | 9000 | Mock external API |
| **Producer UI** | 3000 | Web UI to create events |
| **Monitoring UI** | 3001 | Error monitoring dashboard |
| **Kafka** | 9092 | Message broker |
| **MongoDB** | 27017 | Database |
| **Zookeeper** | 2181 | Kafka coordination |

## 🎯 System Flow

```
Producer UI (3000)
    ↓
Producer Service (8087) → Kafka (9092)
    ↓
Consumer Service (8081) [4-Level Deduplication]
    ↓
Loader Service (8082) [Event Router]
    ↓
├─→ Item Service (8083) → OSP API (9000)
├─→ Trade Item Service (8084)
├─→ Supplier Supply Service (8085)
└─→ Shipment Service (8086)
    ↓
MongoDB (27017)
    ↓
Monitoring UI (3001) [Error Dashboard]
```

## 🔐 4-Level Deduplication

Consumer service implements comprehensive deduplication:

1. **Level 1:** Kafka offset management (exactly-once)
2. **Level 2:** Event ID check (idempotency)
3. **Level 3:** Business key check (SKU, GTIN, tracking#)
4. **Level 4:** Content hash (detect data changes)

## 📝 Creating Events

### Via Producer UI (http://localhost:3000)

1. Select event type (Item, Trade Item, Supplier Supply, Shipment)
2. Fill in the form
3. Click "Create Event"
4. View results instantly

### Via REST API

```bash
# Create Item Event
curl -X POST http://localhost:8087/api/producer/item \
  -H "Content-Type: application/json" \
  -d '{
    "skuId": "SKU001",
    "itemName": "Laptop",
    "category": "Electronics",
    "price": 999.99,
    "status": "ACTIVE",
    "action": "CREATE"
  }'

# Create Trade Item Event
curl -X POST http://localhost:8087/api/producer/trade-item \
  -H "Content-Type: application/json" \
  -d '{
    "gtin": "12345678901234",
    "skuId": "SKU001",
    "supplierName": "ABC Corp",
    "action": "CREATE"
  }'

# Create Supplier Supply Event
curl -X POST http://localhost:8087/api/producer/supplier-supply \
  -H "Content-Type: application/json" \
  -d '{
    "skuId": "SKU001",
    "warehouseId": "WH001",
    "availableQuantity": 100,
    "reorderPoint": 30,
    "action": "UPDATE"
  }'

# Create Shipment Event
curl -X POST http://localhost:8087/api/producer/shipment \
  -H "Content-Type: application/json" \
  -d '{
    "trackingNumber": "TRACK123",
    "carrier": "FedEx",
    "shipmentStatus": "IN_TRANSIT",
    "action": "UPDATE"
  }'
```

## 🐛 Monitoring Errors

Access http://localhost:3001 to view:
- Total errors
- Unresolved errors
- Error details by stage (DB_SAVE, OSP_API, LOADER_SERVICE)
- Event data that failed
- Retry counts

## 🔧 IntelliJ Setup

1. **Install Lombok Plugin**
   - File → Settings → Plugins → Search "Lombok" → Install

2. **Enable Annotation Processing**
   - File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
   - Check "Enable annotation processing"

3. **Import Project**
   - File → Open → Select root `pom.xml`
   - Choose "Open as Project"
   - Wait for Maven sync (2-3 minutes)

4. **Run Services**
   - Right-click on `ConsumerServiceApplication.java` → Run
   - Right-click on `ProducerServiceApplication.java` → Run
   - And so on...

## ✅ Health Checks

```bash
# Check all services
curl http://localhost:8081/actuator/health  # Consumer
curl http://localhost:8087/actuator/health  # Producer
curl http://localhost:8082/api/loader/health  # Loader
curl http://localhost:8083/api/health       # Item
curl http://localhost:8084/api/health       # Trade Item
curl http://localhost:8085/api/health       # Supplier Supply
curl http://localhost:8086/api/health       # Shipment
curl http://localhost:9000/osp/api/health   # OSP Mock
```

## 🗄️ Database Access

```bash
# Connect to MongoDB
docker exec -it mongodb mongosh

# View databases
show dbs

# Use supply chain database
use supply_chain_db

# View collections
show collections

# Query items
db.items.find().pretty()

# Query errors
db.error_logs.find({resolved: false}).pretty()
```

## 🧪 Testing

```bash
# Run all tests
mvn test

# Run specific service tests
mvn -f consumer-service/pom.xml test
```

## 🛑 Stopping Services

```bash
# Stop all Docker containers
docker-compose down

# Stop infrastructure only
docker-compose -f docker-compose-infrastructure.yml down

# Remove volumes (⚠️ deletes data)
docker-compose down -v
```

## 📦 Technologies

- **Backend:** Spring Boot 3.2, Java 17
- **Messaging:** Apache Kafka
- **Database:** MongoDB
- **Frontend:** React 18
- **Build:** Maven
- **Containerization:** Docker, Docker Compose

## 📈 Features

✅ 8 Microservices
✅ 4-Level Event Deduplication/Idempotency
✅ Kafka Event Streaming
✅ MongoDB Persistence
✅ Error Tracking & Monitoring
✅ OSP API Integration
✅ Producer-Side Deduplication
✅ React Web UIs
✅ Docker Deployment
✅ Health Checks
✅ Comprehensive Logging
✅ Event Routing
✅ Retry Logic

## 🎓 Learning Path

1. Start with infrastructure: `docker-compose-infrastructure.yml up -d`
2. Run Producer Service in IntelliJ
3. Run Consumer Service in IntelliJ
4. Create an event via Producer UI
5. Watch logs in IntelliJ console
6. Check MongoDB for saved data
7. View Monitoring UI for any errors
8. Gradually add more services (Loader, Item, etc.)

## 📄 License

This is a demonstration/educational project.

## 🤝 Support

For issues or questions, refer to service logs:

```bash
# View container logs
docker logs -f consumer-service
docker logs -f producer-service
# etc...
```

---

**Built with ❤️ for learning microservices architecture**
