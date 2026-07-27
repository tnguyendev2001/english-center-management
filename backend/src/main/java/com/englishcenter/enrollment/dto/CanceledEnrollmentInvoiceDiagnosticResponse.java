package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.InvoiceStatus;
import java.math.BigDecimal;

public record CanceledEnrollmentInvoiceDiagnosticResponse(
        Long enrollmentId,
        Long studentId,
        Long classroomId,
        EnrollmentStatus enrollmentStatus,
        Long invoiceId,
        InvoiceStatus invoiceStatus,
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal remainingAmount,
        long validPaymentCount,
        long attendanceCount,
        boolean safeToRepair
) {
}
