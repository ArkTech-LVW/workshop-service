package br.com.arktech.workshop.serviceorder.infrastructure.persistence;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;

import java.time.Year;
import java.time.ZoneId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrder, Long>, ServiceOrderRepository {

    ZoneId WORKSHOP_ZONE = ZoneId.of("America/Sao_Paulo");

    @Override
    default ServiceOrderNumber nextNumber() {
        Year year = Year.now(WORKSHOP_ZONE);
        return ServiceOrderNumber.of(year, incrementNumberCounter(year.getValue()));
    }

    @Transactional
    @Query(
            value = """
                    INSERT INTO service_order_number_counter (year, last_value) VALUES (:year, 1)
                    ON CONFLICT (year) DO UPDATE
                    SET last_value = service_order_number_counter.last_value + 1,
                        last_updated = now(),
                        version = service_order_number_counter.version + 1
                    RETURNING last_value
                    """,
            nativeQuery = true)
    long incrementNumberCounter(@Param("year") int year);
}
