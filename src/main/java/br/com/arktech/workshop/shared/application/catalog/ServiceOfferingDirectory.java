package br.com.arktech.workshop.shared.application.catalog;

import br.com.arktech.workshop.shared.domain.money.Money;

public interface ServiceOfferingDirectory {

    Money laborPriceOf(Long serviceOfferingId);
}
