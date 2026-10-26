package br.com.arktech.workshop.registry.infrastructure.persistence;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;

import org.springframework.data.jpa.repository.JpaRepository;

interface VehicleJpaRepository extends JpaRepository<Vehicle, Long>, VehicleRepository {
}
