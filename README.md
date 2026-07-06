# NotiCore

A scalable, multi-channel notification microservice — built as a hands-on
learning project to gain real experience with Spring Boot, Apache Kafka,
Redis, PostgreSQL, and Docker.

NotiCore routes notifications across **email, SMS, and push**, deduplicates
requests and rate-limits per user with Redis, retries failed sends with
exponential backoff, and moves permanently-failed notifications to a dead
letter queue for inspection and replay. It exposes both a REST API, a CLI,
and a full Postman collection.

> **Use case:** the kind of internal notification service a SaaS product's
> other systems (checkout, billing, comments, auth) would all call into,
> rather than each talking to email/SMS/push providers directly.

---

## Status

✅ **All 5 phases complete.**

| Phase | What it adds | Status |
|---|---|---|
| 1 | Notification data model (Spring Boot + Supabase/Postgres) | ✅ Done |
| 2 | Kafka routing pipeline (topic per channel, consumer groups) | ✅ Done |
| 3 | Redis dedup + sliding-window rate limiter | ✅ Done |
| 4 | Spring Retry + dead letter queue | ✅ Done |
| 5 | Picocli CLI + Postman collection | ✅ Done |

---

## Tech stack

- **Spring Boot 3** — REST API and application framework
- **PostgreSQL** (hosted on **Supabase**) — persistent storage
- **Apache Kafka** (KRaft mode, via Docker) — durable, ordered message routing per channel
- **Redis** (via Docker) — deduplication and per-user sliding-window rate limiting
- **Spring Retry** — automatic retry with exponential backoff
- **Picocli** — the `send` / `cancel` / `replay` command-line interface
- **Docker Compose** — local Kafka + Redis environment
- **Postman** — full API testing collection

---

## Architecture

```
Client / CLI / Postman
     │
     ▼
REST API (Spring Boot)
     │
     ▼
Supabase (Postgres) — save as PENDING
     │
     ▼
Kafka topic (per channel: email / sms / push)
     │
     ▼
Consumer group (per channel)
     │
     ▼
Redis dedup check ──duplicate──▶ skip
     │ new
     ▼
Redis rate-limit check ──over limit──▶ reject
     │ allowed
     ▼
Send (simulated) ──fail──▶ Spring Retry (1s, 2s backoff) ──exhausted──▶ Dead Letter Queue
     │success                                                              │
     ▼                                                                     ▼
Status → SENT                                                    Status → FAILED
                                                                   (replayable via CLI/Postman)
```

A `PENDING` notification can also be **cancelled** (via CLI/Postman) before a
consumer processes it.

---

## Data model

**`notifications`** — one row per notification request
- `id` (UUID), `user_id`, `channel` (`EMAIL`/`SMS`/`PUSH`), `recipient`, `message`
- `status` (`PENDING` → `RETRYING` → `SENT` / `FAILED` / `CANCELLED`), `attempt_count`
- `created_at`, `updated_at`

**`channels`** — where a user can be reached, per channel type
- `id`, `user_id`, `type`, `address` (email/phone/device token), `verified`

**`user_preferences`** — per-user rate limits and opt-outs
- `user_id`, `max_notifications_per_minute`, `email_opt_out`, `sms_opt_out`, `push_opt_out`

---

## Running locally

### Prerequisites
- Java 17+
- Docker Desktop
- A free [Supabase](https://supabase.com) project (Postgres database)

### One-time setup
Get your Supabase **Session Pooler** connection string (Project → Connect →
Direct → Session pooler tab) — the direct connection often fails on
networks without IPv6 support, so the pooler is used instead.

### Every time you run it
```powershell
docker compose up -d          # starts Kafka + Redis
docker ps                     # confirm both containers show "Up"

$env:JAVA_HOME="C:\Program Files\Eclipse Adoptium\jdk-17.0.19.10-hotspot"
$env:SUPABASE_DB_URL="jdbc:postgresql://<your-pooler-host>:5432/postgres"
$env:SUPABASE_DB_USER="postgres.<your-project-ref>"
$env:SUPABASE_DB_PASSWORD="<your-password>"

.\mvnw.cmd spring-boot:run
```

### Shutting down
```powershell
Ctrl+C                 # stop the app
docker compose down    # stop Kafka + Redis
```

---

## API endpoints

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/notifications` | Create a notification (saved as `PENDING`) |
| `GET` | `/api/notifications/user/{userId}` | List all notifications for a user |
| `GET` | `/api/notifications/{id}` | Get a single notification by ID |
| `PATCH` | `/api/notifications/{id}/cancel` | Cancel a `PENDING` notification |
| `POST` | `/api/notifications/{id}/replay` | Replay a `FAILED` notification |

**Example — create a notification:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -Method Post `
  -ContentType "application/json" `
  -Body '{"userId":"user123","channel":"EMAIL","recipient":"test@example.com","message":"hello from noticore"}'
```

---

## CLI usage

Run via Maven's `exec` plugin (no separate build needed) while the app is running:

```powershell
# Send a notification
.\mvnw.cmd exec:java "-Dexec.args=send --user user123 --channel EMAIL --to test@example.com --message hello"

# Cancel a PENDING notification
.\mvnw.cmd exec:java "-Dexec.args=cancel <notification-id>"

# Replay a FAILED notification
.\mvnw.cmd exec:java "-Dexec.args=replay <notification-id>"
```

> Note: multi-word `--message` values with spaces can trip up Windows'
> `mvnw.cmd` quoting — stick to single-word messages when testing via the
> CLI, or use the REST API / Postman directly for messages with spaces.

---

## Postman

Import [`postman/NotiCore.postman_collection.json`](postman/NotiCore.postman_collection.json)
into Postman. It includes:
- Create requests for all 3 channels (Email/SMS/Push)
- A deliberately invalid create request (demonstrates the 400 validation response)
- Get by ID, Get all for a user
- Cancel and Replay

Set the collection's `notificationId` variable to an ID returned by a Create
request to use it in the ID-based requests.

---

## Testing key behaviors

**Deduplication** — republish the same notification ID to its Kafka topic
twice within 60 seconds; the second delivery logs `Duplicate detected,
skipping` instead of processing again.

**Rate limiting** — send 11+ requests for the same user within a minute
(default limit is 10); the 11th is rejected with `Rate limit exceeded`.

**Retry + DLQ** — sends fail randomly (~50%, simulating a real provider) and
retry up to 3 times with exponential backoff (~1s, then ~2s) before landing
in the dead letter queue if all attempts fail.

---

## Roadmap

- [x] Phase 1 — Data model, Supabase connection, basic REST API
- [x] Phase 2 — Kafka routing pipeline
- [x] Phase 3 — Redis dedup + rate limiting
- [x] Phase 4 — Retry + dead letter queue
- [x] Phase 5 — CLI + Postman collection

**Possible future improvements:** real email/SMS/push provider integration
(Resend, Twilio, FCM), Flyway migrations instead of `ddl-auto: update`,
authentication on the REST API, and automated integration tests.

---

## License

Personal learning project — no license applied yet.