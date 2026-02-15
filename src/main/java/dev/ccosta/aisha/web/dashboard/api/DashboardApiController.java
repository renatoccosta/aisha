package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardBalanceEvolution;
import dev.ccosta.aisha.application.dashboard.DashboardBalancePoint;
import dev.ccosta.aisha.application.dashboard.DashboardCategoryTotalsEvolution;
import dev.ccosta.aisha.application.dashboard.DashboardCategoryTotalsSeries;
import dev.ccosta.aisha.application.dashboard.DashboardExpenseCategoryBreakdown;
import dev.ccosta.aisha.application.dashboard.DashboardExpenseCategoryItem;
import dev.ccosta.aisha.application.dashboard.DashboardMetric;
import dev.ccosta.aisha.application.dashboard.DashboardRevenueExpenseEvolution;
import dev.ccosta.aisha.application.dashboard.DashboardRevenueExpensePoint;
import dev.ccosta.aisha.application.dashboard.DashboardService;
import dev.ccosta.aisha.application.dashboard.DashboardSummary;
import dev.ccosta.aisha.web.timefilter.DateFilterSessionService;
import dev.ccosta.aisha.web.timefilter.DateFilterState;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardApiController {

    private final DashboardService dashboardService;
    private final DateFilterSessionService dateFilterSessionService;

    public DashboardApiController(DashboardService dashboardService, DateFilterSessionService dateFilterSessionService) {
        this.dashboardService = dashboardService;
        this.dateFilterSessionService = dateFilterSessionService;
    }

    @GetMapping("/summary")
    public DashboardSummaryResponse summary(HttpSession session) {
        DateFilterState filter = dateFilterSessionService.getOrCreate(session);
        DashboardSummary summary = dashboardService.buildSummary(filter.getStartDate(), filter.getEndDate());

        return new DashboardSummaryResponse(
            toMetric(summary.currentBalance()),
            toMetric(summary.totalExpenses()),
            toMetric(summary.totalRevenues())
        );
    }

    @GetMapping("/balance-evolution")
    public DashboardBalanceEvolutionResponse balanceEvolution(HttpSession session) {
        DateFilterState filter = dateFilterSessionService.getOrCreate(session);
        DashboardBalanceEvolution evolution = dashboardService.buildBalanceEvolution(filter.getStartDate(), filter.getEndDate());

        List<DashboardBalanceEvolutionResponse.DashboardBalancePointResponse> points = evolution.points()
            .stream()
            .map(this::toPoint)
            .toList();

        return new DashboardBalanceEvolutionResponse(
            evolution.startDate(),
            evolution.endDate(),
            evolution.granularity(),
            evolution.openingBalance(),
            points
        );
    }

    @GetMapping("/revenues-vs-expenses")
    public DashboardRevenueExpenseEvolutionResponse revenuesVsExpenses(HttpSession session) {
        DateFilterState filter = dateFilterSessionService.getOrCreate(session);
        DashboardRevenueExpenseEvolution evolution = dashboardService.buildRevenueExpenseEvolution(
            filter.getStartDate(),
            filter.getEndDate()
        );

        List<DashboardRevenueExpenseEvolutionResponse.DashboardRevenueExpensePointResponse> points = evolution.points()
            .stream()
            .map(this::toRevenueExpensePoint)
            .toList();

        return new DashboardRevenueExpenseEvolutionResponse(
            evolution.startDate(),
            evolution.endDate(),
            evolution.granularity(),
            points
        );
    }

    @GetMapping("/expenses-by-category")
    public DashboardExpenseCategoryBreakdownResponse expensesByCategory(
        HttpSession session,
        @RequestParam(required = false) Long parentCategoryId
    ) {
        DateFilterState filter = dateFilterSessionService.getOrCreate(session);
        DashboardExpenseCategoryBreakdown breakdown = dashboardService.buildExpenseCategoryBreakdown(
            filter.getStartDate(),
            filter.getEndDate(),
            parentCategoryId
        );

        List<DashboardExpenseCategoryBreakdownResponse.DashboardExpenseCategoryItemResponse> items = breakdown.items()
            .stream()
            .map(this::toExpenseCategoryItem)
            .toList();

        return new DashboardExpenseCategoryBreakdownResponse(
            breakdown.startDate(),
            breakdown.endDate(),
            breakdown.currentParentCategoryId(),
            breakdown.currentParentCategoryName(),
            breakdown.drillUpParentCategoryId(),
            items
        );
    }

    @GetMapping("/category-totals")
    public DashboardCategoryTotalsEvolutionResponse categoryTotals(
        HttpSession session,
        @RequestParam(required = false) Long parentCategoryId
    ) {
        DateFilterState filter = dateFilterSessionService.getOrCreate(session);
        DashboardCategoryTotalsEvolution evolution = dashboardService.buildCategoryTotalsEvolution(
            filter.getStartDate(),
            filter.getEndDate(),
            parentCategoryId
        );

        List<DashboardCategoryTotalsEvolutionResponse.DashboardCategoryTotalsSeriesResponse> series = evolution.series()
            .stream()
            .map(this::toCategoryTotalsSeries)
            .toList();

        return new DashboardCategoryTotalsEvolutionResponse(
            evolution.startDate(),
            evolution.endDate(),
            evolution.granularity(),
            evolution.currentParentCategoryId(),
            evolution.currentParentCategoryName(),
            evolution.drillUpParentCategoryId(),
            evolution.buckets(),
            series
        );
    }

    private DashboardSummaryResponse.DashboardMetricResponse toMetric(DashboardMetric metric) {
        return new DashboardSummaryResponse.DashboardMetricResponse(
            metric.currentValue(),
            metric.previousValue(),
            metric.variationPercent()
        );
    }

    private DashboardBalanceEvolutionResponse.DashboardBalancePointResponse toPoint(DashboardBalancePoint point) {
        return new DashboardBalanceEvolutionResponse.DashboardBalancePointResponse(
            point.date(),
            point.periodAmount(),
            point.accumulatedBalance()
        );
    }

    private DashboardRevenueExpenseEvolutionResponse.DashboardRevenueExpensePointResponse toRevenueExpensePoint(
        DashboardRevenueExpensePoint point
    ) {
        return new DashboardRevenueExpenseEvolutionResponse.DashboardRevenueExpensePointResponse(
            point.date(),
            point.revenues(),
            point.expenses()
        );
    }

    private DashboardExpenseCategoryBreakdownResponse.DashboardExpenseCategoryItemResponse toExpenseCategoryItem(
        DashboardExpenseCategoryItem item
    ) {
        return new DashboardExpenseCategoryBreakdownResponse.DashboardExpenseCategoryItemResponse(
            item.categoryId(),
            item.categoryName(),
            item.amount(),
            item.hasChildren()
        );
    }

    private DashboardCategoryTotalsEvolutionResponse.DashboardCategoryTotalsSeriesResponse toCategoryTotalsSeries(
        DashboardCategoryTotalsSeries series
    ) {
        return new DashboardCategoryTotalsEvolutionResponse.DashboardCategoryTotalsSeriesResponse(
            series.categoryId(),
            series.categoryName(),
            series.hasChildren(),
            series.values()
        );
    }
}
