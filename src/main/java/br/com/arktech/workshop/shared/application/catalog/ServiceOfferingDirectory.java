package br.com.arktech.workshop.shared.application.catalog;

import br.com.arktech.workshop.shared.domain.money.Money;

import java.util.Collection;
import java.util.Map;

public interface ServiceOfferingDirectory {

    Money laborPriceOf(Long serviceOfferingId);

    Map<Long, String> namesOf(Collection<Long> serviceOfferingIds);
}
