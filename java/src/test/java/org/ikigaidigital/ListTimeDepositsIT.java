package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.json.JsonCompareMode;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ListTimeDepositsIT {
    @Autowired
    private MockMvc http;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void returnsAnEmptyArrayWhenThereAreNoDeposits() throws Exception {
        http.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("[]", JsonCompareMode.STRICT));
    }

    @Test
    void listsDepositsAndTheirOwnWithdrawalsInIdOrder() throws Exception {
        jdbc.update("""
                INSERT INTO "timeDeposits" (id, "planType", balance, days) VALUES
                    (3, 'student', 200.03, 366),
                    (1, 'basic', 1200.50, 31),
                    (2, 'premium', 85.25, 46)
                """);
        jdbc.update("""
                INSERT INTO withdrawals (id, "timeDepositId", amount, date) VALUES
                    (30, 1, 4.25, DATE '2026-01-01'),
                    (20, 3, 9.99, DATE '2026-02-03'),
                    (10, 1, 14.75, DATE '2026-01-11')
                """);

        http.perform(get("/time-deposits"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        [
                          {"id": 1, "planType": "basic", "balance": 1200.50, "days": 31, "withdrawals": [
                            {"id": 10, "amount": 14.75, "date": "2026-01-11"},
                            {"id": 30, "amount": 4.25, "date": "2026-01-01"}
                          ]},
                          {"id": 2, "planType": "premium", "balance": 85.25, "days": 46, "withdrawals": []},
                          {"id": 3, "planType": "student", "balance": 200.03, "days": 366, "withdrawals": [
                            {"id": 20, "amount": 9.99, "date": "2026-02-03"}
                          ]}
                        ]
                        """, JsonCompareMode.STRICT));
    }
}
