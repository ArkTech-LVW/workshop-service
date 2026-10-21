package br.com.arktech.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationInfo;
import org.flywaydb.core.api.MigrationInfoService;
import org.flywaydb.core.api.MigrationState;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

@IntegrationTest
class DatabaseMigrationTest {

    @Autowired
    private Flyway flyway;

    @Test
    void shouldApplyAllMigrationsOnStartup() {
        MigrationInfoService info = flyway.info();

        assertThat(info.pending()).isEmpty();
        assertThat(info.applied())
                .isNotEmpty()
                .extracting(MigrationInfo::getState)
                .containsOnly(MigrationState.SUCCESS);
    }
}
