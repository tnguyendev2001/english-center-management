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
        String classroomName,
        Long enrollmentId,
        Long studentPackageId,
        String packageNameSnapshot,
        Integer totalSessionsSnapshot,
        BigDecimal amount,
        BigDecimal discountAmount,
        BigDecimal adjustmentAmount,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        LocalDate dueDate,
        InvoiceStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
