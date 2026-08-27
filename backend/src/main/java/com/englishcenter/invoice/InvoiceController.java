package com.englishcenter.invoice;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.dto.StudentTuitionSummaryResponse;
import com.englishcenter.invoice.dto.TuitionOverviewResponse;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {
    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ApiResponse<List<InvoiceResponse>> getInvoices(
            @RequestParam(required = false) InvoiceStatus status,
            @RequestParam(required = false) Long studentId,
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String packageName,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InvoiceResponse> invoices = invoiceService.getInvoices(
                status,
                studentId,
                classroomId,
                keyword,
                packageName,
                dueFrom,
                dueTo,
                page,
                size
        );
        return ApiResponse.success(invoices.getContent(), toMeta(invoices));
    }

    @GetMapping("/overview")
    public ApiResponse<TuitionOverviewResponse> getOverview() {
        return ApiResponse.success(invoiceService.getOverview());
    }

    @GetMapping("/student-summaries")
    public ApiResponse<List<StudentTuitionSummaryResponse>> getStudentSummaries(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<StudentTuitionSummaryResponse> summaries = invoiceService.getStudentSummaries(keyword, page, size);
        return ApiResponse.success(summaries.getContent(), toMeta(summaries));
    }

    @GetMapping("/{id}")
    public ApiResponse<InvoiceResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(invoiceService.getById(id));
    }

    private PageMeta toMeta(Page<?> page) {
        return new PageMeta(
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
