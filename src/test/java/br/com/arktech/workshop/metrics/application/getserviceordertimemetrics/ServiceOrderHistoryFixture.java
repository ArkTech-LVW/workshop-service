package br.com.arktech.workshop.metrics.application.getserviceordertimemetrics;

import static br.com.arktech.workshop.UniqueTestData.uniqueCPF;
import static br.com.arktech.workshop.UniqueTestData.uniquePlate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import org.springframework.jdbc.core.JdbcTemplate;

public final class ServiceOrderHistoryFixture {

    private final JdbcTemplate jdbcTemplate;

    public ServiceOrderHistoryFixture(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long serviceOffering(String name, int estimatedMinutes) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO service_offering (name, labor_price, estimated_minutes, date_created, last_updated, version)
                VALUES (?, 100, ?, now(), now(), 0)
                RETURNING id
                """,
                Long.class,
                name,
                estimatedMinutes);
    }

    public History openedAt(Instant openedAt) {
        return new History(openedAt);
    }

    public final class History {

        private final List<Entry> entries = new ArrayList<>();
        private final List<ServiceItem> serviceItems = new ArrayList<>();
        private Instant current;

        private History(Instant openedAt) {
            current = openedAt;
            entries.add(new Entry("RECEIVED", openedAt));
        }

        public History withService(Long serviceOfferingId, int quantity) {
            serviceItems.add(new ServiceItem(serviceOfferingId, quantity));
            return this;
        }

        public History then(String status, Duration after) {
            return thenAt(status, current.plus(after));
        }

        public History thenAt(String status, Instant at) {
            current = at;
            entries.add(new Entry(status, at));
            return this;
        }

        public void save() {
            Long customerId = insertReturningId(
                    """
                    INSERT INTO customer (name, document, person_type, email, date_created, last_updated, version)
                    VALUES ('Ana', ?, 'NATURAL_PERSON', 'ana@email.com', now(), now(), 0)
                    RETURNING id
                    """,
                    uniqueCPF().value());
            Long vehicleId = insertReturningId(
                    """
                    INSERT INTO vehicle (customer_id, license_plate, brand, model, year, color,
                                         date_created, last_updated, version)
                    VALUES (?, ?, 'Fiat', 'Uno', 2010, 'Branco', now(), now(), 0)
                    RETURNING id
                    """,
                    customerId,
                    uniquePlate());
            Long serviceOrderId = insertReturningId(
                    """
                    INSERT INTO service_order (public_id, number, customer_id, vehicle_id, problem_description,
                                               status, total_price, date_created, last_updated, version)
                    VALUES (?, ?, ?, ?, 'Barulho no motor', ?, 0, ?, now(), 0)
                    RETURNING id
                    """,
                    UUID.randomUUID(),
                    "MT-%012d".formatted(ThreadLocalRandom.current().nextLong(1_000_000_000_000L)),
                    customerId,
                    vehicleId,
                    entries.getLast().status(),
                    entries.getFirst().at().atOffset(ZoneOffset.UTC));
            for (ServiceItem item : serviceItems) {
                jdbcTemplate.update(
                        """
                        INSERT INTO service_item (service_order_id, service_offering_id, quantity, unit_price,
                                                  date_created, last_updated, version)
                        VALUES (?, ?, ?, 100, now(), now(), 0)
                        """,
                        serviceOrderId,
                        item.serviceOfferingId(),
                        item.quantity());
            }
            String fromStatus = null;
            for (Entry entry : entries) {
                jdbcTemplate.update(
                        """
                        INSERT INTO status_change (service_order_id, from_status, to_status,
                                                   date_created, last_updated, version)
                        VALUES (?, ?, ?, ?, now(), 0)
                        """,
                        serviceOrderId,
                        fromStatus,
                        entry.status(),
                        entry.at().atOffset(ZoneOffset.UTC));
                fromStatus = entry.status();
            }
        }

        private Long insertReturningId(String sql, Object... args) {
            return jdbcTemplate.queryForObject(sql, Long.class, args);
        }
    }

    private record Entry(String status, Instant at) {}

    private record ServiceItem(Long serviceOfferingId, int quantity) {}
}
