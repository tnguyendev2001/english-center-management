package com.englishcenter.invoice;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.invoice.dto.AmbiguousInvoiceDiagnosticResponse;
import com.englishcenter.invoice.dto.InvoiceDocumentResponse;
import com.englishcenter.invoice.dto.InvoiceListSummaryResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;
    private final InvoiceDocumentService invoiceDocumentService;
    private final InvoiceBillingMigrationDiagnosticService migrationDiagnosticService;

    public InvoiceController(
            InvoiceService invoiceService,
            InvoiceDocumentService invoiceDocumentService,
            InvoiceBillingMigrationDiagnosticService migrationDiagnosticService
    ) {
        this.invoiceService = invoiceService;
        this.invoiceDocumentService = invoiceDocumentService;
        this.migrationDiagnosticService = migrationDiagnosticService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Long packageId,
            @RequestParam(required = false) Integer cycleNo,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean hasRemainingDebt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate effectiveTo,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InvoiceResponse> invoices = invoiceService.getInvoices(
                status,
                studentId,
                classroomId,
                packageId,
                cycleNo,
                overdue,
                hasRemainingDebt,
                effectiveFrom,
                effectiveTo,
                dueFrom,
                dueTo,
                keyword,
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

    @GetMapping("/summary")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<InvoiceListSummaryResponse> getSummary(
            @RequestParam(required = false) Long classroomId
    ) {
        return ApiResponse.success(invoiceService.getListSummary(classroomId));
    }

    @GetMapping("/student-summaries")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StudentTuitionSummaryResponse>> getStudentSummaries(
            @RequestParam(required = false) Long classroomId
    ) {
        return ApiResponse.success(invoiceService.getStudentSummaries(classroomId));
    }

    @GetMapping("/migration-diagnostics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<AmbiguousInvoiceDiagnosticResponse>> migrationDiagnostics() {
        return ApiResponse.success(migrationDiagnosticService.findAmbiguousInvoices());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessInvoice(authentication, #id)")
    public ApiResponse<InvoiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getById(id));
    }

    @GetMapping("/{id}/document")
    @PreAuthorize("hasRole('ADMIN') or @authorizationService.canAccessInvoice(authentication, #id)")
    public ApiResponse<InvoiceDocumentResponse> getDocument(@PathVariable Long id) {
        return ApiResponse.success(invoiceDocumentService.getDocument(id));
    }
}
