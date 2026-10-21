package br.com.arktech.workshop;

import org.springframework.boot.SpringApplication;

// Entry point for ./gradlew bootTestRun: runs the application against a disposable PostgreSQL container.
public class TestWorkshopApplication {

    public static void main(String[] args) {
        SpringApplication.from(WorkshopApplication::main)
                .with(TestcontainersConfiguration.class)
                .run(args);
    }
}
