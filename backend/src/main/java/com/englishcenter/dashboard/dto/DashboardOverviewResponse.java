package com.englishcenter.dashboard.dto;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.report.dto.DebtReportItemResponse;
import java.util.List;

public record DashboardOverviewResponse(
        AccountRole role,
        DashboardOverviewSummaryResponse summary,
        List<DashboardAlertResponse> alerts,
        List<DashboardTodaySessionResponse> todaySessions,
        List<DashboardPendingAttendanceResponse> pendingAttendance,
        List<SessionWarningResponse> studentsNeedingRenewal,
        List<DebtReportItemResponse> overdueInvoices,
        List<InvoiceResponse> studentInvoices
) {
}
