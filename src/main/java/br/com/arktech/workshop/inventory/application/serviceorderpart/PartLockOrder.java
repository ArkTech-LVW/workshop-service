package br.com.arktech.workshop.inventory.application.serviceorderpart;

import br.com.arktech.workshop.inventory.domain.model.Part;
import br.com.arktech.workshop.inventory.domain.repository.PartRepository;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public final class PartLockOrder {

    private PartLockOrder() {}

    @SafeVarargs
    public static SortedSet<Long> of(Collection<Long>... partIds) {
        SortedSet<Long> lockOrder = new TreeSet<>();
        Arrays.stream(partIds).forEach(lockOrder::addAll);
        return lockOrder;
    }

    public static List<Part> lock(Collection<Long> partIds, PartRepository partRepository) {
        return of(partIds).stream().map(partRepository::findRequiredForUpdateById).toList();
    }
}
