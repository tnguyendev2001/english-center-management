package com.englishcenter.debt.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record StudentDebtSummaryResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long currentClassroomId,
        String currentClassroomName,
        BigDecimal totalRemainingDebt,
        int debtInvoiceCount,
        int unpaidCount,
        int partialCount,
        LocalDate nearestDueDate
) {
    public StudentDebtSummaryResponse withCurrentClassroom(Long classroomId, String classroomName) {
        return new StudentDebtSummaryResponse(
                studentId,
                studentCode,
                studentName,
                classroomId,
                classroomName,
                totalRemainingDebt,
                debtInvoiceCount,
                unpaidCount,
                partialCount,
                nearestDueDate
        );
    }
}
