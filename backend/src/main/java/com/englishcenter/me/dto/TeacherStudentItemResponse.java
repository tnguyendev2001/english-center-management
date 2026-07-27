package com.englishcenter.me.dto;

import com.englishcenter.enrollment.EnrollmentStatus;
import java.util.List;

public record TeacherStudentItemResponse(
        Long studentId,
        String studentCode,
        String fullName,
        List<String> classroomNames,
        EnrollmentStatus learningStatus,
        int usedSessions,
        int remainingSessions,
        String nextSessionLabel
) {
}
