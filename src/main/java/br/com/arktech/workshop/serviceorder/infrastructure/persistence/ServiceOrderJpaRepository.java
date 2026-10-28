package br.com.arktech.workshop.serviceorder.infrastructure.persistence;

import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrder;
import br.com.arktech.workshop.serviceorder.domain.model.ServiceOrderNumber;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;

import java.time.Year;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

interface ServiceOrderJpaRepository extends JpaRepository<ServiceOrder, Long>, ServiceOrderRepository {

    @Override
    default ServiceOrderNumber nextNumber() {
        return ServiceOrderNumber.of(Year.now(), nextNumberSequence());
    }

    @Query(value = "SELECT nextval('service_order_number_seq')", nativeQuery = true)
    long nextNumberSequence();
}
