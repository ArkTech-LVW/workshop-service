package br.com.arktech.workshop.shared.api.pagination;

import br.com.arktech.workshop.shared.domain.pagination.Page;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

public record PageResponse<T>(
        @Schema(description = "${openapi.page.content}") List<T> content,
        @Schema(description = "${openapi.page.page}", example = "0") int page,
        @Schema(description = "${openapi.page.size}", example = "20") int size,
        @Schema(description = "${openapi.page.totalElements}", example = "42") long totalElements,
        @Schema(description = "${openapi.page.totalPages}", example = "3") int totalPages) {

    public static <T, R> PageResponse<R> from(Page<T> page, Function<? super T, ? extends R> mapper) {
        Page<R> mapped = page.map(mapper);
        return new PageResponse<>(
                mapped.content(),
                mapped.pagination().page(),
                mapped.pagination().size(),
                mapped.totalElements(),
                mapped.totalPages());
    }
}
