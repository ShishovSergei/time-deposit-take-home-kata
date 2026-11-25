package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@JdbcTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class DepositSchemaIT {
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void storesAWithdrawalAgainstItsDeposit() {
        jdbc.update("""
                INSERT INTO "timeDeposits" (id, "planType", days, balance)
                VALUES (1, 'basic', 31, 1200.50)
                """);
        jdbc.update("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date)
                VALUES (10, 1, 14.75, DATE '2026-01-11')
                """);

        BigDecimal amount = jdbc.queryForObject("""
                SELECT w.amount FROM withdrawals w
                JOIN "timeDeposits" d ON d.id = w."timeDepositId"
                WHERE d.id = 1 AND w.id = 10 AND w.date = DATE '2026-01-11'
                """, BigDecimal.class);
        assertThat(amount).isEqualByComparingTo("14.75");
    }

    @Test
    void rejectsAWithdrawalWithoutAnExistingDeposit() {
        assertThatThrownBy(() -> jdbc.update("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date)
                VALUES (10, 99, 14.75, DATE '2026-01-11')
                """))
                .isInstanceOf(DataIntegrityViolationException.class)
                .rootCause().isInstanceOfSatisfying(SQLException.class,
                        cause -> assertThat(cause.getSQLState()).isEqualTo("23503")); // Foreign key violation.
    }
}
