package org.ikigaidigital;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "deposits.update-batch-size=2")
@ActiveProfiles("test")
class UpdateBalancesIT {
    private static final String UPDATE_URL = "/time-deposits/update-balances";

    @Autowired
    private TestRestTemplate http;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private TransactionTemplate transactions;

    @AfterEach
    void removeFixtures() {
        jdbc.update("DELETE FROM withdrawals WHERE id = 81");
        jdbc.update("DELETE FROM \"timeDeposits\" WHERE id IN (-1, 11, 21, 31)");
    }

    @Test
    void updatesAnEmptyDatabase() {
        postAndExpectNoContent();
        assertThat(http.getForObject("/time-deposits", String.class)).isEqualTo("[]");
    }

    @Test
    void persistsInterestAcrossBatchesAndPreservesWithdrawalHistory() throws Exception {
        addDeposit(-1, "basic", "1200", 46);
        addDeposit(11, "premium", "1200", 46);
        addDeposit(21, "basic", "18", 31);
        addDeposit(31, "student", "1234.567890123456789", 366);
        jdbc.update("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date)
                VALUES (81, 11, 25.50, DATE '2026-02-01')
                """);

        postAndExpectNoContent();

        assertThat(balanceOf(31)).isEqualByComparingTo("1234.567890123456789");
        JSONAssert.assertEquals("""
                [
                  {"id":-1,"planType":"basic","balance":1201,"days":46,"withdrawals":[]},
                  {"id":11,"planType":"premium","balance":1205,"days":46,"withdrawals":[
                    {"id":81,"amount":25.50,"date":"2026-02-01"}]},
                  {"id":21,"planType":"basic","balance":18.01,"days":31,"withdrawals":[]},
                  {"id":31,"planType":"student","balance":1234.567890123456789,"days":366,"withdrawals":[]}
                ]
                """, http.getForObject("/time-deposits", String.class), true);
    }

    @Test
    void continuesToTheNextBatchWhenTheFirstBatchEarnsNoInterest() {
        addDeposit(11, "basic", "1200", 30);
        addDeposit(21, "student", "1200", 366);
        addDeposit(31, "premium", "1200", 46);

        postAndExpectNoContent();

        assertThat(balanceOf(11)).isEqualByComparingTo("1200");
        assertThat(balanceOf(21)).isEqualByComparingTo("1200");
        assertThat(balanceOf(31)).isEqualByComparingTo("1205");
    }

    @Test
    void eachPostAccruesInterestOnTheCurrentDecimalBalance() {
        addDeposit(11, "premium", "1200.000000000000001", 46);
        postAndExpectNoContent();
        assertThat(balanceOf(11)).isEqualByComparingTo("1205.000000000000001");
        postAndExpectNoContent();
        assertThat(balanceOf(11)).isEqualByComparingTo("1210.020000000000001");
    }

    @Test
    void rollsBackEarlierBatchesWhenTheDatabaseRejectsALaterCredit() {
        addDeposit(11, "basic", "1200", 31);
        addDeposit(21, "basic", "1200", 31);
        addDeposit(31, "basic", "1200", 31);
        // Only the third deposit, in the second batch, will reject the new balance.
        jdbc.execute("""
                ALTER TABLE "timeDeposits" ADD CONSTRAINT test_reject_credit
                CHECK (id <> 31 OR balance <= 1200)
                """);
        try {
            assertThat(http.postForEntity(UPDATE_URL, null, String.class).getStatusCode())
                    .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
            for (int id : new int[]{11, 21, 31}) {
                assertThat(balanceOf(id)).isEqualByComparingTo("1200");
            }
        } finally {
            jdbc.execute("ALTER TABLE \"timeDeposits\" DROP CONSTRAINT test_reject_credit");
        }
    }

    @Test
    void concurrentPostsHaveTheSameEffectAsTwoSuccessiveUpdates() throws Exception {
        addDeposit(11, "premium", "1200", 46);
        ExecutorService clients = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> requests = transactions.execute(status -> {
                jdbc.queryForObject("SELECT id FROM \"timeDeposits\" WHERE id = 11 FOR UPDATE", Integer.class);
                Future<?> first = clients.submit(this::postAndExpectNoContent);
                Future<?> second = clients.submit(this::postAndExpectNoContent);
                // Keep the row locked until both requests are competing for it in PostgreSQL.
                await().atMost(Duration.ofSeconds(15)).until(() -> jdbc.queryForObject("""
                        SELECT count(*) FROM pg_stat_activity
                        WHERE datname = current_database() AND cardinality(pg_blocking_pids(pid)) > 0
                        """, Integer.class) == 2);
                return List.of(first, second);
            });
            for (Future<?> request : requests) {
                request.get(20, TimeUnit.SECONDS);
            }
            assertThat(balanceOf(11)).isEqualByComparingTo("1210.02");
        } finally {
            clients.shutdownNow();
        }
    }

    private void postAndExpectNoContent() {
        ResponseEntity<String> response = http.postForEntity(UPDATE_URL, null, String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
    }

    private void addDeposit(int id, String planType, String balance, int days) {
        jdbc.update("""
                INSERT INTO "timeDeposits" (id, "planType", balance, days) VALUES (?, ?, ?, ?)
                """, id, planType, new BigDecimal(balance), days);
    }

    private BigDecimal balanceOf(int id) {
        return jdbc.queryForObject("SELECT balance FROM \"timeDeposits\" WHERE id = ?", BigDecimal.class, id);
    }
}
