package com.englishcenter.student.dto;

import com.englishcenter.student.StudentStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record StudentResponse(
        Long id,
        String studentCode,
        String fullName,
        LocalDate dateOfBirth,
        String phone,
        String parentName,
        String parentPhone,
        String address,
        StudentStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
