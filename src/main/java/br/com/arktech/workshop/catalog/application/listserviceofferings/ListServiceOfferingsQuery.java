package br.com.arktech.workshop.catalog.application.listserviceofferings;

import org.jspecify.annotations.Nullable;

public record ListServiceOfferingsQuery(@Nullable String name, int page, int size) {
}
