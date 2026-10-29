package br.com.arktech.workshop.inventory.application.withdrawstock;

import br.com.arktech.workshop.shared.application.serviceorder.QuoteApprovedEvent;

import lombok.RequiredArgsConstructor;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/** Synchronous on purpose: the withdrawal runs in the approval transaction and its failure aborts the approval. */
@Component
@RequiredArgsConstructor
class QuoteApprovedListener {

    private final WithdrawStockUseCase withdrawStockUseCase;

    @EventListener
    void on(QuoteApprovedEvent event) {
        withdrawStockUseCase.execute(new WithdrawStockCommand(
                event.serviceOrderId(),
                event.parts().stream()
                        .map(part -> new WithdrawStockCommand.Item(part.partId(), part.quantity()))
                        .toList(),
                event.approvedByUserId()));
    }
}
