package com.englishcenter.invoice.dto;

import java.math.BigDecimal;

public record StudentTuitionSummaryResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        BigDecimal totalTuitionAmount,
        BigDecimal totalPaidAmount,
        BigDecimal remainingDebt,
        int totalInvoiceCount,
        int unpaidCount,
        int partialCount,
        int paidCount,
        boolean hasReplacedInvoices
) {
}
