package com.englishcenter.studentpackage.dto;

import com.englishcenter.studentpackage.StudentPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentPackageResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        Long enrollmentId,
        Long tuitionPackageId,
        String packageName,
        Integer totalSessions,
        BigDecimal price,
        BigDecimal discountAmount,
        BigDecimal adjustmentAmount,
        BigDecimal finalAmount,
        LocalDate startDate,
        LocalDate endDate,
        StudentPackageStatus status,
        Integer cycleNo,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
