package dev.ccosta.aisha.web.pagination;

import dev.ccosta.aisha.domain.shared.PagedResult;

public record PaginationView(
    int page,
    int pageSize,
    long totalItems,
    int totalPages,
    long rangeStart,
    long rangeEnd
) {

    public static PaginationView from(PagedResult<?> pageResult) {
        long totalItems = pageResult.totalItems();
        int totalPages = pageResult.totalPages();
        int page = pageResult.page();
        int pageSize = pageResult.pageSize();
        long rangeStart = totalItems == 0 ? 0 : ((long) page * pageSize) + 1;
        long rangeEnd = totalItems == 0 ? 0 : Math.min(((long) page + 1) * pageSize, totalItems);
        return new PaginationView(page, pageSize, totalItems, totalPages, rangeStart, rangeEnd);
    }

    public boolean hasPrevious() {
        return page > 0 && totalPages > 0;
    }

    public boolean hasNext() {
        return totalPages > 0 && page < totalPages - 1;
    }

    public int firstPage() {
        return 0;
    }

    public int previousPage() {
        return Math.max(0, page - 1);
    }

    public int nextPage() {
        return totalPages <= 0 ? 0 : Math.min(totalPages - 1, page + 1);
    }

    public int lastPage() {
        return totalPages <= 0 ? 0 : totalPages - 1;
    }

    public int currentPageDisplay() {
        return totalItems == 0 ? 0 : page + 1;
    }
}
