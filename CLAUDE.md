# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Microservices-based supply chain event processing system. 9 Java Spring Boot services (including BFF API gateway), 2 React frontends, connected via Kafka and MongoDB.

## Build & Run Commands

```bash
# Build all backend services (from root)
mvn clean package -DskipTests

# Build a single service
mvn -pl consumer-service -am clean package -DskipTests

# Run tests (all)
mvn test

# Run tests for a single service
mvn -f consumer-service/pom.xml test

# Full Docker deployment (build first)
docker-compose up -d

# Dev mode: infrastructure only (Kafka, MongoDB, Zookeeper), run services from IDE
docker-compose -f docker-compose-infrastructure.yml up -d

# Stop everything
docker-compose down

# Frontend dev (from producer-ui/ or monitoring-ui/)
npm install
npm start
```

## Architecture

**Event flow:** Producer UI (3000) -> BFF Service (8080) -> Producer Service (8087) -> Kafka -> Consumer Service (8081) -> Loader Service (8082) -> [Item|TradeItem|SupplierSupply|Shipment] Services -> MongoDB. Monitoring UI (3001) -> BFF Service (8080) -> Consumer Service (8081) for error logs.

### Services and Ports

| Service                 | Port | Role                                                 |
|-------------------------|------|------------------------------------------------------|
| bff-service             | 8080 | BFF API Gateway, single entry point for frontends    |
| consumer-service        | 8081 | Kafka consumer, 4-level deduplication, error logging |
| loader-service          | 8082 | Event router to downstream services                  |
| item-service            | 8083 | Item processing, calls OSP API with retry            |
| trade-item-service      | 8084 | GTIN/supplier management                             |
| supplier-supply-service | 8085 | supplier level tracking                              |
| shipment-service        | 8086 | Shipment tracking                                    |
| producer-service        | 8087 | REST API for event creation, publishes to Kafka      |
| osp-mock-api            | 9000 | Mock external API                                    |
| producer-ui             | 3000 | React event creation form                            |
| monitoring-ui           | 3001 | React error monitoring dashboard                     |

### Key Design Patterns

- **4-Level Deduplication** (consumer-service): Kafka offset management -> Event ID check -> Business key check -> SHA-256 content hash. Manual offset commits (auto-commit disabled).
- **Producer-side deduplication** (producer-service): Idempotency key + content hash stored in MongoDB before publishing.
- **Event routing** (loader-service): Extracts event type from payload, falls back to field-presence inference (e.g., `gtin` -> trade-item, `trackingNumber` -> shipment). Routes via WebClient to downstream services.
- **Retry with backoff** (item-service): Exponential backoff (2^n * 1000ms) for OSP API calls.
- **BFF API Gateway** (bff-service): Single entry point for React frontends. Proxies `/api/producer/*` to producer-service and `/api/errors/*` to consumer-service via WebClient. Forwards `Idempotency-Key` header.
- **Error tracking**: Consumer service logs failures to MongoDB `error_logs` collection with stage (DB_SAVE, OSP_API, LOADER_SERVICE), retry count, and resolution workflow.

## Tech Stack

- **Java 17**, **Spring Boot 3.2.0**, **Spring Cloud 2023.0.0**
- **Kafka** (Confluent 7.5.0) with Zookeeper, **MongoDB**
- **React 18** with Axios, Recharts (monitoring-ui)
- **Maven** parent POM with 9 child modules
- **Lombok** for boilerplate reduction (requires annotation processing enabled in IDE)
- **Docker** multi-stage builds (Maven build + Alpine JRE runtime)

## Code Organization

Each backend service follows: `com.supplychain.<service>` with subpackages `config/`, `controller/`, `entity/`, `model/`, `repository/`, `service/`. The consumer-service is the largest (22 files) with the dedup logic. Other services have 3-5 files each.

Frontend apps are single-component React apps: `ProducerUI.js` (producer-ui) and `Dashboard.js` (monitoring-ui).

## Kafka Topics

- `supply-chain.item.events`
- `supply-chain.trade-item.events`
- `supply-chain.supplier-supply.events`
- `supply-chain.shipment-tracking.events`

## Environment Variables (Docker overrides)

Services use `application.yml` for local dev (localhost) and environment variables in docker-compose for containerized deployment:
- `SPRING_KAFKA_BOOTSTRAP_SERVERS` (kafka:9092 in Docker)
- `SPRING_DATA_MONGODB_URI` (mongodb://mongodb:27017/supply_chain_db in Docker)
- `LOADER_SERVICE_URL`, `SERVICES_*_SERVICE_URL`, `OSP_API_URL` for inter-service URLs

## Notes

- No test files exist yet (test directories are empty).
- No CI/CD pipeline configured.
- MongoDB database: `supply_chain_db`. Collections: items, trade_items, supplier_supply, shipments, error_logs, produced_events.
- Kafka producer config: acks=all, idempotence enabled, snappy compression, 3 retries.
- All services expose health endpoints (`/actuator/health` or `/api/health`).
