package br.com.arktech.workshop.shared.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record Page<T>(List<T> content, Pagination pagination, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    public int totalPages() {
        return (int) Math.ceilDiv(totalElements, pagination.size());
    }

    public <R> Page<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new Page<>(mapped, pagination, totalElements);
    }
}
