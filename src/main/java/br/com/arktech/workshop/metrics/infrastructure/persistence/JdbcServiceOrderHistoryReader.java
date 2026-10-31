package br.com.arktech.workshop.metrics.infrastructure.persistence;

import br.com.arktech.workshop.metrics.application.port.ServiceExecutionTime;
import br.com.arktech.workshop.metrics.application.port.ServiceOrderHistoryReader;
import br.com.arktech.workshop.metrics.application.port.StatusTime;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

import lombok.RequiredArgsConstructor;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JdbcServiceOrderHistoryReader implements ServiceOrderHistoryReader {

    private static final String COUNT_COMPLETED = """
            SELECT count(*)
            FROM status_change
            WHERE to_status = 'COMPLETED'
              AND date_created >= :start
              AND date_created < :end
            """;

    private static final String AVERAGE_TIME_BY_STATUS = """
            WITH completed AS (
                SELECT service_order_id
                FROM status_change
                WHERE to_status = 'COMPLETED'
                  AND date_created >= :start
                  AND date_created < :end
            ),
            period AS (
                SELECT history.service_order_id,
                       history.to_status AS status,
                       LEAD(history.date_created) OVER (
                           PARTITION BY history.service_order_id
                           ORDER BY history.date_created, history.id) - history.date_created AS duration
                FROM status_change history
                JOIN completed ON completed.service_order_id = history.service_order_id
            ),
            time_in_status AS (
                SELECT service_order_id, status, sum(duration) AS duration
                FROM period
                WHERE duration IS NOT NULL
                GROUP BY service_order_id, status
            )
            SELECT status,
                   round(EXTRACT(EPOCH FROM avg(duration)))::BIGINT AS average_seconds,
                   count(*) AS service_order_count
            FROM time_in_status
            GROUP BY status
            ORDER BY array_position(ARRAY['RECEIVED', 'IN_DIAGNOSIS', 'AWAITING_APPROVAL', 'QUOTE_APPROVED',
                                          'QUOTE_REJECTED', 'IN_PROGRESS', 'AWAITING_SUPPLEMENTARY_APPROVAL',
                                          'SUPPLEMENTARY_APPROVED', 'SUPPLEMENTARY_REJECTED', 'COMPLETED',
                                          'DELIVERED', 'CANCELED']::VARCHAR[], status)
            """;

    private static final String AVERAGE_EXECUTION_TIME_BY_SERVICE = """
            WITH completed AS (
                SELECT service_order_id
                FROM status_change
                WHERE to_status = 'COMPLETED'
                  AND date_created >= :start
                  AND date_created < :end
            ),
            period AS (
                SELECT history.service_order_id,
                       history.to_status AS status,
                       LEAD(history.date_created) OVER (
                           PARTITION BY history.service_order_id
                           ORDER BY history.date_created, history.id) - history.date_created AS duration
                FROM status_change history
                JOIN completed ON completed.service_order_id = history.service_order_id
            ),
            execution AS (
                SELECT service_order_id, sum(duration) AS duration
                FROM period
                WHERE status = 'IN_PROGRESS'
                  AND duration IS NOT NULL
                GROUP BY service_order_id
            ),
            service AS (
                SELECT DISTINCT item.service_order_id, item.service_offering_id
                FROM service_item item
                JOIN completed ON completed.service_order_id = item.service_order_id
            )
            SELECT offering.id AS service_offering_id,
                   offering.name,
                   offering.estimated_minutes,
                   round(EXTRACT(EPOCH FROM avg(execution.duration)))::BIGINT AS average_seconds,
                   count(*) AS service_order_count
            FROM service
            JOIN execution ON execution.service_order_id = service.service_order_id
            JOIN service_offering offering ON offering.id = service.service_offering_id
            GROUP BY offering.id, offering.name, offering.estimated_minutes
            ORDER BY offering.name, offering.id
            """;

    private final JdbcClient jdbcClient;

    @Override
    public long countCompletedBetween(Instant start, Instant end) {
        return jdbcClient.sql(COUNT_COMPLETED)
                .param("start", atUtc(start))
                .param("end", atUtc(end))
                .query(Long.class)
                .single();
    }

    @Override
    public List<StatusTime> averageTimeByStatusOfCompletedBetween(Instant start, Instant end) {
        return jdbcClient.sql(AVERAGE_TIME_BY_STATUS)
                .param("start", atUtc(start))
                .param("end", atUtc(end))
                .query((resultSet, rowNumber) -> new StatusTime(
                        resultSet.getString("status"),
                        Duration.ofSeconds(resultSet.getLong("average_seconds")),
                        resultSet.getLong("service_order_count")))
                .list();
    }

    @Override
    public List<ServiceExecutionTime> averageExecutionTimeByServiceOfCompletedBetween(Instant start, Instant end) {
        return jdbcClient.sql(AVERAGE_EXECUTION_TIME_BY_SERVICE)
                .param("start", atUtc(start))
                .param("end", atUtc(end))
                .query((resultSet, rowNumber) -> new ServiceExecutionTime(
                        resultSet.getLong("service_offering_id"),
                        resultSet.getString("name"),
                        resultSet.getInt("estimated_minutes"),
                        Duration.ofSeconds(resultSet.getLong("average_seconds")),
                        resultSet.getLong("service_order_count")))
                .list();
    }

    private static OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
