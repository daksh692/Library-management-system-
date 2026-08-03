# Project Memory — Library Management System

Living record of what is built, what is partial, and what is still open.
Last updated: 2026-07-26

---

## 1. Stack as actually implemented

| Layer | Choice | Notes |
|---|---|---|
| Backend | Spring Boot 3.2.3, Java 17 | Maven, `com.library:lms` |
| Security | Spring Security + jjwt 0.11.5 | stateless, BCrypt |
| Database | MongoDB `mongodb://localhost:27017/lms` | no Docker compose file yet |
| Rate limiting | Bucket4j 8.9.0 (`bucket4j-core`) | in-memory `ConcurrentHashMap` |
| Frontend | React + Vite, React Router, Axios | Tailwind via `index.css` |
| Icons | lucide-react | |
| Tests | JUnit 5 + Mockito | 2 test classes only |

---

## 2. Phase status

### Phase 1 — Foundation & Database Design — **DONE**

- `LmsApplication` with `@EnableScheduling`.
- Models: `Book`, `User` (implements `UserDetails`), `Transaction` — all Lombok `@Data @Builder`, all match the PRD schema.
- Repositories: `BookRepository`, `UserRepository`, `TransactionRepository`.
- `DataSeeder` (CommandLineRunner) seeds `admin/admin123`, `user1/password`, `user2/password`, and 3 books.
- Frontend scaffolded with Vite; routing configured in `App.jsx`.
- **Not done:** no Docker/`docker-compose.yml` despite Phases.md listing it.

### Phase 2 — Security, Auth & Base APIs — **DONE (with caveats)**

- `SecurityConfig`: CSRF off, CORS limited to `http://localhost:5173`, stateless sessions, `/api/auth/**` + `/api/public/**` open, `/api/admin/**` → `ROLE_ADMIN`, `/api/user/**` → `ROLE_USER`.
- `JwtAuthenticationFilter` + `JwtUtil` (HS256, 10-hour expiry, `role` and `name` claims, subject = `userId`).
- `CustomUserDetailsService` resolves by `userId`.
- `AuthController`: `POST /api/auth/login`, `POST /api/auth/register` (auto-generates `LIB-2026-XXXX`, rejects duplicate phone).
- `RateLimitingFilter`: 5/min auth, 30/min public, 100/min authenticated.
- `GlobalExceptionHandler`: field-level validation errors + generic 500 message.
- CRUD book endpoints under `AdminController`.

### Phase 3 — Core UI — **DONE (with gaps)**

Built: `AuthContext`, `ProtectedRoute` (role-gated), `Navbar`, `BookCard`, `AlertBanner`, `Login`, `Register`, `UserDashboard`, `BookDetails`, `AdminDashboard` (3-tab: Books / Users / Transactions), `UserManager`, `TransactionManager`, `BookFormModal`, `ManageCopiesModal`, `UserFormModal`, `UserTransactionsModal`.
`services/api.js` = Axios instance with a Bearer-token request interceptor reading `localStorage`.

**Missing from the PRD user dashboard:**

- Reading History panel (last 4 returned books) — marked as a placeholder comment in `UserDashboard.jsx`.
- Personalized Recommendations carousel (5 books by `previouslyReadGenre`) — not started.
- "New Collections" renders *all* books, not a 4x2 grid ordered by creation date.
- Related Books panel on `BookDetails` — not started.
- Header search bar wired to `/public/books/search` — endpoint exists, UI does not call it.
- `BookDetails` "Reserve Next" button is a dead control (no `onClick`).

### Phase 4 — Booking & Penalty Engine — **MOSTLY DONE**

`TransactionService`:

- `issueBook` — issues if `availableCopies > 0` (14-day due date), else appends to `BOOKED_IN_QUEUE` with a computed `queueSequence`.
- `returnBook` — sets `RETURNED`, late fee $1/day, adds `book.price` (default $50) for `DAMAGED`/`LOST`, decrements `totalCopies` on `LOST`.
- `processNextInQueue` — promotes the lowest `queueSequence` to `HELD_FOR_PICKUP` and stamps `issueDate` as the window start; restocks the copy if the queue is empty.

`QueueMgmtService.sweepExpiredHolds` — `@Scheduled(fixedRate = 3600000)`, cancels holds older than 48h (status `CANCELLED_HOLD`) and advances the queue.

**Open in Phase 4:**

- Estimated availability date (closest due date of issued copies) — never computed anywhere.
- `penaltyPaid` exists on the model but nothing ever sets it; no fine-payment endpoint or UI.
- `HELD_FOR_PICKUP` → `ISSUED` handover has no endpoint; the state machine in architecture.md is therefore incomplete.
- `CANCELLED_HOLD` is not in the PRD's documented status enum — either add it to the PRD or rename.

### Phase 5 — Refinement, Integration & Testing — **BARELY STARTED**

- Only two test classes exist, 6 tests total:
  - `BookServiceTest` — add-book, update-copies (grow only), soft-delete.
  - `TransactionServiceTest` — issue-available, issue-waitlist, return-lost.
- The copy-shrink path is untested and can drive `availableCopies` negative (see defect #9).
- No tests for: `QueueMgmtService` 48h sweep, auth/JWT, rate limiting, controllers, or any integration slice.
- No end-to-end user-flow validation performed.

---

## 3. Known defects / risks (verified in source)

Ordered roughly by severity.

1. **Password hash leaks through the API.** `AdminController` returns the raw `User` entity. `User` is `@Data`, so `passwordHash` — and `password` via the `UserDetails` getter — serialize into every `/api/admin/users*` response. Needs a `UserDto` response mapping or `@JsonIgnore`.
2. **JWT signing key is regenerated on every boot.** `JwtUtil` uses `Keys.secretKeyFor(HS256)` as a field initializer, so every restart invalidates all tokens and no second instance can validate them. Move to an env-var-backed base64 secret. (Upside: nothing is hardcoded, so Rules #6 is technically satisfied.)
3. **No `@Transactional` on issue/return.** `issueBook` computes `queueSequence` from a `count()` and `returnBook` does multi-document writes — both race under concurrency and can produce duplicate queue positions or lost copy counts.
4. **`RuntimeException` used for all domain errors.** `GlobalExceptionHandler` maps them to a generic 500, so "book not found" surfaces as a server error. Needs typed exceptions + proper 404/409 mapping.
5. **Rate limiting is per-IP only.** Rules.md #1 asks for per-IP *and* per-account limits, exponential backoff, and configurable thresholds. All three are absent; the numbers are hardcoded in `createNewBucket`. Buckets also never evict — unbounded map growth.
6. **`ROLE_ADMIN` can reach `/home` but not its API.** `App.jsx` allows admins into `UserDashboard`, which calls `/api/user/transactions/active`; `SecurityConfig` restricts `/api/user/**` to `ROLE_USER`, so admins get a silent 403.
7. **In-memory search.** `BookService.searchBooks` and `getAllActiveBooks` call `findAll()` and filter in Java. Will not scale; should be a Mongo query/text index.
8. **`UserService.getAllUsers` ignores `isDeleted`.** Soft-deleted users still appear in the admin directory.
9. **`updateBook` copy math is order-dependent.** `diff` is computed against `book.getTotalCopies()` before it is overwritten — correct today, but fragile and untested for the shrink case (can drive `availableCopies` negative).
10. **Error logging uses `System.err` + `printStackTrace`.** Rules.md #4 wants server-side structured logging; switch to SLF4J.
11. **Debug logging is on in `application.properties`** (`mongodb=DEBUG`, `security=DEBUG`) — noisy and leaks query detail in any shared environment.
12. **`backend/target/` is not gitignored.** The root `.gitignore` covers IntelliJ/Eclipse/NetBeans/VS Code output but never `target/`, and compiled `.class` files are already on disk. `frontend/.gitignore` correctly covers `node_modules` and `dist`. Add `target/` before the next commit.

---

## 4. Rules.md compliance ledger

| # | Rule | Status |
|---|---|---|
| 1 | Tiered rate limiting | **Partial** — tiers exist; no per-account limit, no backoff, not configurable |
| 2 | Strict input validation | **Partial** — `@Valid` on controllers; no `location` (`Aisle-Shelf-Bin`) or ISBN-13 pattern enforcement |
| 3 | Dependency audit | **Not started** — no `mvn dependency-check` or `npm audit` run recorded |
| 4 | No info leakage in errors | **Partial** — generic 500s are correct; `printStackTrace` + `passwordHash` serialization undercut it |
| 5 | File upload safety (2.5 MB, jpeg/jpg/png) | **N/A so far** — no upload path exists; covers are external URLs in `photoUrl` |
| 6 | No hardcoded secrets | **Pass** — nothing hardcoded. Caveat: seeded demo credentials in `DataSeeder` should not ship to prod |
| 7 | Code quality | **Partial** |
| 8 | Documentation | **Partial** — services have Javadoc; controllers, DTOs, models, and all frontend components do not |
| 9 | Unit tests | **Partial** — 2 service test classes, no controller/integration coverage |

---

## 5. Indexing state

Already annotated: `Book.isbn`, `Book.name`, `Book.author`, `Book.genre`; `User.userId` (unique), `User.phone` (unique).
Missing: any index on `Transaction` — `bookId`, `userId`, and `status` are all queried on every hot path (`findByBookId`, `findByUserId`, `findByStatus`).

---

## 6. Reference credentials (local seed data only)

`admin / admin123` (ROLE_ADMIN) · `user1 / password` · `user2 / password`
Seeded books: Effective Java (A-1-S1), The Pragmatic Programmer (A-1-S2), Clean Code (A-2-S1).

---

## 7. Suggested next actions

1. Fix the password-hash leak and the JWT key — both are one-file changes with outsized impact.
2. Add `@Transactional` and typed exceptions to the transaction path, then test the 48h sweep.
3. Close the Phase 3 dashboard gaps (history, recommendations, related books, wired search).
4. Add the `HELD_FOR_PICKUP` → `ISSUED` handover endpoint to complete the state machine.
5. Run the Rules.md #3 dependency audit and index `Transaction`.
