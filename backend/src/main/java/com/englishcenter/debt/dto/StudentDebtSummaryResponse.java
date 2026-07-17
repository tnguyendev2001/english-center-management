package com.englishcenter.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentDebtSummaryResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        BigDecimal totalRemainingDebt,
        int debtInvoiceCount,
        int unpaidCount,
        int partialCount,
        LocalDate nearestDueDate
) {
}
