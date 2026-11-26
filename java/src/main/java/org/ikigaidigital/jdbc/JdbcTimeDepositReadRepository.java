package org.ikigaidigital.jdbc;

import org.ikigaidigital.application.TimeDepositReadRepository;
import org.ikigaidigital.application.TimeDepositView;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Repository
class JdbcTimeDepositReadRepository implements TimeDepositReadRepository {
    private final JdbcTemplate jdbc;

    JdbcTimeDepositReadRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<TimeDepositView> findAll() {
        return jdbc.query("""
                SELECT d.id, d."planType", d.balance, d.days,
                       w.id AS withdrawal_id, w.amount, w.date
                FROM "timeDeposits" d
                LEFT JOIN withdrawals w ON w."timeDepositId" = d.id
                ORDER BY d.id, w.id
                """, this::readDeposits);
    }

    private List<TimeDepositView> readDeposits(ResultSet rows) throws SQLException {
        List<TimeDepositView> deposits = new ArrayList<>();
        TimeDepositView current = null;
        while (rows.next()) {
            int id = rows.getInt("id");
            // Ordering by deposit ID keeps all withdrawals for one deposit together.
            if (current == null || current.id() != id) {
                current = new TimeDepositView(id, rows.getString("planType"), rows.getBigDecimal("balance"),
                        rows.getInt("days"), new ArrayList<>());
                deposits.add(current);
            }
            Integer withdrawalId = rows.getObject("withdrawal_id", Integer.class);
            if (withdrawalId != null) {
                current.withdrawals().add(new TimeDepositView.Withdrawal(withdrawalId,
                        rows.getBigDecimal("amount"), rows.getObject("date", LocalDate.class)));
            }
        }
        return deposits;
    }
}
