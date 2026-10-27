package br.com.arktech.workshop.catalog.application.listserviceofferings;

import org.jspecify.annotations.Nullable;

public record ListServiceOfferingsQuery(@Nullable String name, @Nullable Boolean active, int page, int size) {
}
