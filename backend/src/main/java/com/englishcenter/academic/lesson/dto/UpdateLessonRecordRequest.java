package com.englishcenter.academic.lesson.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateLessonRecordRequest(
        @NotBlank @Size(max = 255) String title,
        String objectives,
        String plannedContent,
        String actualContent,
        String vocabulary,
        String grammarTopics,
        String skills,
        String homeworkInstruction,
        String teacherNote
) {
}
