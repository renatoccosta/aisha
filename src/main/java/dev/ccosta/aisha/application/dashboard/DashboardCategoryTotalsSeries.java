package dev.ccosta.aisha.application.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardCategoryTotalsSeries(
    Long categoryId,
    String categoryName,
    boolean hasChildren,
    List<BigDecimal> values
) {
}
