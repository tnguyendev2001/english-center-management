package com.englishcenter.invoice.dto;

import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.dto.PaymentResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record InvoiceDocumentResponse(
        CenterProfileResponse center,
        String documentTitle,
        Long invoiceId,
        String invoiceCode,
        LocalDate issueDate,
        LocalDate dueDate,
        InvoiceStatus status,
        String studentCode,
        String studentName,
        String parentName,
        String parentPhone,
        String classroomCode,
        String classroomName,
        String teacherName,
        String packageName,
        Integer packageSessionCount,
        BigDecimal packagePriceSnapshot,
        Integer cycleNo,
        String billingLabel,
        LocalDate effectiveFrom,
        LocalDate estimatedEffectiveTo,
        String estimatedEffectiveToDisplay,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String transferContentSuggestion,
        String note,
        String nextCycleMessage,
        NextCycleSummary nextCycle,
        Integer remainingSessionsBeforeRenewal,
        List<PaymentResponse> payments
) {
    public record NextCycleSummary(
            Long invoiceId,
            String invoiceCode,
            String billingLabel,
            LocalDate effectiveFrom,
            LocalDate dueDate,
            BigDecimal remainingAmount
    ) {
    }
}
