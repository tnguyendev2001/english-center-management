package com.englishcenter.academic.assignment.dto;

import com.englishcenter.academic.assignment.AssignmentTargetMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record UpdateAssignmentRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        String instructions,
        Long lessonRecordId,
        @NotNull LocalDate assignedDate,
        LocalDate dueDate,
        BigDecimal maxScore,
        Boolean allowSubmission,
        Boolean allowLateSubmission,
        @NotNull AssignmentTargetMode targetMode,
        List<Long> targetStudentIds
) {
}
