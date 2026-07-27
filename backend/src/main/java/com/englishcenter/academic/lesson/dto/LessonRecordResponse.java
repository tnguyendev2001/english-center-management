package com.englishcenter.academic.lesson.dto;

import com.englishcenter.academic.lesson.LessonStatus;
import java.time.LocalDateTime;

public record LessonRecordResponse(
        Long id,
        Long classSessionId,
        Long classroomId,
        String title,
        String objectives,
        String plannedContent,
        String actualContent,
        String vocabulary,
        String grammarTopics,
        String skills,
        String homeworkInstruction,
        String teacherNote,
        LessonStatus status,
        String createdBy,
        LocalDateTime createdAt,
        String updatedBy,
        LocalDateTime updatedAt
) {
}
