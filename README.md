# NotiCore

A scalable, multi-channel notification microservice — built as a hands-on
learning project to gain real experience with Spring Boot, Apache Kafka,
Redis, PostgreSQL, and Docker.

NotiCore routes notifications across **email, SMS, and push**, deduplicates
requests and rate-limits per user with Redis, retries failed sends with
exponential backoff, and moves permanently-failed notifications to a dead
letter queue for inspection and replay. It exposes both a REST API and a CLI.

> **Use case:** the kind of internal notification service a SaaS product's
> other systems (checkout, billing, comments, auth) would all call into,
> rather than each talking to email/SMS/push providers directly.

---

## Status

🚧 **In progress — Phase 1 of 5 complete.**

| Phase | What it adds | Status |
|---|---|---|
| 1 | Notification data model (Spring Boot + Supabase/Postgres) | ✅ Done |
| 2 | Kafka routing pipeline (topic per channel, consumer groups) | ⬜ Not started |
| 3 | Redis dedup + sliding-window rate limiter | ⬜ Not started |
| 4 | Spring Retry + dead letter queue | ⬜ Not started |
| 5 | Picocli CLI + full Postman collection | ⬜ Not started |

---

## Tech stack

- **Spring Boot 3** — REST API and application framework
- **PostgreSQL** (hosted on **Supabase**) — persistent storage
- **Apache Kafka** *(coming in Phase 2)* — durable, ordered message routing per channel
- **Redis** *(coming in Phase 3)* — deduplication and per-user rate limiting
- **Spring Retry** *(coming in Phase 4)* — automatic retry with exponential backoff
- **Resend API** *(coming later)* — email delivery
- **Picocli** *(coming in Phase 5)* — command-line interface
- **Docker Compose** *(coming in Phase 2)* — local Kafka/Redis environment
- **Postman** *(coming in Phase 5)* — API testing collection

---

## Architecture (target — full picture across all phases)

```
Client / CLI
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
Redis check (dedup + rate limit)
     │
     ▼
Send (e.g. Resend API) ──fail──▶ Spring Retry ──exhausted──▶ Dead Letter Queue
     │success
     ▼
Status updated to SENT
```

Right now (Phase 1), only the top three boxes exist — a notification is
created and saved as `PENDING`, and nothing picks it up yet.

---

## Data model

**`notifications`** — one row per notification request
- `id` (UUID), `user_id`, `channel` (`EMAIL`/`SMS`/`PUSH`), `recipient`, `message`
- `status` (`PENDING` → `RETRYING` → `SENT` or `FAILED`), `attempt_count`
- `created_at`, `updated_at`

**`channels`** — where a user can be reached, per channel type
- `id`, `user_id`, `type`, `address` (email/phone/device token), `verified`

**`user_preferences`** — per-user rate limits and opt-outs
- `user_id`, `max_notifications_per_minute`, `email_opt_out`, `sms_opt_out`, `push_opt_out`

---

## Running locally

### Prerequisites
- Java 17+
- A free [Supabase](https://supabase.com) project (Postgres database)

### Setup
1. Clone this repo and open it in your editor of choice.
2. Set your Supabase connection details as environment variables:
   ```powershell
   $env:SUPABASE_DB_URL="jdbc:postgresql://<your-pooler-host>:5432/postgres"
   $env:SUPABASE_DB_USER="postgres.<your-project-ref>"
   $env:SUPABASE_DB_PASSWORD="<your-password>"
   ```
   > Use Supabase's **Session pooler** connection string (Project → Connect →
   > Direct → Session pooler), not the Direct connection — the direct
   > hostname only resolves over IPv6, which many home networks don't support.
3. Run the app:
   ```powershell
   .\mvnw.cmd spring-boot:run
   ```
4. On first run, Hibernate auto-creates all 3 tables in your Supabase database.

---

## API endpoints (Phase 1)

| Method | Path | Description |
|---|---|---|
| `POST` | `/api/notifications` | Create a notification (saved as `PENDING`) |
| `GET` | `/api/notifications/user/{userId}` | List all notifications for a user |
| `GET` | `/api/notifications/{id}` | Get a single notification by ID |

**Example request:**
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/notifications" -Method Post `
  -ContentType "application/json" `
  -Body '{"userId":"user123","channel":"EMAIL","recipient":"test@example.com","message":"hello from noticore"}'
```

---

## Roadmap

- [x] Phase 1 — Data model, Supabase connection, basic REST API
- [ ] Phase 2 — Kafka routing pipeline (Docker Compose for local Kafka)
- [ ] Phase 3 — Redis dedup + rate limiting
- [ ] Phase 4 — Retry + dead letter queue
- [ ] Phase 5 — CLI + Postman collection

---

## License

Personal learning project — no license applied yet.
