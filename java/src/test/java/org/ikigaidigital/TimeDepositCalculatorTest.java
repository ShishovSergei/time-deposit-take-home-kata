package org.ikigaidigital;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;

class TimeDepositCalculatorTest {
    private final TimeDepositCalculator calculator = new TimeDepositCalculator();

    @ParameterizedTest(name = "{0}, day {1}: balance {2}")
    @CsvSource({
        "basic,   30, 1200",
        "basic,   31, 1201",
        "student, 30, 1200",
        "student, 31, 1203",
        "student, 365, 1203",
        "student, 366, 1200",
        "premium, 30, 1200",
        "premium, 45, 1200",
        "premium, 46, 1205"
    })
    void appliesMonthlyInterestOnEligibleDays(String planType, int days, double expectedBalance) {
        TimeDeposit deposit = new TimeDeposit(1, planType, 1200.00, days);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(expectedBalance);
    }

    @Test
    void creditsRoundedMonthlyInterestToTheBasicDeposit() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 1_234_567.00, 45);

        calculator.updateBalance(List.of(deposit));

        assertThat(deposit.getBalance()).isEqualTo(1_235_595.81);
    }

    @Test
    void preservesLegacyRoundingAtHalfACent() {
        TimeDeposit deposit = new TimeDeposit(1, "basic", 18.00, 31);

        calculator.updateBalance(List.of(deposit));

        // The original double calculation places the interest just below 0.015.
        assertThat(deposit.getBalance()).isEqualTo(18.01);
    }

    @Test
    void updatesEachOriginalDepositAndPreservesItsOtherFields() {
        TimeDeposit student = new TimeDeposit(7, "student", 1200.00, 365);
        TimeDeposit unknown = new TimeDeposit(8, "other", 1200.00, 46);
        TimeDeposit premium = new TimeDeposit(9, "premium", 2400.00, 46);

        calculator.updateBalance(List.of(student, unknown, premium));

        assertThat(student.getBalance()).isEqualTo(1203.00);
        assertThat(unknown.getBalance()).isEqualTo(1200.00);
        assertThat(premium.getBalance()).isEqualTo(2410.00);
        assertThat(student.getId()).isEqualTo(7);
        assertThat(student.getDays()).isEqualTo(365);
        assertThat(student.getPlanType()).isEqualTo("student");
    }

    @Test
    void acceptsAnEmptyList() {
        assertThatNoException().isThrownBy(() -> calculator.updateBalance(List.of()));
    }
}
