package com.englishcenter.enrollment.mapper;

import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.dto.EnrollmentLearningProgressResponse;
import com.englishcenter.studentpackage.LearningProgressWarningType;
import com.englishcenter.studentpackage.StudentPackage;
import org.springframework.stereotype.Component;

@Component
public class EnrollmentLearningProgressMapper {
    public EnrollmentLearningProgressResponse toResponse(
            Enrollment enrollment,
            StudentPackage latestStudentPackage,
            int makeupAvailableSessions
    ) {
        int totalSessions = enrollment.getTotalSessions();
        int usedSessions = enrollment.getUsedSessions();
        int remainingSessions = Math.max(totalSessions - usedSessions, 0);
        int overusedSessions = Math.max(usedSessions - totalSessions, 0);
        LearningProgressWarningType warningType = resolveWarningType(remainingSessions, overusedSessions);
        String warningMessage = buildWarningMessage(remainingSessions, overusedSessions);

        return new EnrollmentLearningProgressResponse(
                enrollment.getId(),
                enrollment.getStudent().getId(),
                enrollment.getStudent().getFullName(),
                enrollment.getClassroom().getId(),
                enrollment.getClassroom().getClassName(),
                totalSessions,
                usedSessions,
                remainingSessions,
                overusedSessions,
                latestStudentPackage == null ? null : latestStudentPackage.getId(),
                latestStudentPackage == null ? enrollment.getPackageNameSnapshot() : latestStudentPackage.getPackageName(),
                latestStudentPackage == null ? enrollment.getPackagePriceSnapshot() : latestStudentPackage.getPrice(),
                latestStudentPackage == null ? enrollment.getTotalSessionsSnapshot() : latestStudentPackage.getTotalSessions(),
                latestStudentPackage == null
                        ? enrollment.getSelectedPackage().getId()
                        : latestStudentPackage.getTuitionPackage().getId(),
                makeupAvailableSessions,
                warningType,
                warningMessage
        );
    }

    private LearningProgressWarningType resolveWarningType(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0) {
            return LearningProgressWarningType.OVERUSED;
        }

        if (remainingSessions <= 0) {
            return LearningProgressWarningType.DEPLETED;
        }

        if (remainingSessions <= 2) {
            return LearningProgressWarningType.LOW;
        }

        return LearningProgressWarningType.OK;
    }

    private String buildWarningMessage(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0) {
            return "Vượt " + overusedSessions + " buổi - cần gia hạn";
        }

        if (remainingSessions <= 0) {
            return "Hết buổi - cần gia hạn";
        }

        if (remainingSessions <= 2) {
            return "Sắp hết buổi";
        }

        return null;
    }
}
