package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import java.math.BigDecimal;
import java.util.List;

public record QuoteResult(
        List<QuoteItemResult> services,
        List<QuoteItemResult> parts,
        BigDecimal servicesTotal,
        BigDecimal partsTotal,
        BigDecimal totalPrice) {

    public QuoteResult {
        services = List.copyOf(services);
        parts = List.copyOf(parts);
    }
}
