package com.hrm.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.function.Function;

/** One page of results plus the total count, serialised as JSON for AJAX tables and the API. */
public record Page<T>(List<T> items, int page, int size, long totalItems) {

    @JsonProperty("totalPages")
    public int totalPages() {
        return size == 0 ? 0 : (int) ((totalItems + size - 1) / size);
    }

    public <R> Page<R> map(Function<T, R> mapper) {
        return new Page<>(items.stream().map(mapper).toList(), page, size, totalItems);
    }

    public static <T> Page<T> of(List<T> items, PageRequest request, long total) {
        return new Page<>(items, request.page(), request.size(), total);
    }
}
