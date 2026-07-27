package com.englishcenter.teacher.dto;

import com.englishcenter.teacher.TeacherStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TeacherCreateRequest(
        @NotBlank(message = "Teacher code is required")
        @Size(max = 50)
        String teacherCode,

        @NotBlank(message = "Full name is required")
        @Size(max = 255)
        String fullName,

        @Size(max = 255)
        String email,

        @Size(max = 30)
        String phone,

        @NotNull
        TeacherStatus status,

        @Size(max = 1000)
        String note
) {
}
