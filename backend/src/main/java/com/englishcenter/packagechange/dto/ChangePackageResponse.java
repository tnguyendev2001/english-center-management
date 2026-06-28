package com.englishcenter.packagechange.dto;

import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;

public record ChangePackageResponse(
        Long packageChangeLogId,
        ChangePackagePreviewResponse calculation,
        StudentPackageProgressResponse newStudentPackage,
        InvoiceResponse newInvoice
) {
}
