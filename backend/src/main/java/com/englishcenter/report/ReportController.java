package com.englishcenter.report;

import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.report.dto.AttendanceReportResponse;
import com.englishcenter.report.dto.ClassroomReportItemResponse;
import com.englishcenter.report.dto.DebtReportItemResponse;
import com.englishcenter.report.dto.DebtReportSummary;
import com.englishcenter.report.dto.PaymentReportResponse;
import com.englishcenter.report.dto.RevenueReportResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
public class ReportController {
    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/debts/summary")
    public ApiResponse<DebtReportSummary> getDebtReportSummary() {
        return ApiResponse.success(reportService.getDebtReportSummary());
    }

    @GetMapping("/debts")
    public ApiResponse<List<DebtReportItemResponse>> getDebtReport(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Page<DebtReportItemResponse> debts = reportService.getDebtReport(
                classroomId,
                keyword,
                status,
                fromDate,
                toDate,
                page,
                size
        );

        PageMeta meta = new PageMeta(
                debts.getNumber(),
                debts.getSize(),
                debts.getTotalElements(),
                debts.getTotalPages()
        );

        return ApiResponse.success(debts.getContent(), meta);
    }

    @GetMapping("/payments")
    public ApiResponse<PaymentReportResponse> getPaymentReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(required = false) PaymentStatus paymentStatus,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(reportService.getPaymentReport(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                paymentStatus,
                keyword,
                page,
                size
        ));
    }

    @GetMapping("/revenue")
    public ApiResponse<RevenueReportResponse> getRevenueReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) PaymentMethod paymentMethod,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(reportService.getRevenueReport(
                fromDate,
                toDate,
                classroomId,
                paymentMethod,
                page,
                size
        ));
    }

    @GetMapping("/invoices")
    public ApiResponse<List<InvoiceResponse>> getInvoiceReport(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        Page<InvoiceResponse> invoices = reportService.getInvoiceReport(
                classroomId,
                keyword,
                status,
                fromDate,
                toDate,
                page,
                size
        );

        PageMeta meta = new PageMeta(
                invoices.getNumber(),
                invoices.getSize(),
                invoices.getTotalElements(),
                invoices.getTotalPages()
        );

        return ApiResponse.success(invoices.getContent(), meta);
    }

    @GetMapping("/attendance")
    public ApiResponse<AttendanceReportResponse> getAttendanceReport(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sessionDate,
            @RequestParam(required = false) AttendanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        return ApiResponse.success(reportService.getAttendanceReport(
                classroomId,
                keyword,
                sessionDate,
                status,
                page,
                size
        ));
    }

    @GetMapping("/enrollment-progress")
    public ApiResponse<List<EnrollmentLearningProgressResponse>> getEnrollmentProgressReport() {
        return ApiResponse.success(reportService.getEnrollmentProgressReport());
    }

    @GetMapping("/classrooms")
    public ApiResponse<List<ClassroomReportItemResponse>> getClassroomReport() {
        return ApiResponse.success(reportService.getClassroomReport());
    }
}
