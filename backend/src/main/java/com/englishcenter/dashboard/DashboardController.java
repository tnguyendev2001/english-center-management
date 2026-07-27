package com.englishcenter.dashboard;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.dashboard.dto.DashboardOverviewResponse;
import com.englishcenter.dashboard.dto.DashboardSummaryResponse;
import com.englishcenter.dashboard.dto.DashboardTodaySessionResponse;
import com.englishcenter.dashboard.dto.SessionWarningResponse;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.report.dto.DebtReportItemResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping({"/overview", ""})
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER', 'STUDENT')")
    public ApiResponse<DashboardOverviewResponse> getOverview() {
        return ApiResponse.success(dashboardService.getOverview());
    }

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DashboardSummaryResponse> getSummary() {
        return ApiResponse.success(dashboardService.getSummary());
    }

    @GetMapping("/today-sessions")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DashboardTodaySessionResponse>> getTodaySessions() {
        return ApiResponse.success(dashboardService.getTodaySessions());
    }

    @GetMapping("/debt-alerts")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DebtReportItemResponse>> getDebtAlerts(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(dashboardService.getDebtAlerts(limit));
    }

    @GetMapping("/session-warnings")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<SessionWarningResponse>> getSessionWarnings(
            @RequestParam(defaultValue = "2") int remainingThreshold,
            @RequestParam(required = false) String remaining
    ) {
        return ApiResponse.success(dashboardService.getSessionWarnings(remainingThreshold, remaining));
    }

    @GetMapping("/recent-payments")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<PaymentResponse>> getRecentPayments(
            @RequestParam(defaultValue = "10") int limit
    ) {
        return ApiResponse.success(dashboardService.getRecentPayments(limit));
    }
}
