package com.englishcenter.attendance.dto;

import com.englishcenter.attendance.AttendanceStatus;
import java.time.LocalDateTime;

public record AttendanceResponse(
        Long id,
        Long sessionId,
        Long studentId,
        String studentCode,
        String studentName,
        AttendanceStatus status,
        String note,
        LocalDateTime markedAt,
        boolean valid,
        String voidReason,
        LocalDateTime voidedAt
) {
}
