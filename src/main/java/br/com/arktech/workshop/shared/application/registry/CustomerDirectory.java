package br.com.arktech.workshop.shared.application.registry;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;

import java.util.Optional;

public interface CustomerDirectory {

    Optional<Long> findIdByDocument(CPFCNPJ document);
}
