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
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        BigDecimal amount,
        LocalDate paymentDate,
        PaymentMethod method,
        PaymentStatus status,
        String note,
        String cancelReason,
        LocalDateTime createdAt,
        LocalDateTime canceledAt
) {
}
