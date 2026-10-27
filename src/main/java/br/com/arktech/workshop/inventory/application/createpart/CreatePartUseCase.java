package br.com.arktech.workshop.inventory.application.createpart;

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
public class CreatePartUseCase implements UseCase<CreatePartCommand, PartResult> {

    private final PartRepository partRepository;

    @Override
    @Transactional
    public PartResult execute(CreatePartCommand command) {
        Sku sku = new Sku(command.sku());
        if (partRepository.existsBySku(sku)) {
            throw new ConflictException("part.skuAlreadyExists", sku.value());
        }

        Part part = new Part(
                sku,
                command.name(),
                command.type(),
                command.unit(),
                new Money(command.salePrice()),
                command.minimumStock());
        return PartResult.from(partRepository.save(part));
    }
}
