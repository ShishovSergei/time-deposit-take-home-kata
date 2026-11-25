package org.ikigaidigital.web;

import org.ikigaidigital.application.TimeDepositBalanceService;
import org.ikigaidigital.application.TimeDepositReadRepository;
import org.ikigaidigital.application.TimeDepositView;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class TimeDepositController {
    private final TimeDepositReadRepository repository;
    private final TimeDepositBalanceService balanceService;

    TimeDepositController(TimeDepositReadRepository repository, TimeDepositBalanceService balanceService) {
        this.repository = repository;
        this.balanceService = balanceService;
    }

    @GetMapping("/time-deposits")
    public List<TimeDepositView> listTimeDeposits() {
        return repository.findAll();
    }

    @PostMapping("/time-deposits/update-balances")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateBalances() {
        balanceService.updateAllBalances();
    }
}
