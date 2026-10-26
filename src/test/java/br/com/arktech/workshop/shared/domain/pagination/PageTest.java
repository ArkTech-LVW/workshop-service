package br.com.arktech.workshop.shared.domain.pagination;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class PageTest {

    @ParameterizedTest
    @CsvSource({"0, 0", "1, 1", "20, 1", "21, 2", "40, 2"})
    void shouldCountPages(long totalElements, int totalPages) {
        Page<String> page = new Page<>(List.of(), new Pagination(0, 20), totalElements);

        assertThat(page.totalPages()).isEqualTo(totalPages);
    }

    @Test
    void shouldMapContentKeepingPagination() {
        Page<String> page = new Page<>(List.of("a", "bb"), new Pagination(1, 2), 4);

        Page<Integer> mapped = page.map(String::length);

        assertThat(mapped).isEqualTo(new Page<>(List.of(1, 2), new Pagination(1, 2), 4));
    }

    @ParameterizedTest
    @CsvSource({"-1, 20", "0, 0"})
    void shouldRejectInvalidPagination(int page, int size) {
        assertThatThrownBy(() -> new Pagination(page, size)).isInstanceOf(IllegalArgumentException.class);
    }
}
