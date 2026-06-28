package com.englishcenter.classroom.dto;

import java.math.BigDecimal;
import java.util.List;

public record ClassroomRenewalConfirmResponse(
        int renewedStudents,
        BigDecimal totalInvoiceAmount,
        List<ClassroomRenewalConfirmItemResponse> items
) {
}
