#  VELOCITY LIMIT ENGINE

> _A financial transaction velocity guard. Evaluates. Enforces. Protects._

---

## ▶️ HOW TO RUN

### 1 — PREREQUISITES

Make sure you have:

- **Java 21+**
- **Gradle 7+**

Verify:

```bash
java -version
gradle -version
```

---

### 2 — BUILD

```bash
./gradlew build
```

> Or if Gradle is installed globally: `gradle build`

---

### 3 — RUN

```bash
./gradlew bootRun
```
---

## 🔎 VERIFY RESULTS

**Request:**

```json
{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "load_amount": "$1234.56",
  "time": "2000-01-01T00:00:00Z"
}
```

**Response:**

```json
{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "accepted": true
}
```

---

## 🧪 TESTS

```bash
./gradlew test
```

Expected: `BUILD SUCCESSFUL`

Tests cover:

-  Idempotency handling
-  Daily transaction count limits
-  Daily amount limits
-  Weekly limits
-  Midnight UTC boundary reset
-  Weekly boundary reset (Monday start)

---

##  VELOCITY RULES

###  Daily Limits _(per UTC day)_

| Rule | Limit |
|------|-------|
| Max accepted loads | **3** |
| Max accepted amount | **$5,000** |

###  Weekly Limits _(Monday → Sunday UTC)_

| Rule | Limit |
|------|-------|
| Max accepted amount | **$20,000** |

> Exceeding **any** limit → load is **declined**.

---

##  EVALUATION LOGIC

```
accepted = true

if dailyCount + 1 > 3        → accepted = false
if dailyAmount + amount > 5000   → accepted = false
if weeklyAmount + amount > 20000 → accepted = false

// Counters only update if accepted = true
```

---

##  IDEMPOTENCY

| Scenario | Result |
|----------|--------|
| Same `load_id` + same `customer_id` | **Ignored. Not counted.** |
| Same `load_id` + different `customer_id` | **Allowed** |

Duplicate loads **never** affect system state or velocity counters.

---

##  TIME HANDLING

All calculations use **UTC**.

**Daily boundary resets at:** `00:00:00 UTC`

```
2000-02-01T23:59:59Z  →  Feb 1
2000-02-02T00:00:00Z  →  Feb 2  ← new day
```

**Weekly boundary:** Weeks start **Monday UTC**

```
Mon Feb 7 00:00:00Z  →  new week begins
```

---

##  ARCHITECTURE

```
LoadService
│
├── IdempotentLoadService
│
├── LoadRepository
├── LoadPerDayRepository
└── LoadPerWeekRepository
```

---

##  DATA MODEL

**`LoadEntity`** — every load attempt

| Field | Description |
|-------|-------------|
| `loadId` | Load identifier |
| `customerId` | Customer |
| `eventTime` | Timestamp |
| `amountCents` | Amount in cents |
| `accepted` | Accepted or declined |

**`LoadPerDay`** — daily aggregates

| Field | Description |
|-------|-------------|
| `customerId` | Customer |
| `dayInUtc` | UTC date |
| `acceptedCount` | Number of accepted loads |
| `acceptedAmountCents` | Total accepted amount |

> Unique constraint: `(customer_id, day_utc)`

**`LoadPerWeek`** — weekly aggregates

| Field | Description |
|-------|-------------|
| `customerId` | Customer |
| `weekStartDate` | Monday of the week |
| `acceptedAmountCents` | Weekly accepted amount |

> Unique constraint: `(customer_id, week_start_date)`

---

##  CONCURRENCY

Uses **`PESSIMISTIC_WRITE`** locking.

Processing order:

1. Lock daily aggregate
2. Lock weekly aggregate
3. Validate limits
4. Update aggregates

No concurrent request can exceed velocity limits.

---

##  DESIGN DECISIONS

**Aggregation tables**
Storing daily/weekly aggregates avoids expensive full-scan queries → faster performance, simpler logic, predictable scaling.

**Pessimistic locking**
Prevents race conditions under concurrent load.

**Monetary values in cents**
Eliminates floating-point precision errors entirely.

---

##  ASSUMPTIONS

- All timestamps are provided in **UTC**
- Duplicate loads **never** affect counters
- Only **accepted** loads contribute to velocity totals

---

##  POSSIBLE IMPROVEMENTS

- Distributed event ingestion
- Metrics & monitoring dashboards
- Rate-limit observability
- Horizontal scaling support
- Event replay capability

---

> _Built for correctness. Designed for scale._