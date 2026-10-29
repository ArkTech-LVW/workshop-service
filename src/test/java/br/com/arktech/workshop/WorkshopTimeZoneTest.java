package br.com.arktech.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.ZoneId;

import org.junit.jupiter.api.Test;

class WorkshopTimeZoneTest {

    @Test
    void shouldRunTestsInTheWorkshopTimeZone() {
        assertThat(ZoneId.systemDefault()).isEqualTo(ZoneId.of("America/Sao_Paulo"));
    }
}
