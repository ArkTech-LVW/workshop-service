package br.com.arktech.workshop.shared.application.registry;

import br.com.arktech.workshop.shared.domain.email.Email;

import java.util.Optional;

public interface CustomerContacts {

    Optional<Email> findEmailById(Long customerId);
}
