package com.englishcenter.invoice.dto;

public record AmbiguousInvoiceDiagnosticResponse(
        Long invoiceId,
        String invoiceCode,
        Long studentId,
        String studentCode,
        String studentName,
        Long enrollmentId,
        Long studentPackageId,
        Integer packageCycleNo,
        Integer invoiceCycleNo,
        java.time.LocalDate issueDate,
        Integer candidateCycleNo,
        String reason
) {
}
