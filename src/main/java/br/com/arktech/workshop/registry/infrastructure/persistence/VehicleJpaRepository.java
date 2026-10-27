package br.com.arktech.workshop.registry.infrastructure.persistence;

import br.com.arktech.workshop.registry.domain.model.Vehicle;
import br.com.arktech.workshop.registry.domain.repository.VehicleFilter;
import br.com.arktech.workshop.registry.domain.repository.VehicleRepository;
import br.com.arktech.workshop.shared.domain.exception.ConflictException;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

interface VehicleJpaRepository
        extends JpaRepository<Vehicle, Long>, JpaSpecificationExecutor<Vehicle>, VehicleRepository {

    Sort PAGE_ORDER = Sort.by("licensePlate", "id");

    @Override
    default Page<Vehicle> findPage(VehicleFilter filter, Pagination pagination) {
        var page = findAll(matching(filter), PageRequest.of(pagination.page(), pagination.size(), PAGE_ORDER));
        return new Page<>(page.getContent(), pagination, page.getTotalElements());
    }

    @Override
    default void remove(Vehicle vehicle) {
        try {
            delete(vehicle);
            flush();
        } catch (DataIntegrityViolationException ex) {
            throw new ConflictException("vehicle.inUse", vehicle.getId().toString());
        }
    }

    private static Specification<Vehicle> matching(VehicleFilter filter) {
        Specification<Vehicle> specification = Specification.unrestricted();

        LicensePlate licensePlate = filter.licensePlate();
        if (licensePlate != null) {
            specification = specification.and(
                    (root, query, builder) -> builder.equal(root.get("licensePlate"), licensePlate));
        }

        Long customerId = filter.customerId();
        if (customerId != null) {
            specification =
                    specification.and((root, query, builder) -> builder.equal(root.get("customerId"), customerId));
        }

        return specification;
    }
}
