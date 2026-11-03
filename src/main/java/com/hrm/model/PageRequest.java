package com.hrm.model;

public record PageRequest(int page, int size, String sort, boolean ascending) {

    public static final int MAX_SIZE = 100;

    public PageRequest {
        page = Math.max(page, 1);
        sort = sort == null || sort.isBlank() ? null : sort.trim();
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public static PageRequest of(Integer page, Integer size, String sort, String direction, int defaultSize) {
        return new PageRequest(page == null ? 1 : page, size == null ? defaultSize : size, sort,
                !"desc".equalsIgnoreCase(direction));
    }

    public int offset() {
        return (page - 1) * size;
    }
}
