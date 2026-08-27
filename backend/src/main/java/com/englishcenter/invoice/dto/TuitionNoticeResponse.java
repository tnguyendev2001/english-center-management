package com.englishcenter.invoice.dto;

import com.englishcenter.invoice.InvoiceStatus;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TuitionNoticeResponse(
        Long invoiceId,
        String invoiceCode,
        String centerName,
        String centerAddress,
        String centerPhone,
        LocalDate noticeDate,
        String studentCode,
        String studentName,
        String classroomName,
        String packageName,
        Integer packageSessionCount,
        LocalDate periodStart,
        LocalDate periodEnd,
        BigDecimal invoiceAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        String amountInWords,
        LocalDate dueDate,
        InvoiceStatus invoiceStatus,
        String noticeText,
        String footerText
) {
}
