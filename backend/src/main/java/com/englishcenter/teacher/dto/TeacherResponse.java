package com.englishcenter.teacher.dto;

import com.englishcenter.teacher.TeacherStatus;
import java.time.LocalDateTime;

public record TeacherResponse(
        Long id,
        String teacherCode,
        String fullName,
        String email,
        String phone,
        TeacherStatus status,
        String note,
        boolean hasUserAccount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
