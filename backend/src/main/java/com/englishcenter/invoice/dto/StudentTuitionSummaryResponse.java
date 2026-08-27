package com.englishcenter.invoice.dto;

import java.math.BigDecimal;

public record StudentTuitionSummaryResponse(
        Long studentId,
        String studentCode,
        String studentName,
        Long currentClassroomId,
        String currentClassroomName,
        BigDecimal totalTuitionAmount,
        BigDecimal totalPaidAmount,
        BigDecimal remainingDebt,
        int totalInvoiceCount,
        int unpaidCount,
        int partialCount,
        int paidCount,
        boolean hasReplacedInvoices
) {
    public StudentTuitionSummaryResponse withCurrentClassroom(Long classroomId, String classroomName) {
        return new StudentTuitionSummaryResponse(
                studentId,
                studentCode,
                studentName,
                classroomId,
                classroomName,
                totalTuitionAmount,
                totalPaidAmount,
                remainingDebt,
                totalInvoiceCount,
                unpaidCount,
                partialCount,
                paidCount,
                hasReplacedInvoices
        );
    }
}
