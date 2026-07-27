package com.englishcenter.auth.dto;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;
import java.time.LocalDateTime;

public record UserAccountResponse(
        Long id,
        String username,
        AccountRole role,
        AccountStatus status,
        Long studentId,
        String studentCode,
        String studentName,
        Long teacherId,
        String teacherCode,
        String teacherName,
        boolean mustChangePassword,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
