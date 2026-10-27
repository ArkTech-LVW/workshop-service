package br.com.arktech.workshop.inventory.application.deletepart;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.VoidUseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeletePartUseCase implements VoidUseCase<DeletePartCommand> {

    private final PartRepository partRepository;

    @Override
    @Transactional
    public void execute(DeletePartCommand command) {
        Part part = partRepository.findRequiredById(command.id());

        partRepository.remove(part);
    }
}
