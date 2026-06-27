package com.englishcenter.enrollment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EnrollStudentRequest(
        @NotNull(message = "Student id is required")
        Long studentId,

        @NotNull(message = "Classroom id is required")
        Long classroomId,

        @NotNull(message = "Tuition package id is required")
        Long tuitionPackageId,

        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @DecimalMin(value = "0.00", message = "Discount amount must be greater than or equal to 0")
        BigDecimal discountAmount,

        @Size(max = 1000, message = "Note must not exceed 1000 characters")
        String note
) {
}
