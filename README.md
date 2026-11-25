# Time Deposit Refactoring Kata - Take-Home Assignment

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

### Run the Java application

From the repository root, use JDK 17, Maven and Docker Compose:

```sh
docker compose up -d --wait postgres
mvn -f java/pom.xml spring-boot:run
```

Flyway creates the tables on startup. Open [Swagger UI](http://localhost:8080/swagger-ui.html), expand `GET /time-deposits`, select **Try it out**, then **Execute**. A fresh database returns `200` with `[]`. Populated results list deposits and their withdrawals in ascending ID order.

To apply one month's interest, execute `POST /time-deposits/update-balances` with no request body. A successful call returns `204`. Run GET again to see the new balances. Each additional POST accrues another month's interest on the current balances. The update changes only the balance and treats withdrawals as history. Both operations are described in the [OpenAPI contract](java/src/main/resources/static/openapi.yaml).

The update runs in one transaction and locks deposits in ascending ID order before calculating interest. The application holds one batch of deposits at a time (256 by default, configured by `deposits.update-batch-size`). PostgreSQL holds the row locks until commit and rolls back the entire update if a batch fails.

Stop the application with Ctrl+C and run `docker compose down` to stop PostgreSQL. The named volume retains the data.

### Verify the Java module

Run `mvn -f java/pom.xml test` for the calculator tests. Run `mvn -f java/pom.xml verify` for the full build, including the schema and HTTP integration tests. Testcontainers starts its own PostgreSQL instance through Docker; the Compose service is only needed when running the application.

To run the same integration tests against a dedicated disposable PostgreSQL database, set `TEST_DATABASE_URL` to its JDBC URL and set `TEST_DATABASE_USERNAME` and `TEST_DATABASE_PASSWORD`. Flyway migrates that database. Review the expected balances against the business rules, inspect uncovered branches in `java/target/site/jacoco/index.html`, and review the diff before starting the next stage.
