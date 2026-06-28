package com.englishcenter.report.dto;

import java.math.BigDecimal;

public record DebtReportSummary(
        BigDecimal totalDebtAmount,
        long studentsWithDebtCount,
        long unpaidInvoiceCount,
        long partiallyPaidInvoiceCount
) {
}
