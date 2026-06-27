package com.englishcenter.classsession.dto;

import com.englishcenter.classsession.ClassSessionStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ClassSessionResponse(
        Long id,
        Long classroomId,
        String classroomName,
        Integer sessionNo,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        ClassSessionStatus status,
        String cancelReason,
        String note,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
