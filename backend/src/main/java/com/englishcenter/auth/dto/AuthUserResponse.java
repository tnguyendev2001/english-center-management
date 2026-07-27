package com.englishcenter.auth.dto;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.AccountStatus;

public record AuthUserResponse(
        Long id,
        String username,
        AccountRole role,
        AccountStatus status,
        boolean mustChangePassword,
        LinkedProfileSummary linkedProfile
) {
    public record LinkedProfileSummary(
            Long studentId,
            String studentCode,
            String studentName,
            Long teacherId,
            String teacherCode,
            String teacherName
    ) {
    }
}
