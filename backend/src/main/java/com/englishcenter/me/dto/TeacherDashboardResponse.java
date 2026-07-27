package com.englishcenter.me.dto;

import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import java.util.List;

public record TeacherDashboardResponse(
        int assignedClassroomCount,
        int activeStudentCount,
        int todaySessionCount,
        int incompleteAttendanceCount,
        List<TodayClassItem> todayClasses,
        List<ClassSessionResponse> upcomingSessions,
        List<ActionItem> actionItems,
        List<ClassroomResponse> assignedClassrooms
) {
    public record TodayClassItem(
            Long sessionId,
            Long classroomId,
            String className,
            String startTime,
            String endTime,
            String room,
            int studentCount
    ) {
    }

    public record ActionItem(
            String type,
            String message,
            Long sessionId,
            Long classroomId
    ) {
    }
}
