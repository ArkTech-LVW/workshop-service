package br.com.arktech.workshop.registry.application.customer;

import br.com.arktech.workshop.registry.domain.model.Customer;
import br.com.arktech.workshop.shared.domain.document.PersonType;
import br.com.arktech.workshop.shared.domain.phone.Phone;

import org.jspecify.annotations.Nullable;

public record CustomerResult(
        Long id, String name, String document, PersonType personType, String email, @Nullable String phone) {

    public static CustomerResult from(Customer customer) {
        Phone phone = customer.getPhone();
        return new CustomerResult(
                customer.getId(),
                customer.getName(),
                customer.getDocument().value(),
                customer.getPersonType(),
                customer.getEmail().value(),
                phone == null ? null : phone.value());
    }
}
