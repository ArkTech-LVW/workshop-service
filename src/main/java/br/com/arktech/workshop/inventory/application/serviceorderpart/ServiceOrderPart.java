package br.com.arktech.workshop.inventory.application.serviceorderpart;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ServiceOrderPart(Long partId, int quantity) {

    public static Map<Long, Integer> quantityByPart(List<ServiceOrderPart> parts) {
        return parts.stream()
                .collect(Collectors.toMap(ServiceOrderPart::partId, ServiceOrderPart::quantity, Math::addExact));
    }
}
