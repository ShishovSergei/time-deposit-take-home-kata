package org.ikigaidigital.web;

import org.ikigaidigital.application.TimeDepositReadRepository;
import org.ikigaidigital.application.TimeDepositView;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
class TimeDepositController {
    private final TimeDepositReadRepository repository;

    TimeDepositController(TimeDepositReadRepository repository) {
        this.repository = repository;
    }

    @GetMapping("/time-deposits")
    public List<TimeDepositView> listTimeDeposits() {
        return repository.findAll();
    }
}
