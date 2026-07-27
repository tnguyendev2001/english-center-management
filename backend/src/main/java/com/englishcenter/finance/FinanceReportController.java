package com.englishcenter.finance;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.finance.dto.AccountBalanceResponse;
import com.englishcenter.finance.dto.CategoryBreakdownItemResponse;
import com.englishcenter.finance.dto.CompareDefaultsResponse;
import com.englishcenter.finance.dto.ComparePeriodsResponse;
import com.englishcenter.finance.dto.FinanceConfigResponse;
import com.englishcenter.finance.dto.FinanceOverviewResponse;
import com.englishcenter.finance.dto.MonthlyReportResponse;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.dto.RepairPaymentLedgerRequest;
import com.englishcenter.finance.dto.RepairPaymentLedgerResponse;
import com.englishcenter.finance.dto.StudentDebtItemResponse;
import com.englishcenter.finance.dto.YearlyReportResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance")
@PreAuthorize("hasRole('ADMIN')")
public class FinanceReportController {
    private final FinanceCalculationService financeCalculationService;
    private final FinanceExportService financeExportService;
    private final FinancePostingService financePostingService;
    private final FinanceConfigService financeConfigService;

    public FinanceReportController(
            FinanceCalculationService financeCalculationService,
            FinanceExportService financeExportService,
            FinancePostingService financePostingService,
            FinanceConfigService financeConfigService
    ) {
        this.financeCalculationService = financeCalculationService;
        this.financeExportService = financeExportService;
        this.financePostingService = financePostingService;
        this.financeConfigService = financeConfigService;
    }

    @GetMapping("/config")
    public ApiResponse<FinanceConfigResponse> config() {
        return ApiResponse.success(financeConfigService.getConfig());
    }

    @GetMapping("/overview")
    public ApiResponse<FinanceOverviewResponse> overview(
            @RequestParam FinanceScope scope,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        return ApiResponse.success(financeCalculationService.getOverview(scope, year, month));
    }

    @GetMapping("/reports/monthly")
    public ApiResponse<MonthlyReportResponse> monthly(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.success(financeCalculationService.calculateMonthlyReport(year, month));
    }

    @GetMapping("/reports/yearly")
    public ApiResponse<YearlyReportResponse> yearly(
            @RequestParam int fromYear,
            @RequestParam(required = false) Integer toYear
    ) {
        int year = toYear != null ? toYear : fromYear;
        return ApiResponse.success(financeCalculationService.calculateYearlyReport(year));
    }

    @GetMapping("/reports/period-summary")
    public ApiResponse<PeriodSummaryResponse> periodSummary(@RequestParam int year, @RequestParam int month) {
        return ApiResponse.success(financeCalculationService.calculatePeriodSummary(year, month, true));
    }

    @GetMapping("/reports/category-breakdown")
    public ApiResponse<List<CategoryBreakdownItemResponse>> categoryBreakdown(
            @RequestParam LocalDate fromDate,
            @RequestParam LocalDate toDate,
            @RequestParam CategoryDirection direction
    ) {
        return ApiResponse.success(financeCalculationService.calculateCategoryBreakdown(fromDate, toDate, direction));
    }

    @GetMapping("/reports/account-balances")
    public ApiResponse<List<AccountBalanceResponse>> accountBalances(
            @RequestParam(required = false) LocalDate asOfDate
    ) {
        return ApiResponse.success(financeCalculationService.calculateAccountBalances(asOfDate));
    }

    @GetMapping("/reports/student-debt")
    public ApiResponse<List<StudentDebtItemResponse>> studentDebt() {
        return ApiResponse.success(financeCalculationService.calculateStudentDebtDetails());
    }

    @GetMapping("/reports/compare/defaults")
    public ApiResponse<CompareDefaultsResponse> compareDefaults() {
        return ApiResponse.success(financeCalculationService.getCompareDefaults());
    }

    @GetMapping("/reports/compare")
    public ApiResponse<ComparePeriodsResponse> compare(
            @RequestParam int currentYear,
            @RequestParam int currentMonth,
            @RequestParam int comparisonYear,
            @RequestParam int comparisonMonth
    ) {
        return ApiResponse.success(financeCalculationService.comparePeriods(
                currentYear, currentMonth, comparisonYear, comparisonMonth
        ));
    }

    @GetMapping("/reconciliation/payments")
    public ApiResponse<PaymentReconciliationResponse> reconcilePayments(
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate
    ) {
        return ApiResponse.success(financeCalculationService.reconcilePayments(fromDate, toDate));
    }

    @PostMapping("/reconciliation/payments/repair")
    public ApiResponse<RepairPaymentLedgerResponse> repairMissingPaymentLedger(
            @Valid @RequestBody RepairPaymentLedgerRequest request
    ) {
        return ApiResponse.success(financePostingService.repairMissingPaymentLedger(request));
    }

    @GetMapping("/reports/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) FinanceScope scope,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month
    ) {
        byte[] bytes = financeExportService.exportWorkbook(scope, year, month);
        String filename = "finance-report.xlsx";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType(
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                ))
                .body(bytes);
    }
}
