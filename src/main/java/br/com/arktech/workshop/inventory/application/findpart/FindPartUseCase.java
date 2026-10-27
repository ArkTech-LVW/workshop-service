package br.com.arktech.workshop.inventory.application.findpart;

import br.com.arktech.workshop.inventory.application.part.PartResult;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindPartUseCase implements UseCase<FindPartQuery, PartResult> {

    private final PartRepository partRepository;

    @Override
    @Transactional(readOnly = true)
    public PartResult execute(FindPartQuery query) {
        return PartResult.from(partRepository.findRequiredById(query.id()));
    }
}
