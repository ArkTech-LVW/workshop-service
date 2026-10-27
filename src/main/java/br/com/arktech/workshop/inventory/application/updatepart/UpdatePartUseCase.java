package br.com.arktech.workshop.inventory.application.updatepart;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.model.Sku;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.money.Money;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UpdatePartUseCase implements UseCase<UpdatePartCommand, PartResult> {

    private final PartRepository partRepository;

    @Override
    @Transactional
    public PartResult execute(UpdatePartCommand command) {
        Part part = partRepository.findRequiredById(command.id());
        Sku sku = new Sku(command.sku());
        if (partRepository.existsBySkuAndIdNot(sku, part.getId())) {
            throw new ConflictException("part.skuAlreadyExists", sku.value());
        }

        part.changeSku(sku);
        part.rename(command.name());
        part.classify(command.type(), command.unit());
        part.changeSalePrice(new Money(command.salePrice()));
        part.changeMinimumStock(command.minimumStock());
        return PartResult.from(partRepository.save(part));
    }
}
