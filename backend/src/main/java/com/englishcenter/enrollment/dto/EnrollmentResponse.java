package com.englishcenter.enrollment.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.studentpackage.dto.StudentPackageResponse;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record EnrollmentResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        LocalDate startDate,
        LocalDate endDate,
        EnrollmentStatus status,
        Long selectedPackageId,
        String packageNameSnapshot,
        Integer totalSessionsSnapshot,
        BigDecimal packagePriceSnapshot,
        BigDecimal discountAmount,
        BigDecimal finalAmount,
        String note,
        StudentPackageResponse studentPackage,
        InvoiceResponse invoice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
