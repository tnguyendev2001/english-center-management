package com.englishcenter.studentpackage.dto;

import com.englishcenter.studentpackage.StudentPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentPackageProgressResponse(
        Long id,
        Long studentId,
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
        LocalDateTime updatedAt,
        int usedSessions,
        int remainingSessions,
        int makeupAvailableSessions,
        int totalAvailableSessions
) {
}
