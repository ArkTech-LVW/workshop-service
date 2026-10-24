package br.com.arktech.workshop.shared.api.error;

import org.jspecify.annotations.Nullable;

public record ApiFieldError(@Nullable String field, String message) {}
