package br.com.arktech.workshop.serviceorder.application.openserviceorder;

import br.com.arktech.workshop.shared.application.registry.VehicleDetails;

public record OpenServiceOrderCommand(String customerDocument, VehicleDetails vehicle, String problemDescription) {
}
