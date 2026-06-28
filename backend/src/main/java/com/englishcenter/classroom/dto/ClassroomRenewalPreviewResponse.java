package com.englishcenter.classroom.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClassroomRenewalPreviewResponse(
        int totalSelectedStudents,
        BigDecimal totalInvoiceAmount,
        List<ClassroomRenewalPreviewItemResponse> items
) {
}
