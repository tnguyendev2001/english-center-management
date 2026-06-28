package com.englishcenter.packagechange.dto;

import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;

public record ChangePackageResponse(
        Long packageChangeLogId,
        ChangePackagePreviewResponse calculation,
        EnrollmentLearningProgressResponse newStudentPackage,
        InvoiceResponse newInvoice
) {
}
