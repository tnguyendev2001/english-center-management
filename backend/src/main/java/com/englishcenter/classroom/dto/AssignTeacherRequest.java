package com.englishcenter.classroom.dto;

import jakarta.validation.constraints.NotNull;

public record AssignTeacherRequest(
        @NotNull(message = "Vui lòng chọn giáo viên phụ trách lớp.")
        Long teacherId
) {
}
