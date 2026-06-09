# API Gateway Abuse Detection System

[![CI](https://github.com/sachiny0106/api-gateway-abuse-detector/actions/workflows/ci.yml/badge.svg)](https://github.com/sachiny0106/api-gateway-abuse-detector/actions/workflows/ci.yml)

A real-time API abuse detection gateway built with **Spring Boot 3** and **Spring Cloud Gateway**.
It sits in front of your backend, inspects every request, scores it against six abuse-detection
rules, and dynamically **allows, flags, throttles, or blocks** traffic — without adding noticeable
latency to legitimate calls.

## How it works

```
        Client traffic
              │
              ▼
   ┌──────────────────────┐     reads decisions      ┌─────────┐
   │  Gateway  (:8080)     │◄────────────────────────►│  Redis  │
   │  reactive filter chain│     rate-limit counters   └─────────┘
   └──────────┬───────────┘
              │ async request events
              ▼
        ┌───────────┐
        │   Kafka    │
        └─────┬─────┘
       ┌──────┴───────┐
       ▼              ▼
┌──────────────┐ ┌──────────────┐     ┌────────────┐
│ Detector     │ │ Auditor       │────►│ PostgreSQL │
│ (:8081)      │ │ (:8082)       │     └────────────┘
│ 6 rules →    │ │ batch writer  │
│ score →      │ └──────────────┘
│ decision →   │
│ Redis+Kafka  │
└──────────────┘
```

- **Gateway** — extracts request metadata, enforces cached decisions, applies a Redis sliding-window
  rate limit, and publishes each request to Kafka asynchronously (non-blocking).
- **Detector** — consumes request events, runs six rules in parallel, combines them into a weighted
  score, and writes an allow/flag/throttle/block decision back to Redis.
- **Auditor** — consumes events and batch-writes them to PostgreSQL for analytics and audit.

If Redis or Kafka is unavailable, the gateway **fails open** so real users are never blocked by an
infrastructure outage.

## Detection rules

| Rule              | Detects                                  | Weight |
|-------------------|------------------------------------------|--------|
| Spike             | Sudden burst (short vs. long window rate)| 0.25   |
| Brute force       | >10 login attempts/60s on an endpoint    | 0.35   |
| Bot pattern       | Machine-regular request timing (low CV)  | 0.20   |
| User-agent        | Blocklisted / empty / low-entropy UA     | 0.10   |
| IP behaviour      | One IP hitting many unique endpoints     | 0.15   |
| Sequence anomaly  | Known multi-step abuse flows             | 0.30   |

**Decision thresholds:** `<0.45` allow · `0.45` flag · `0.65` throttle · `0.80` block.
Any single rule scoring `≥0.90` forces an immediate block.

## Tech stack

Java 21 · Spring Boot 3.3 · Spring Cloud Gateway · Redis · Kafka · PostgreSQL · Prometheus + Grafana · Docker Compose.

## Quick start

```bash
# 1. Start infrastructure (Redis, Kafka, Postgres, Prometheus, Grafana)
docker compose -f deployments/docker-compose.yml up -d

# 2. Build all modules
mvn clean install

# 3. Run each service (separate terminals)
mvn spring-boot:run -pl gateway-service    # :8080
mvn spring-boot:run -pl detector-service   # :8081
mvn spring-boot:run -pl auditor-service    # :8082
```

Send a request through the gateway:

```bash
curl -H "X-API-Key: my-key" http://localhost:8080/api/data
```

Health and metrics are exposed per service at `/actuator/health` and `/actuator/prometheus`
(e.g. `http://localhost:8080/actuator/health`). Grafana is at `http://localhost:3000`.

## Project layout

```
common/           Shared events and models (RequestEvent, DecisionEvent, Decision)
gateway-service/  Spring Cloud Gateway + filter chain + sliding-window rate limiter
detector-service/ Kafka consumer, 6 rules, scorer, decision maker
auditor-service/  Kafka consumer, batch JPA writer
deployments/      docker-compose, Prometheus, Grafana
migrations/       PostgreSQL schema (Flyway)
scripts/          Kafka topic seeding, load testing
```

## Author

Maintained by **Sachin Yadav** ([@sachiny0106](https://github.com/sachiny0106)).

## License

MIT — Copyright (c) 2026 Sachin Yadav. See [LICENSE](LICENSE).
