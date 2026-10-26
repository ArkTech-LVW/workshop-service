package br.com.arktech.workshop.registry.domain.repository;

import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;

import org.jspecify.annotations.Nullable;

public record CustomerFilter(@Nullable CPFCNPJ document) {
}
