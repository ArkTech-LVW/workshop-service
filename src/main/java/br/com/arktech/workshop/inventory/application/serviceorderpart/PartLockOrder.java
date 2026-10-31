package br.com.arktech.workshop.inventory.application.serviceorderpart;

import java.util.Arrays;
import java.util.Collection;
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
}
