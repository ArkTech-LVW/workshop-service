package br.com.arktech.workshop.shared.domain.pagination;

public record Pagination(int page, int size) {

    public Pagination {
        if (page < 0 || size < 1) {
            throw new IllegalArgumentException("Invalid pagination: page=" + page + ", size=" + size);
        }
    }
}
