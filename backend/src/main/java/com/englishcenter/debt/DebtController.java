package com.englishcenter.debt;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.debt.dto.StudentDebtSummaryResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@PreAuthorize("hasRole('ADMIN')")
public class DebtController {
    private final DebtService debtService;

    public DebtController(DebtService debtService) {
        this.debtService = debtService;
    }

    @GetMapping("/api/debts/student-summaries")
    public ApiResponse<List<StudentDebtSummaryResponse>> getStudentSummaries(
            @RequestParam(required = false) Long classroomId
    ) {
        return ApiResponse.success(debtService.getStudentSummaries(classroomId));
    }

    @GetMapping("/api/debts")
    public ApiResponse<List<InvoiceResponse>> getDebts(
            @RequestParam(required = false) Long classroomId,
            @RequestParam(required = false) Long packageId,
            @RequestParam(required = false) Integer cycleNo,
            @RequestParam(required = false) Boolean overdue,
            @RequestParam(required = false) Boolean multipleInvoices,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dueTo,
            @RequestParam(required = false) BigDecimal remainingFrom,
            @RequestParam(required = false) BigDecimal remainingTo,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<InvoiceResponse> debts = debtService.getDebts(
                classroomId,
                packageId,
                cycleNo,
                overdue,
                multipleInvoices,
                dueFrom,
                dueTo,
                remainingFrom,
                remainingTo,
                keyword,
                status,
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
}
