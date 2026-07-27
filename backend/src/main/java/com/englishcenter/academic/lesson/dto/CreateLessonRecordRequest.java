package com.englishcenter.academic.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateLessonRecordRequest(
        @NotNull Long classSessionId,
        @NotBlank @Size(max = 255) String title,
        String objectives,
        String plannedContent,
        String vocabulary,
        String grammarTopics,
        String skills,
        String homeworkInstruction,
        String teacherNote
) {
}
