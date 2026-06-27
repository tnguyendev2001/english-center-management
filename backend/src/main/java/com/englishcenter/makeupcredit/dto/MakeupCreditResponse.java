package com.englishcenter.makeupcredit.dto;

import com.englishcenter.makeupcredit.MakeupCreditReason;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import java.time.LocalDateTime;

public record MakeupCreditResponse(
        Long id,
        Long studentId,
        String studentName,
        Long classroomId,
        String classroomName,
        Long sourceSessionId,
        MakeupCreditReason reason,
        Integer creditSessions,
        Integer usedSessions,
        MakeupCreditStatus status,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
