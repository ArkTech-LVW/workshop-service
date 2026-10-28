package br.com.arktech.workshop.shared.application.inventory;

import br.com.arktech.workshop.shared.domain.money.Money;

public record PartDetails(Money salePrice, int availableQuantity) {
}
