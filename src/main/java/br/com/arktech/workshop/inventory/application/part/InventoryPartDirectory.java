package br.com.arktech.workshop.inventory.application.part;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;
import br.com.arktech.workshop.shared.application.inventory.PartDetails;
import br.com.arktech.workshop.shared.application.inventory.PartDirectory;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
class InventoryPartDirectory implements PartDirectory {

    private final PartRepository partRepository;

    @Override
    public PartDetails detailsOf(Long partId) {
        Part part = partRepository.findRequiredById(partId);
        return new PartDetails(part.getSalePrice(), part.getStockQuantity());
    }

    @Override
    public Map<Long, String> namesOf(Collection<Long> partIds) {
        return partRepository.findAllById(partIds).stream().collect(Collectors.toMap(Part::getId, Part::getName));
    }
}
