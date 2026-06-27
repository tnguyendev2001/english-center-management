package com.englishcenter.student.dto;

import com.englishcenter.student.StudentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record StudentUpdateRequest(
        @NotBlank(message = "Student code is required")
        @Size(max = 50, message = "Student code must not exceed 50 characters")
        String studentCode,

        @NotBlank(message = "Full name is required")
        @Size(max = 255, message = "Full name must not exceed 255 characters")
        String fullName,

        LocalDate dateOfBirth,

        @Size(max = 30, message = "Phone must not exceed 30 characters")
        String phone,

        @Size(max = 255, message = "Parent name must not exceed 255 characters")
        String parentName,

        @Size(max = 30, message = "Parent phone must not exceed 30 characters")
        String parentPhone,

        @Size(max = 500, message = "Address must not exceed 500 characters")
        String address,

        @NotNull(message = "Status is required")
        StudentStatus status,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
