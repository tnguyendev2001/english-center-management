package com.englishcenter.finance.dto;

import java.math.BigDecimal;

public record StudentDebtItemResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        BigDecimal remainingDebt,
        long debtInvoiceCount
) {
}
