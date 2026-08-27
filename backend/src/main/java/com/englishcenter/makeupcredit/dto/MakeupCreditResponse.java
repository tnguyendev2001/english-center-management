package com.englishcenter.makeupcredit.dto;

import com.englishcenter.makeupcredit.MakeupCreditReason;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Approved-leave tracking DTO (user-facing "Nghỉ phép").
 * Backed by MakeupCredit for compatibility; not an extra class session.
 */
public record MakeupCreditResponse(
        Long id,
        Long studentId,
        String studentCode,
        String studentName,
        Long classroomId,
        String classroomName,
        Long sourceSessionId,
        Integer sourceSessionNo,
        LocalDate sourceSessionDate,
        MakeupCreditReason reason,
        MakeupCreditStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
