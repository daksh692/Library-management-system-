# Library Management System

A full-stack library management system with dual role-based interfaces — a patron-facing
discovery experience and a librarian command centre — built on Spring Boot, MongoDB, and React.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-green)
![MongoDB](https://img.shields.io/badge/MongoDB-7.0-brightgreen)
![React](https://img.shields.io/badge/React-19-blue)

---

## What it does

**For patrons**
- Browse and search the catalogue by title, author, genre, or ISBN
- Personalised recommendations drawn from previously borrowed genres
- Reading history, active loan reminders, and overdue warnings
- Reserve unavailable titles and join a queue, with an estimated availability date
- In-app notifications when a reserved book is ready to collect

**For librarians**
- Full catalogue CRUD with shelf-location validation (`Aisle-Shelf-Bin`)
- Patron directory searchable by phone, member ID, name, or email
- Issue and return with searchable pickers — no identifiers typed by hand
- Automatic fine calculation for late returns, damage, and loss
- Reservation queue with a 48-hour pickup window and automatic expiry

## Architecture

```
React (Vite) ──JWT over HTTPS──► Spring Boot ──► MongoDB
   │                                  │
   ├─ AuthContext, ProtectedRoute      ├─ SecurityConfig, JwtAuthenticationFilter
   ├─ Axios + interceptors             ├─ Controllers → Services → Repositories
   └─ Tailwind design system           └─ @Scheduled queue maintenance
```

Layered backend with strict boundaries: controllers never touch repositories, services never
return entities to the wire, and every response goes through a DTO. See
[`IMP_Files/architecture.md`](IMP_Files/architecture.md) for the full diagram and
[`IMP_Files/STRUCTURE.md`](IMP_Files/STRUCTURE.md) for the file-by-file map.

---

## Getting started

### Prerequisites

| Tool | Version | Notes |
|---|---|---|
| JDK | 17+ | `java -version` |
| Maven | 3.8+ | or use the bundled wrapper |
| Node.js | 20+ | `node -v` |
| MongoDB | 7.0+ | **must run as a replica set** — see below |
| Docker | any recent | optional; needed for integration tests |

### 1. MongoDB as a single-node replica set

Multi-document transactions require a replica set. A standalone `mongod` will not work.

```bash
mongod --dbpath /your/data/path --replSet rs0
```

Then once, ever:

```javascript
// mongosh
rs.initiate()
rs.status()      // "myState": 1 means you're ready
```

### 2. Environment variables

The application will not start without a JWT secret. This is deliberate — it must never fall back
to a predictable key.

```bash
# generate one
openssl rand -base64 32
```

```bash
# Linux / macOS
export JWT_SECRET="your-generated-value"
export MONGODB_URI="mongodb://localhost:27017/lms?replicaSet=rs0"
```

```powershell
# Windows PowerShell
$env:JWT_SECRET = "your-generated-value"
$env:MONGODB_URI = "mongodb://localhost:27017/lms?replicaSet=rs0"
```

| Variable | Required | Default | Purpose |
|---|---|---|---|
| `JWT_SECRET` | **yes** | — | Base64, ≥32 bytes. Token signing key |
| `MONGODB_URI` | no | `mongodb://localhost:27017/lms?replicaSet=rs0` | Database connection |
| `EMAIL_ENABLED` | no | `false` | When false, emails are logged not sent |
| `SMTP_HOST` / `SMTP_PORT` | no | `smtp.gmail.com` / `587` | Only used when email is enabled |
| `SMTP_USERNAME` / `SMTP_PASSWORD` | no | — | Never commit these |

### 3. Backend

```bash
cd backend
mvn spring-boot:run
```

Runs on `http://localhost:8080`. On first start `DataSeeder` creates demo accounts and three books.

### 4. Frontend

```bash
cd frontend
npm install
npm run dev
```

Runs on `http://localhost:5173`.

### Demo credentials

> Development only. `DataSeeder` is annotated `@Profile("!prod")` and never runs in production.

| Role | User ID | Password |
|---|---|---|
| Admin | `admin` | `admin123` |
| Patron | `user1` | `password` |
| Patron | `user2` | `password` |

---

## Configuration

Every library policy is configurable — no thresholds are hardcoded (Rules.md #1).

```properties
app.loan.period-days=14           # loan length
app.loan.max-active-books=5       # concurrent items per patron
app.penalty.per-day-late=1.00     # late fee per day
app.penalty.damaged-rate=0.5      # share of book price charged for damage
app.penalty.lost-rate=1.0         # share charged for loss
app.hold.window-hours=48          # reservation pickup window
app.card.validity-months=12       # library card lifetime
app.card.enforce-expiry=true      # block borrowing on an expired card
```

---

## API

Interactive documentation at `http://localhost:8080/swagger-ui.html` while the app is running.

| Method | Endpoint | Access | Purpose |
|---|---|---|---|
| POST | `/api/auth/login` | public | Obtain a JWT |
| POST | `/api/auth/register` | public | Self-registration |
| GET | `/api/public/books` | public | Paginated catalogue |
| GET | `/api/public/books/search?query=` | public | Search title/author/genre/ISBN |
| GET | `/api/public/books/{id}` | public | Detail, with availability estimate |
| GET | `/api/public/books/{id}/related` | public | Same genre or author |
| GET | `/api/user/dashboard` | patron | Loans, history, recommendations, arrivals |
| POST | `/api/user/reservations` | patron | Join a queue |
| GET | `/api/user/notifications` | patron | Notification inbox |
| GET | `/api/admin/users/search?query=` | admin | Directory lookup |
| POST | `/api/admin/books` | admin | Add to catalogue |
| POST | `/api/admin/transactions/issue` | admin | Issue or enqueue |
| POST | `/api/admin/transactions/return` | admin | Check in with condition |
| POST | `/api/admin/transactions/{id}/handover` | admin | Complete a reservation |
| POST | `/api/admin/transactions/{id}/settle-penalty` | admin | Mark a fine paid |

### Error format

Every non-2xx response uses one shape:

```json
{
  "error":     "Book not found: 653f1a2b3c4d5e6f7a8b9c0d",
  "code":      "BOOK_NOT_FOUND",
  "status":    404,
  "timestamp": "2026-08-11T10:30:00Z"
}
```

Validation failures add a `fields` map. Stack traces, file paths, and driver errors are never
returned — unexpected faults get a generic message plus a trace reference that appears in the logs.

---

## Testing

```bash
cd backend
mvn test                               # all tests
mvn test -Dtest='*IntegrationTest'     # integration only (requires Docker)
```

Integration tests spin up a real MongoDB replica set via Testcontainers, so no local database is
needed and nothing is left behind.

```bash
cd frontend
npm run lint
```

---

## Security

- **Passwords** BCrypt-hashed; never serialized into any API response
- **Tokens** HS256 JWT, key supplied by environment, 10-hour expiry
- **Authorisation** enforced at the filter chain, not in controllers
- **Rate limiting** tiered per route class, with per-account limits and exponential backoff on auth
- **Validation** every input is checked against a strict schema and rejected, not sanitised
- **Errors** never leak stack traces, paths, or database messages

See [`IMP_Files/Rules.md`](IMP_Files/Rules.md) for the full standard and
[`IMP_Files/ASSESSMENT.md`](IMP_Files/ASSESSMENT.md) for the current compliance audit.

---

## Project layout

```
backend/src/main/java/com/library/lms/
├── config/        SecurityConfig, JwtAuthenticationFilter, RateLimitingFilter, MongoConfig
├── controller/    Auth, Book, User, Admin, Transaction, Notification
├── service/       Book, User, Transaction, QueueMgmt, Discovery, Notification
├── repository/    Spring Data interfaces + custom atomic fragments
├── model/         MongoDB documents
├── dto/           requests, and dto/response for outbound shapes
├── exception/     typed hierarchy + GlobalExceptionHandler
├── validation/    custom constraints (ISBN-13)
└── util/          JwtUtil

frontend/src/
├── components/    shared UI, ui/ for primitives
├── context/       AuthContext
├── services/      Axios instance, error helpers
└── views/         auth/, user/, admin/
```

## Documentation

| Document | Contents |
|---|---|
| [`IMP_Files/PRD.md`](IMP_Files/PRD.md) | Product requirements |
| [`IMP_Files/architecture.md`](IMP_Files/architecture.md) | System design and state machine |
| [`IMP_Files/Desigine.md`](IMP_Files/Desigine.md) | Design system |
| [`IMP_Files/Rules.md`](IMP_Files/Rules.md) | Engineering standards |
| [`IMP_Files/STRUCTURE.md`](IMP_Files/STRUCTURE.md) | File-by-file map |
| [`IMP_Files/ASSESSMENT.md`](IMP_Files/ASSESSMENT.md) | Compliance audit |
| [`IMP_Files/upgrade/`](IMP_Files/upgrade/) | Upgrade workstreams |

## Licence

MIT