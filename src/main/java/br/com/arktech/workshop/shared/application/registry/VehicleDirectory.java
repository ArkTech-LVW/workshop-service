package br.com.arktech.workshop.shared.application.registry;

public interface VehicleDirectory {

    Long findOrRegister(Long customerId, VehicleDetails vehicle);

    VehicleDetails detailsOf(Long vehicleId);
}
