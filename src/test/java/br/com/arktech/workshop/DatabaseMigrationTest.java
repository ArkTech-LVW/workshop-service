package br.com.arktech.workshop;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

@IntegrationTest
class DatabaseMigrationTest {

    private static final String OUT_OF_ORDER_SCHEMA = "out_of_order";

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldApplyAllMigrationsOnStartup() {
        MigrationInfoService info = flyway.info();

        assertThat(info.pending()).isEmpty();
        assertThat(info.applied())
                .isNotEmpty()
                .extracting(MigrationInfo::getState)
                .containsOnly(MigrationState.SUCCESS);
    }

    @Test
    void shouldApplyAMigrationWithLowerVersionThanTheLatestApplied() {
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS " + OUT_OF_ORDER_SCHEMA + " CASCADE");
        outOfOrderFlyway("classpath:db/out-of-order/first").migrate();

        Flyway withLateMigration = outOfOrderFlyway("classpath:db/out-of-order/first", "classpath:db/out-of-order/late");

        withLateMigration.migrate();

        assertThat(withLateMigration.info().applied())
                .filteredOn(migration -> migration.getVersion() != null)
                .extracting(migration -> migration.getVersion().getVersion(), MigrationInfo::getState)
                .containsExactlyInAnyOrder(
                        tuple("20990101000000", MigrationState.SUCCESS),
                        tuple("20000101000000", MigrationState.OUT_OF_ORDER));
    }

    private Flyway outOfOrderFlyway(String... locations) {
        return Flyway.configure()
                .configuration(flyway.getConfiguration())
                .schemas(OUT_OF_ORDER_SCHEMA)
                .defaultSchema(OUT_OF_ORDER_SCHEMA)
                .locations(locations)
                .load();
    }
}
