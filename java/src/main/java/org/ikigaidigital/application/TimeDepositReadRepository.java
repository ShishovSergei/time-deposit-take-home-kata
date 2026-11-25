package org.ikigaidigital.application;

import java.util.List;

public interface TimeDepositReadRepository {
    List<TimeDepositView> findAll();
}
