package com.englishcenter.invoice.dto;

import java.math.BigDecimal;

public record TuitionOverviewResponse(
        BigDecimal totalOutstanding,
        long studentsWithDebt,
        long unpaidInvoiceCount,
        long partialInvoiceCount
) {
}
