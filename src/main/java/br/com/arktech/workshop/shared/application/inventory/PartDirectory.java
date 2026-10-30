package br.com.arktech.workshop.shared.application.inventory;

import java.util.Collection;
import java.util.Map;

public interface PartDirectory {

    PartDetails detailsOf(Long partId);

    Map<Long, String> namesOf(Collection<Long> partIds);
}
