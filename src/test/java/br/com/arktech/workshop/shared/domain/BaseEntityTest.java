package br.com.arktech.workshop.shared.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class BaseEntityTest {

    @Test
    void shouldBeEqualWhenSameTypeAndId() {
        assertThat(customer(1L)).isEqualTo(customer(1L)).hasSameHashCodeAs(customer(1L));
    }

    @Test
    void shouldDifferWhenIdsDiffer() {
        assertThat(customer(1L)).isNotEqualTo(customer(2L));
    }

    @Test
    void shouldDifferWhenTypesDiffer() {
        assertThat(customer(1L)).isNotEqualTo(vehicle(1L));
    }

    @Test
    void shouldTreatUnsavedEntitiesAsDifferentUnlessSameInstance() {
        Customer unsaved = new Customer();

        assertThat(unsaved).isEqualTo(unsaved).isNotEqualTo(new Customer());
    }

    @Test
    void shouldMatchSubclassLikeHibernateProxy() {
        Customer proxy = withId(new CustomerProxy(), 1L);

        assertThat(proxy).isEqualTo(customer(1L));
        assertThat(customer(1L)).isEqualTo(proxy);
    }

    @Test
    void shouldKeepHashCodeWhenIdIsAssigned() {
        Customer customer = new Customer();
        int before = customer.hashCode();

        withId(customer, 1L);

        assertThat(customer.hashCode()).isEqualTo(before);
    }

    private static Customer customer(Long id) {
        return withId(new Customer(), id);
    }

    private static Vehicle vehicle(Long id) {
        return withId(new Vehicle(), id);
    }

    private static <T extends BaseEntity> T withId(T entity, Long id) {
        ReflectionTestUtils.setField(entity, BaseEntity.class, "id", id, Long.class);
        return entity;
    }

    static class Customer extends BaseEntity {
    }

    static class CustomerProxy extends Customer {
    }

    static class Vehicle extends BaseEntity {
    }
}
