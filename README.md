# Time Deposit Refactoring Kata - Take-Home Assignment

## Run the Java solution

From the repository root, use JDK 17, Maven 3.6.3 or later, and a running Docker engine with Docker Compose v2. The build starts PostgreSQL via Testcontainers and runs the unit and integration tests. The application uses a separate PostgreSQL instance on port 5432 and serves HTTP on port 8080.

```sh
mvn -f java/pom.xml clean verify
docker compose up -d --wait postgres
java -jar java/target/time-deposit-kata-1.0-SNAPSHOT.jar
```

After Spring Boot starts, open [Swagger UI](http://localhost:8080/swagger-ui.html). Expand `GET /time-deposits`, choose **Try it out**, then **Execute**. On a fresh database the response is `200` with `[]`. Flyway creates the tables at startup. The [OpenAPI contract](java/src/main/resources/static/openapi.yaml) describes both operations.

### Try one monthly update

In a second terminal, run `docker compose exec postgres psql -U kata -d time_deposits`. Paste this SQL once into a fresh demo database. The premium balance already includes the recorded withdrawal.

```sql
BEGIN;
WITH premium AS (
    INSERT INTO "timeDeposits" ("planType", days, balance)
    VALUES ('premium', 46, 1200.00)
    RETURNING id
)
INSERT INTO withdrawals ("timeDepositId", amount, date)
SELECT id, 25.50, DATE '2026-02-01' FROM premium;

INSERT INTO "timeDeposits" ("planType", days, balance)
VALUES ('student', 366, 800.00);
COMMIT;
```

1. Execute **GET /time-deposits** in Swagger UI. Check the two deposits against the **Before** column below.
2. Expand **POST /time-deposits/update-balances**, choose **Try it out** and **Execute** once. The request has no body. A successful commit returns `204` with an empty response body.
3. Execute **GET /time-deposits** again. Compare the balances with the **After one POST** column. Both GETs return the same IDs, days and withdrawal history, ordered by deposit ID and then withdrawal ID.

| Plan | Days | Before | After one POST | Withdrawals |
| --- | ---: | ---: | ---: | --- |
| premium | 46 | 1200.00 | 1205.00 | One withdrawal of 25.50 on 2026-02-01 |
| student | 366 | 800.00 | 800.00 | `[]` |

Exit psql with `\q`, stop the Java process with Ctrl+C and run `docker compose down`. Compose retains the data in its named volume.

### Interest rules and assumptions

The original calculator defines the compatibility baseline. It treats the published rates as annual and calculates one month's interest on the current balance per call. Each successful POST represents another accrual, including a repeated request in the same calendar month. For the example above, a second POST brings the premium balance to 1210.02. The operation updates balances only. Days and withdrawal history are inputs or historical records.

| Stored plan type | Annual rate | Eligible days, inclusive |
| --- | ---: | --- |
| `basic` | 1% | 31 onwards |
| `student` | 3% | 31 through 365 |
| `premium` | 5% | 46 onwards |

Plan names are case-sensitive. An unrecognised plan earns zero interest. The shared Java API still updates the supplied `TimeDeposit` objects in place. Both entry points use the original calculation order, `balance × annualRate / 12` in Java `double`, then round the interest to cents using `HALF_UP` on the exact binary value. For example, a basic balance of 18.00 on day 31 earns 0.01 because the intermediate value falls just below 0.015. PostgreSQL adds the rounded credit to the stored `NUMERIC` balance, preserving any existing fractional cents.

### Extending the solution and processing larger lists

- **New plans.** Implement an `InterestRule`, or reuse `fixedAnnualRate` with a day predicate. Register the plan in `TimeDepositCalculator.PLAN_RULES` and add tests for its first and last eligible days. The controller and SQL adapters work with any stored plan name.
- **Bulk updates.** The service reads in primary-key order using `id > lastId`, locks the selected rows, and writes credits in JDBC batches. `deposits.update-batch-size` defaults to 256. Ten times as many deposits means roughly ten times as many rows to process. Application memory for the update stays proportional to the batch size. A single transaction makes the update atomic and holds row locks until commit. Overlapping POSTs can therefore wait for one another.
- **Full-list reads.** GET uses one ordered `LEFT JOIN` and includes deposits with no withdrawals. The response and its in-memory representation grow with the total number of deposits and withdrawals.

### Tests

Use `mvn -f java/pom.xml test` for a fast calculator check. The full `clean verify` above also exercises Flyway, the HTTP contract, repeated accrual, concurrent POSTs and rollback after a failed batch. Inspect the JaCoCo branch report at `java/target/site/jacoco/index.html`. To use an existing PostgreSQL instance for integration tests, supply an **empty, disposable database** via `TEST_DATABASE_URL`, `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD`. The suite migrates the schema and exercises the bulk update against that database.

### How I used AI

I used Codex in IntelliJ IDEA and took a test-first approach to refactoring the calculator. Codex helped me write tests for the existing interest rules, including day boundaries and rounding. I used those tests to check that the refactored calculator produced the same results as the original.

I then wrote the REST controllers and used Codex to suggest integration test cases for both endpoints. For example, one test checks that if a batch update fails, PostgreSQL rolls back the earlier balance changes too. My working rule for Codex was simple: work on one change at a time, run the relevant tests, and stop so I can review the diff.

## XA Bank Time Deposit

### Context
A junior developer implemented domain logic for a time deposit system but did not complete the API functionality. Your task is to refactor the existing codebase to implement all required functionalities based on the provided business requirements, ensuring no breaking changes occur.

### Requirements

1. **API Endpoints**:
    - Create a RESTful API endpoint to update the balances of all time deposits in the database.
    - Create a RESTful API endpoint to retrieve all time deposits.
        - The GET endpoint should return a list of all time deposits with the following schema:
            - `id`
            - `planType`
            - `balance`
            - `days`
            - `withdrawals`

2. **Database Setup**:
    - Store all time deposit plans in a database.
    - Define the following tables:
        - `timeDeposits`:
            - `id`: Integer (primary key)
            - `planType`: String (required)
            - `days`: Integer (required)
            - `balance`: Decimal (required)
        - `withdrawals`:
            - `id`: Integer (primary key)
            - `timeDepositId`: Integer (foreign key, required)
            - `amount`: Decimal (required)
            - `date`: Date (required)

3. **Interest Calculation**:
    - Implement logic to calculate monthly interest based on the plan type:
        - **Basic Plan**: 1% interest
        - **Student Plan**: 3% interest (no interest after 1 year)
        - **Premium Plan**: 5% interest (interest starts after 45 days)
    - No interest is applied for the first 30 days for any existing plans.

4. **Refactoring Constraints**:
    - Do not introduce breaking changes to the shared `TimeDeposit` class or modify the `updateBalance` method signature.
    - Ensure the design is extensible to accommodate future complexities in interest calculations.

5. **Code Quality**:
    - Adhere to SOLID principles, design patterns, and clean code practices where applicable.

6. **AI-Assisted Development**:
    - Set up an AI harness or agent workflow and use it throughout the development for this take-home exercise.
    - Briefly document the tools and setup used (e.g., LLMs, coding assistants, agentic frameworks, configuration).
    - Ensure your AI setup is practical and reproducible.
    - Include any custom rules, system prompts, or agent configurations used.
    - Include a brief summary of which parts of the solution were AI-assisted and why.

### Important Guidelines
- The existing `TimeDepositCalculator.updateBalance` method is functioning correctly. Ensure its behavior remains unchanged after refactoring.
- The final solution must include **exactly two API endpoints**. Do not develop additional endpoints.
- **Do not** create a pull request or a new branch in the ikigai-digital repository. Instead, fork the repository into your own GitHub repository and develop the solution there.
- Handling invalid input or exceptions is not required.
- Use any tools, frameworks, or libraries you find suitable.
- In case of ambiguity, make logical assumptions and justify them in code comments.

### Preferred Stack
- Use an OpenAPI Swagger contract.
- Embrace Hexagonal Architecture.
- Follow atomic commit practices.
- Utilize testcontainers.
- Leverage AI-assisted development tools for code generation, testing, and refactoring.

### Submission Instructions
- Provide clear instructions on how to trigger the endpoints using the Swagger contract.
- Email the link to your public GitHub repository.
