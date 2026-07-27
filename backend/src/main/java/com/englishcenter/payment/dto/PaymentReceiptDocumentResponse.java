package com.englishcenter.payment.dto;

import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record PaymentReceiptDocumentResponse(
        CenterProfileResponse center,
        String documentTitle,
        Long paymentId,
        String paymentCode,
        LocalDate paymentDate,
        PaymentStatus status,
        boolean canceled,
        String payerName,
        String studentCode,
        String studentName,
        String classroomCode,
        String classroomName,
        String invoiceCode,
        String billingLabel,
        String packageName,
        LocalDate effectiveFrom,
        BigDecimal invoiceTotalAmount,
        BigDecimal invoiceRemainingAfterPayment,
        BigDecimal amount,
        String amountInWords,
        PaymentMethod method,
        String financialAccountName,
        String referenceNumber,
        String note,
        String recordedBy,
        LocalDateTime createdAt,
        String createdBy,
        LocalDateTime canceledAt,
        String canceledBy,
        String cancelReason,
        String footerNote
) {
}
