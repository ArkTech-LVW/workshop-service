package br.com.arktech.workshop.shared.infrastructure.seed;

import javax.sql.DataSource;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.output.MigrateResult;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile("demo")
@RequiredArgsConstructor
class DemoSeedInitializer implements ApplicationRunner {

    static final String LOCATION = "classpath:db/seed";
    static final String HISTORY_TABLE = "flyway_seed_history";

    private final DataSource dataSource;

    @Override
    public void run(ApplicationArguments args) {
        MigrateResult result = Flyway.configure()
                .dataSource(dataSource)
                .locations(LOCATION)
                .table(HISTORY_TABLE)
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .load()
                .migrate();
        log.info("Demo seed applied {} migration(s)", result.migrationsExecuted);
    }
}
