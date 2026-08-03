# Project Structure Map

Hand-built reference tree and dependency map for the Library Management System.
Last updated: 2026-07-26

> **Note:** this was assembled by reading source directly, not by `/graphify` — the
> sandboxed Linux environment that graphify's Python pipeline needs would not start
> in this session. Once it is available, `/graphify .` will supersede this file with
> `graphify-out/graph.json`, `GRAPH_REPORT.md`, and an interactive `graph.html`.

---

## 1. Top-level tree

```
library system/
├── CLAUDE.md                 # graphify usage rules for agents
├── .claude/
│   ├── CLAUDE.md             # /graphify trigger declaration
│   └── skills/graphify/      # the graphify skill itself
├── IMP_Files/                # planning + memory docs (not shipped)
│   ├── PRD.md                # master spec: schemas, features, endpoints
│   ├── architecture.md       # layer diagram, state machine, dir layout
│   ├── Phases.md             # 5-week roadmap
│   ├── Desigine.md           # design system: palette, type, layouts
│   ├── Rules.md              # 9 engineering/security rules
│   ├── memory.md             # what's done / open  ← read this first
│   └── STRUCTURE.md          # this file
├── backend/                  # Spring Boot 3.2.3, Java 17, Maven
└── frontend/                 # React + Vite + Tailwind
```

---

## 2. Backend tree — `backend/src/main/java/com/library/lms/`

```
LmsApplication.java              @SpringBootApplication @EnableScheduling
│
├── config/
│   ├── SecurityConfig.java      filter chain, CORS(:5173), role→path rules, BCrypt
│   ├── JwtAuthenticationFilter  extracts Bearer token → SecurityContext
│   └── RateLimitingFilter.java  Bucket4j, per-IP, 5/30/100 per min by route tier
│
├── controller/
│   ├── AuthController.java      POST /api/auth/login, /register
│   ├── BookController.java      GET /api/public/books, /search, /{id}
│   ├── AdminController.java     /api/admin/books/*, /users/*, /users/{id}/transactions
│   └── TransactionController    /api/admin/transactions/{issue,return,active}
│                                /api/user/transactions/active
│
├── service/
│   ├── BookService.java             catalog CRUD + in-memory search + soft delete
│   ├── UserService.java             user CRUD + soft delete + BCrypt on write
│   ├── TransactionService.java      ★ issue / return / penalty / queue promotion
│   ├── QueueMgmtService.java        @Scheduled hourly 48h-hold expiry sweep
│   └── CustomUserDetailsService     loads User by userId for Spring Security
│
├── model/                       MongoDB documents
│   ├── User.java                implements UserDetails; unique idx: userId, phone
│   ├── Book.java                idx: isbn, name, author, genre; price for penalties
│   └── Transaction.java         no indexes yet ← known gap
│
├── repository/
│   ├── UserRepository           findByUserId, findByPhone
│   ├── BookRepository           findByIsbn, findByGenre
│   └── TransactionRepository    findByUserId, findByBookId, findByStatus
│
├── dto/
│   ├── LoginRequest, RegisterRequest, AuthResponse
│   ├── BookDto, UserDto
│   └── TransactionRequest, ReturnRequest
│
├── exception/
│   └── GlobalExceptionHandler   @RestControllerAdvice: field errors + generic 500
│
├── util/
│   └── JwtUtil.java             HS256, 10h expiry, claims: role + name
│
└── component/
    └── DataSeeder.java          CommandLineRunner: admin + 2 users + 3 books
```

Tests — `backend/src/test/java/com/library/lms/service/`

```
BookServiceTest.java
TransactionServiceTest.java   3 tests: issue-available, issue-waitlist, return-lost
```

---

## 3. Frontend tree — `frontend/src/`

```
main.jsx                      React root
App.jsx                       Router + AuthProvider + Navbar + route table
index.css / App.css           Tailwind layers, serif/sans font setup
│
├── context/
│   └── AuthContext.jsx        user state, login/register/logout, localStorage
│
├── services/
│   └── api.js                 Axios @ :8080/api + Bearer interceptor
│
├── components/
│   ├── ProtectedRoute.jsx     role guard wrapper (allowedRoles prop)
│   ├── Navbar.jsx             top bar
│   ├── BookCard.jsx           split card: cover | genre/title/author/desc/location
│   ├── AlertBanner.jsx        amber active-loan / overdue banner
│   ├── BookFormModal.jsx      admin add/edit book
│   ├── ManageCopiesModal.jsx  admin copy-count adjust
│   ├── UserFormModal.jsx      admin add/edit user
│   └── UserTransactionsModal  admin per-user transaction list
│
└── views/
    ├── auth/
    │   ├── Login.jsx
    │   └── Register.jsx
    ├── user/
    │   ├── UserDashboard.jsx  active-loan banners + full book grid
    │   └── BookDetails.jsx    split layout, location, availability pill
    └── admin/
        ├── AdminDashboard.jsx 3-tab command center (books/users/transactions)
        ├── UserManager.jsx    user directory table
        └── TransactionManager issue/return/queue table
```

### Route table (`App.jsx`)

| Path | Component | Guard |
|---|---|---|
| `/login` | Login | public |
| `/register` | Register | public |
| `/` , `*` | → `/login` | — |
| `/home` | UserDashboard | ROLE_USER, ROLE_ADMIN |
| `/book/:id` | BookDetails | ROLE_USER, ROLE_ADMIN |
| `/admin/dashboard` | AdminDashboard | ROLE_ADMIN |

---

## 4. Dependency graph (who calls whom)

```
                        ┌──────────────┐
                        │  AuthContext │──► api.js ──► POST /auth/login
                        └──────┬───────┘
                               │ user.role
                        ┌──────▼────────┐
                        │ProtectedRoute │
                        └──┬─────────┬──┘
              ROLE_USER    │         │   ROLE_ADMIN
                 ┌─────────▼──┐   ┌──▼──────────────┐
                 │UserDashboard│  │ AdminDashboard  │
                 │ BookDetails │  │ ├ UserManager   │
                 └─────────┬───┘  │ └ TransactionMgr│
                           │      └──┬──────────────┘
                           └────┬────┘
                                │ Axios + Bearer
        ════════════════════════▼══════════════════════  HTTP boundary
                        RateLimitingFilter
                                │
                        JwtAuthenticationFilter ──► JwtUtil
                                │                      │
                                │            CustomUserDetailsService
                                ▼                      │
        ┌──────────┬────────────┴─────────┬────────────┘
        │          │                      │
   AuthController  BookController   AdminController   TransactionController
        │          │                 │      │              │
        │     BookService ◄───────────┘   UserService  TransactionService ★
        │          │                        │              │        ▲
        │          │                        │              │        │
        │          │                        │       QueueMgmtService (@Scheduled)
        │          │                        │              │
        ▼          ▼                        ▼              ▼
   UserRepository  BookRepository    UserRepository   TransactionRepository
        └──────────┴────────┬───────────────┴──────────────┘
                            ▼
                    MongoDB: users · books · transactions
```

★ `TransactionService` is the hub node — it is the only class touching all three
repositories, and `QueueMgmtService` calls back into its `processNextInQueue`.
Any change there ripples through issue, return, penalties, and the 48h sweep.

---

## 5. Transaction state machine (as implemented)

```
   AVAILABLE ──issue (copies>0)──► ISSUED ──return──► RETURNED
       ▲                              │                  │
       │                              │                  │
       │                    issue (copies==0)            │
       │                              ▼                  │
       │                      BOOKED_IN_QUEUE ◄──────────┘
       │                              │  processNextInQueue()
       │                              ▼
       │                      HELD_FOR_PICKUP
       │                         │         │
       └──queue empty────────────┘         └──48h elapsed──► CANCELLED_HOLD
                                                                  │
                                              processNextInQueue()┘
```

**Gap:** there is no `HELD_FOR_PICKUP → ISSUED` transition. The admin handover
described in `architecture.md` has no endpoint, so a held book can only expire.

---

## 6. Where to look for a given concern

| Concern | Start here |
|---|---|
| Login / token / roles | `SecurityConfig`, `JwtUtil`, `AuthContext.jsx` |
| Who can hit which route | `SecurityConfig.securityFilterChain`, `App.jsx` |
| Fines and late fees | `TransactionService.returnBook` |
| Reservation queue | `TransactionService.processNextInQueue`, `QueueMgmtService` |
| Book stock counts | `BookService.updateBook`, `TransactionService` (both mutate `availableCopies`) |
| Search | `BookService.searchBooks` (in-memory), `AdminDashboard.filteredBooks` (client-side) |
| Error responses | `GlobalExceptionHandler` |
| Seed / demo data | `DataSeeder` |
| Design tokens | `IMP_Files/Desigine.md`, `index.css` |

---

## 7. Rebuilding this with graphify

When the sandbox is available:

```
/graphify .              # full build → graphify-out/
/graphify . --wiki       # add a crawlable wiki
/graphify query "how does the reservation queue advance?"
/graphify path "TransactionService" "QueueMgmtService"
/graphify update .       # after code changes (AST-only, free)
```

Exclude `frontend/node_modules/` and `backend/target/` — the repo currently carries
~6,600 files, the vast majority of which are vendored dependencies and build output.
