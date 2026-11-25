package org.ikigaidigital.jdbc;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.application.TimeDepositBalanceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
class JdbcTimeDepositBalanceRepository implements TimeDepositBalanceRepository {
    private final JdbcTemplate jdbc;

    JdbcTimeDepositBalanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TimeDeposit> findNextBatchForUpdate(long afterId, int limit) {
        // Lock before calculating so a concurrent POST sees the committed balance.
        return jdbc.query("""
                SELECT id, "planType", balance, days
                FROM "timeDeposits"
                WHERE id > ?
                ORDER BY id
                LIMIT ?
                FOR UPDATE
                """, (row, index) -> new TimeDeposit(row.getInt("id"), row.getString("planType"),
                        row.getDouble("balance"), row.getInt("days")), afterId, limit);
    }

    @Override
    public void applyCredits(List<InterestCredit> credits) {
        jdbc.batchUpdate("""
                UPDATE "timeDeposits" SET balance = balance + ? WHERE id = ?
                """, credits, credits.size(), (statement, credit) -> {
            statement.setBigDecimal(1, credit.amount());
            statement.setInt(2, credit.depositId());
        });
    }
}
