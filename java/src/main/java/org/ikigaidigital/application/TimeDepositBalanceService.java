package org.ikigaidigital.application;

import org.ikigaidigital.TimeDeposit;
import org.ikigaidigital.TimeDepositCalculator;
import org.ikigaidigital.application.TimeDepositBalanceRepository.InterestCredit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class TimeDepositBalanceService {
    private final TimeDepositBalanceRepository repository;
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();
    private final int batchSize;

    TimeDepositBalanceService(TimeDepositBalanceRepository repository,
                              @Value("${deposits.update-batch-size}") int batchSize) {
        if (batchSize < 1) {
            throw new IllegalArgumentException("deposits.update-batch-size must be positive");
        }
        this.repository = repository;
        this.batchSize = batchSize;
    }

    @Transactional
    public void updateAllBalances() {
        // Start below every valid INTEGER key, including manually assigned negative IDs.
        long afterId = (long) Integer.MIN_VALUE - 1;
        while (true) {
            List<TimeDeposit> deposits = repository.findNextBatchForUpdate(afterId, batchSize);
            if (deposits.isEmpty()) {
                return;
            }
            creditMonthlyInterest(deposits);
            afterId = deposits.get(deposits.size() - 1).getId();
        }
    }

    private void creditMonthlyInterest(List<TimeDeposit> deposits) {
        List<InterestCredit> credits = new ArrayList<>();
        for (TimeDeposit deposit : deposits) {
            BigDecimal interest = calculator.monthlyInterest(deposit);
            if (interest.signum() != 0) {
                credits.add(new InterestCredit(deposit.getId(), interest));
            }
        }
        if (!credits.isEmpty()) {
            repository.applyCredits(credits);
        }
    }
}
