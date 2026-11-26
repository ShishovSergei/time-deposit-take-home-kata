package org.ikigaidigital;

import java.util.function.IntPredicate;

@FunctionalInterface
interface InterestRule {
    double monthlyInterest(TimeDeposit deposit);

    static InterestRule fixedAnnualRate(double annualRate, IntPredicate eligibleOnDay) {
        // The legacy calculator applies one twelfth of the annual rate per call.
        return deposit -> eligibleOnDay.test(deposit.getDays())
                ? deposit.getBalance() * annualRate / 12
                : 0;
    }
}
