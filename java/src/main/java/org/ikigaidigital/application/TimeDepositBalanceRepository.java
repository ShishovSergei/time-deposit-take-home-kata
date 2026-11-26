package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;
import java.util.List;

public interface TimeDepositBalanceRepository {
    /**
     * Returns deposits after {@code afterId} in strictly increasing ID order so the last ID
     * can serve as the next batch's cursor. Row locks are held until the transaction commits or rolls back.
     */
    List<TimeDeposit> findNextBatchForUpdate(long afterId, int limit);

    void applyCredits(List<InterestCredit> credits);

    record InterestCredit(int depositId, BigDecimal amount) {}
}
