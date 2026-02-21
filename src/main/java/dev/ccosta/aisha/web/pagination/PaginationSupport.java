package dev.ccosta.aisha.web.pagination;

import dev.ccosta.aisha.domain.shared.PagedResult;
import java.util.List;

public final class PaginationSupport {

    public static final int DEFAULT_PAGE_SIZE = 25;
    public static final List<Integer> ALLOWED_PAGE_SIZES = List.of(25, 50, 100);

    private PaginationSupport() {}

    public static int sanitizePage(Integer page) {
        if (page == null || page < 0) {
            return 0;
        }
        return page;
    }

    public static int sanitizePageSize(Integer size) {
        if (size == null || !ALLOWED_PAGE_SIZES.contains(size)) {
            return DEFAULT_PAGE_SIZE;
        }
        return size;
    }

    public static int clampPageIndex(int requestedPage, int totalPages) {
        if (totalPages <= 0) {
            return 0;
        }
        if (requestedPage >= totalPages) {
            return totalPages - 1;
        }
        return Math.max(requestedPage, 0);
    }

    public static PaginationView toView(PagedResult<?> pageResult) {
        return PaginationView.from(pageResult);
    }
}
