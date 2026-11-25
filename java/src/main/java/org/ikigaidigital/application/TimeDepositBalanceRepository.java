package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;

import java.math.BigDecimal;
import java.util.List;

public interface TimeDepositBalanceRepository {
    List<TimeDeposit> findNextBatchForUpdate(long afterId, int limit);

    void applyCredits(List<InterestCredit> credits);

    record InterestCredit(int depositId, BigDecimal amount) {}
}
