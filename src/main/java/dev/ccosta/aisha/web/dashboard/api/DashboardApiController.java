package dev.ccosta.aisha.web.dashboard.api;

import dev.ccosta.aisha.application.dashboard.DashboardBalanceEvolution;
import dev.ccosta.aisha.application.dashboard.DashboardBalancePoint;
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
}
