package com.englishcenter.report.dto;

import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.studentpackage.StudentPackageSourceType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DebtReportItemResponse(
        Long studentId,
        String studentName,
        String classroomName,
        Long invoiceId,
        String invoiceCode,
        StudentPackageSourceType invoiceType,
        LocalDateTime issueDate,
        LocalDate dueDate,
        BigDecimal finalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        InvoiceStatus status,
        String latestPackageName
) {
}
