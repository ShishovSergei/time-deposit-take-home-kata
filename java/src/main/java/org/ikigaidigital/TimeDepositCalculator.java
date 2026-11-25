package org.ikigaidigital;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

import static org.ikigaidigital.InterestRule.fixedAnnualRate;

public class TimeDepositCalculator {
    private static final InterestRule NO_INTEREST = deposit -> 0;
    private static final Map<String, InterestRule> PLAN_RULES = Map.of(
            "basic",   fixedAnnualRate(0.01, days -> days > 30),
            "student", fixedAnnualRate(0.03, days -> days > 30 && days <= 365),
            "premium", fixedAnnualRate(0.05, days -> days > 45)
    );

    public void updateBalance(List<TimeDeposit> deposits) {
        for (TimeDeposit deposit : deposits) {
            BigDecimal interest = monthlyInterest(deposit);
            deposit.setBalance(deposit.getBalance() + interest.doubleValue());
        }
    }

    BigDecimal monthlyInterest(TimeDeposit deposit) {
        InterestRule rule = PLAN_RULES.getOrDefault(deposit.getPlanType(), NO_INTEREST);
        double interest = rule.monthlyInterest(deposit);
        // Preserve the original binary value at half-cent boundaries before rounding.
        return new BigDecimal(interest).setScale(2, RoundingMode.HALF_UP);
    }
}
