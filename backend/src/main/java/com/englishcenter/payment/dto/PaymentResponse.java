package com.englishcenter.payment.dto;

import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentResponse(
        Long id,
        String paymentCode,
        Long invoiceId,
        String invoiceCode,
        Integer cycleNo,
        String billingLabel,
        String packageNameSnapshot,
        LocalDate effectiveFrom,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomCode,
        String classroomName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        Long financialAccountId,
        String financialAccountCode,
        String financialAccountName,
        PaymentStatus status,
        String note,
        String cancelReason,
        String createdBy,
        String canceledBy,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
}
