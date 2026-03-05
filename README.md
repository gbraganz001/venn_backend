Velocity Load Processor
Overview

This project implements a velocity limit processor for financial load transactions.
Each incoming load request is evaluated against a set of daily and weekly limits per customer to determine whether the transaction should be accepted or declined.

The service ensures:

Idempotency (duplicate loads are ignored)

Concurrency safety

Accurate daily and weekly aggregation

UTC-based time handling

The implementation uses Spring Boot, Spring Data JPA, and transactional database locking to guarantee correctness.

Problem Statement

A financial system processes load transactions for customer accounts.

Each load request contains:

id – unique identifier of the load event

customer_id – customer receiving the load

load_amount – dollar amount

time – timestamp of the event

Example request:

{
"id": "LOAD_ID",
"customer_id": "CUSTOMER_ID",
"load_amount": "$1234.56",
"time": "2000-01-01T00:00:00Z"
}

For every request the system must output:

{
"id": "LOAD_ID",
"customer_id": "CUSTOMER_ID",
"accepted": true | false
}
Business Rules

Velocity limits are enforced per customer.

Daily Limits

Per UTC day:

Rule	Limit
Maximum number of accepted loads	3
Maximum total accepted amount	$5,000

If a load causes either of these limits to be exceeded, the load must be declined.

Weekly Limits

Per UTC week (Monday → Sunday):

Rule	Limit
Maximum total accepted amount	$20,000

If accepting the load would exceed this limit, it must be declined.

Idempotency

If the system receives the same id for the same customer more than once:

Only the first request should be processed.

Subsequent duplicates must produce no response and must not affect counters.

However:

The same id for different customers is allowed.

Time Rules

All calculations use UTC.

Daily boundary

A new day starts at:

00:00:00 UTC

Example:

2000-02-01T23:59:59Z  → Feb 1
2000-02-02T00:00:00Z  → Feb 2

Daily counters reset at midnight.

Weekly boundary

Weeks start on Monday (UTC).

Example:

Mon Feb 7 00:00:00Z → new week

Weekly counters reset at this boundary.

Architecture

The system is implemented as a Spring Boot service with the following components.

Core Service
LoadService

Responsible for:

Processing load requests

Checking limits

Updating aggregates

Returning responses

Key method:

Optional<LoadResponse> process(LoadRequest request)

Returns:

Optional.empty() → duplicate load

LoadResponse → accepted or declined

Idempotency
IdempotentLoadService

Checks whether a load ID has already been processed for the same customer.

existsByCustomerIdAndLoadId()

If duplicate:

return Optional.empty()
Data Model

The service maintains three tables.

LoadEntity

Stores every load attempt.

Field	Description
id	database id
loadId	load identifier
customerId	customer
eventTime	timestamp
amountCents	amount in cents
accepted	accepted / declined
LoadPerDay

Stores daily aggregates.

Field	Description
customerId	customer
dayInUtc	UTC day
acceptedCount	number of accepted loads
acceptedAmountCents	accepted total

Unique constraint:

(customer_id, day_utc)
LoadPerWeek

Stores weekly aggregates.

Field	Description
customerId	customer
weekStartDate	Monday of the week
acceptedAmountCents	weekly accepted total

Unique constraint:

(customer_id, week_start_date)
Concurrency Handling

To ensure correct limits under concurrent requests, the service uses:

PESSIMISTIC_WRITE locks

Repositories provide locking queries:

lockByCustomerAndDay()
lockByCustomerAndWeek()

These ensure that only one transaction can update the aggregate rows at a time.

Time Calculations

All time calculations use UTC.

UTC Day
LocalDate utcDay = instant.atZone(UTC).toLocalDate()
Week Start (Monday)
LocalDate weekStart =
utcDay.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));

This ensures consistent week boundaries.

Implementation Flow

Processing a request:

1. Check idempotency
2. Persist load attempt
3. Lock daily aggregate
4. Lock weekly aggregate
5. Validate limits
6. If accepted:
   update aggregates
7. Save result
8. Return response
   Limit Validation Logic
   accepted = true

if dailyCount + 1 > 3
accepted = false

if dailyAmount + amount > 5000
accepted = false

if weeklyAmount + amount > 20000
accepted = false

Counters update only if accepted.

Testing

Integration tests validate:

Idempotency

Duplicate loads are ignored.

Daily limits

Max 3 loads

Max $5000

Weekly limits

Max $20000.

Midnight boundary

Daily counters reset at midnight.

Week boundary

Weekly counters reset on Monday.

Tests run with:

@SpringBootTest
@ActiveProfiles("test")

Database is cleared between tests.

Running the Application
Build
mvn clean install
Run
mvn spring-boot:run
Design Considerations
Why store aggregates instead of computing from raw loads?

Benefits:

Faster reads

Lower query cost

Predictable performance

Why pessimistic locking?

Prevents race conditions where two concurrent requests could:

read same totals
both pass validation
exceed limits

Locks ensure atomic updates.

Why use cents instead of decimals?

Avoids floating point precision issues.

Assumptions

Time values are trusted and provided in UTC.

Duplicate loads with the same id must not produce a second response.

Limits apply only to accepted loads.

Future Improvements

Possible enhancements:

Event streaming ingestion

Horizontal scaling

Caching layer

Rate-limit metrics

Observability (Prometheus / tracing)