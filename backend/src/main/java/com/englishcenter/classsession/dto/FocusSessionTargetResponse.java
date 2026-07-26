package com.englishcenter.classsession.dto;

import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classsession.FocusSessionType;
import java.time.LocalDate;
import java.time.LocalTime;

public record FocusSessionTargetResponse(
        Long sessionId,
        Integer page,
        FocusSessionType type,
        LocalDate sessionDate,
        LocalTime startTime,
        LocalTime endTime,
        ClassSessionStatus status,
        Integer sessionNo,
        Integer markedCount,
        Integer totalStudents
) {
}
