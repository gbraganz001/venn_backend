▶️ How to Run the Application
1️⃣ Prerequisites

Ensure the following are installed:

Java 21+

Gradle 7+

Verify installation:

java -version
gradle -version
2️⃣ Build the Project
./gradlew build

or if Gradle is installed globally:

gradle build
3️⃣ Run the Application

Start the Spring Boot application:

./gradlew bootRun

The service will start at:

http://localhost:8080
🔎 How to Verify Results

The application evaluates load requests and determines whether the transaction should be accepted or declined based on velocity rules.

Example Request
{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "load_amount": "$1234.56",
  "time": "2000-01-01T00:00:00Z"
}
Example Response
{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "accepted": true
}
🧪 Running the Tests

Integration tests verify the velocity rules and system behavior.

Run:

./gradlew test

Expected output:

BUILD SUCCESSFUL

Tests validate:

✔ Idempotency handling
✔ Daily transaction count limits
✔ Daily amount limits
✔ Weekly limits
✔ Midnight UTC boundary reset
✔ Weekly boundary reset (Monday start)

📘 Problem Overview

A financial system processes load transactions for customer accounts.

Each incoming transaction must be evaluated against velocity limits to determine whether it should be accepted or declined.

The system ensures:

Idempotent processing

Correct daily and weekly aggregation

UTC time boundary enforcement

Concurrency-safe updates

📥 Load Request Format

Each load request contains:

Field	Description
id	Unique identifier for the load
customer_id	Customer identifier
load_amount	Dollar amount
time	ISO-8601 timestamp

Example:

{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "load_amount": "$1234.56",
  "time": "2000-01-01T00:00:00Z"
}
📤 Response Format

For every processed request:

{
  "id": "LOAD_ID",
  "customer_id": "CUSTOMER_ID",
  "accepted": true | false
}
📏 Velocity Rules

Velocity limits apply per customer.

📅 Daily Limits

Per UTC day:

Rule	Limit
Maximum accepted loads	3
Maximum accepted amount	$5,000

If either rule is exceeded, the load is declined.

📆 Weekly Limits

Per UTC week (Monday → Sunday):

Rule	Limit
Maximum accepted amount	$20,000

If accepting the transaction exceeds the weekly limit, it must be declined.

🔁 Idempotency

Duplicate loads must not affect system state.

If the system receives the same request twice:

same load id + same customer

The duplicate request is:

ignored

not processed

not counted toward limits

However:

same load id + different customer → allowed
⏱ Time Handling

All calculations use UTC.

Daily Boundary

Daily counters reset at:

00:00:00 UTC

Example:

2000-02-01T23:59:59Z → Feb 1
2000-02-02T00:00:00Z → Feb 2
Weekly Boundary

Weeks start on Monday (UTC).

Example:

Mon Feb 7 00:00:00Z → new week

Weekly counters reset at this boundary.

🏗 Architecture
LoadService
│
├── IdempotentLoadService
│
├── LoadRepository
├── LoadPerDayRepository
└── LoadPerWeekRepository
🗄 Data Model

The system maintains three tables.

LoadEntity

Stores each load attempt.

Field	Description
loadId	Load identifier
customerId	Customer
eventTime	Timestamp
amountCents	Amount in cents
accepted	Accepted or declined
LoadPerDay

Stores daily aggregates.

Field	Description
customerId	Customer
dayInUtc	UTC date
acceptedCount	Number of accepted loads
acceptedAmountCents	Total accepted amount

Unique constraint:

(customer_id, day_utc)
LoadPerWeek

Stores weekly aggregates.

Field	Description
customerId	Customer
weekStartDate	Monday of the week
acceptedAmountCents	Weekly accepted amount

Unique constraint:

(customer_id, week_start_date)
🔒 Concurrency Handling

To prevent race conditions, the system uses:

PESSIMISTIC_WRITE locking

Processing steps:

Lock daily aggregate

Lock weekly aggregate

Validate limits

Update aggregates

This ensures concurrent requests cannot exceed limits.

⚙ Limit Evaluation Logic
accepted = true

if dailyCount + 1 > 3
    accepted = false

if dailyAmount + amount > 5000
    accepted = false

if weeklyAmount + amount > 20000
    accepted = false

Counters update only if the transaction is accepted.

🧠 Design Decisions
Aggregation tables

Storing daily and weekly aggregates avoids expensive queries over all loads.

Benefits:

faster performance

simpler queries

predictable scaling

Pessimistic locking

Prevents concurrent requests from exceeding velocity limits.

Monetary values stored in cents

Avoids floating-point precision errors.

📌 Assumptions

timestamps are provided in UTC

duplicate loads must not affect counters

only accepted loads contribute to velocity totals

🔮 Possible Improvements

Future enhancements may include:

distributed event ingestion

metrics and monitoring

rate-limit dashboards

horizontal scaling support

event replay capability