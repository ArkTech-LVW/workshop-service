package br.com.arktech.workshop;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CiFailureCheckTest {

    @Test
    void shouldFailOnPurpose() {
        assertThat(1 + 1).isEqualTo(3);
    }
}
