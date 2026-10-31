package br.com.arktech.workshop.inventory.application.liststockadjustmentrequests;

import br.com.arktech.workshop.inventory.domain.model.StockAdjustmentRequestStatus;

import org.jspecify.annotations.Nullable;

public record ListStockAdjustmentRequestsQuery(@Nullable StockAdjustmentRequestStatus status, int page, int size) {
}
