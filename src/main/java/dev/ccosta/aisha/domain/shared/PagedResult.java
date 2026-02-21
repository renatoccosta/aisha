package dev.ccosta.aisha.domain.shared;

import java.util.List;

public record PagedResult<T>(
    List<T> items,
    int page,
    int pageSize,
    long totalItems,
    int totalPages
) {}
