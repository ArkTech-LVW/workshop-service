package br.com.arktech.workshop.metrics.infrastructure.persistence;

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

    private static OffsetDateTime atUtc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }
}
