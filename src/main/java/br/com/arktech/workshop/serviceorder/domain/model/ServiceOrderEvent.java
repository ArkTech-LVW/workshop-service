package br.com.arktech.workshop.serviceorder.domain.model;

import java.util.UUID;

public sealed interface ServiceOrderEvent {

    UUID publicId();

    record QuoteSent(UUID publicId) implements ServiceOrderEvent {
    }

    record QuoteApproved(UUID publicId) implements ServiceOrderEvent {
    }

    record QuoteRejected(UUID publicId) implements ServiceOrderEvent {
    }

    record QuoteReopened(UUID publicId) implements ServiceOrderEvent {
    }

    record ServiceOrderCompleted(UUID publicId) implements ServiceOrderEvent {
    }

    record ServiceOrderCanceled(UUID publicId) implements ServiceOrderEvent {
    }
}
