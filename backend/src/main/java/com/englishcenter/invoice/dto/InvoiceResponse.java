package com.englishcenter.invoice.dto;

import com.englishcenter.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record InvoiceResponse(
        Long id,
        String invoiceCode,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomCode,
        String classroomName,
        String teacherName,
        Long enrollmentId,
        Long studentPackageId,
        Long packageId,
        String packageCode,
        String packageNameSnapshot,
        Integer totalSessionsSnapshot,
        BigDecimal packagePriceSnapshot,
        Integer cycleNo,
        String billingLabel,
        LocalDate effectiveFrom,
        LocalDate estimatedEffectiveTo,
        BigDecimal amount,
        BigDecimal discountAmount,
        BigDecimal adjustmentAmount,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate issueDate,
        LocalDate dueDate,
        Integer overdueDays,
        String debtStatus,
        InvoiceStatus status,
        String note,
        String source,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
