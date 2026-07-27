package com.englishcenter.enrollment.dto;

import java.util.List;

public record CanceledEnrollmentInvoiceRepairResponse(
        boolean dryRun,
        int detectedCount,
        int safeCount,
        int unsafeCount,
        int repairedCount,
        List<Long> repairedInvoiceIds,
        List<CanceledEnrollmentInvoiceDiagnosticResponse> unsafeRows
) {
}
