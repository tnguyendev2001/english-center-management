package com.englishcenter.studentpackage.mapper;

import com.englishcenter.studentpackage.LearningProgressWarningType;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;
import com.englishcenter.studentpackage.dto.StudentPackageResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentPackageMapper {
    public StudentPackageResponse toResponse(StudentPackage studentPackage) {
        return new StudentPackageResponse(
                studentPackage.getId(),
                studentPackage.getStudent().getId(),
                studentPackage.getStudent().getStudentCode(),
                studentPackage.getStudent().getFullName(),
                studentPackage.getClassroom().getId(),
                studentPackage.getClassroom().getClassName(),
                studentPackage.getEnrollment().getId(),
                studentPackage.getTuitionPackage().getId(),
                studentPackage.getPackageName(),
                studentPackage.getTotalSessions(),
                studentPackage.getPrice(),
                studentPackage.getDiscountAmount(),
                studentPackage.getAdjustmentAmount(),
                studentPackage.getFinalAmount(),
                studentPackage.getStartDate(),
                studentPackage.getEndDate(),
                studentPackage.getStatus(),
                studentPackage.getCycleNo(),
                studentPackage.getCreatedAt(),
                studentPackage.getUpdatedAt()
        );
    }

    public StudentPackageProgressResponse toProgressResponse(
            StudentPackage studentPackage,
            int usedSessions,
            int makeupAvailableSessions
    ) {
        int totalSessions = studentPackage.getTotalSessions();
        int remainingSessions = Math.max(totalSessions - usedSessions, 0);
        int overusedSessions = Math.max(usedSessions - totalSessions, 0);
        LearningProgressWarningType warningType = resolveWarningType(remainingSessions, overusedSessions);
        String warningMessage = buildWarningMessage(remainingSessions, overusedSessions);

        return new StudentPackageProgressResponse(
                studentPackage.getId(),
                studentPackage.getStudent().getId(),
                studentPackage.getStudent().getStudentCode(),
                studentPackage.getStudent().getFullName(),
                studentPackage.getClassroom().getId(),
                studentPackage.getClassroom().getClassName(),
                studentPackage.getEnrollment().getId(),
                studentPackage.getTuitionPackage().getId(),
                studentPackage.getPackageName(),
                totalSessions,
                studentPackage.getPrice(),
                studentPackage.getDiscountAmount(),
                studentPackage.getAdjustmentAmount(),
                studentPackage.getFinalAmount(),
                studentPackage.getStartDate(),
                studentPackage.getEndDate(),
                studentPackage.getStatus(),
                studentPackage.getCycleNo(),
                studentPackage.getCreatedAt(),
                studentPackage.getUpdatedAt(),
                usedSessions,
                remainingSessions,
                overusedSessions,
                makeupAvailableSessions,
                warningType,
                warningMessage
        );
    }

    private LearningProgressWarningType resolveWarningType(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0) {
            return LearningProgressWarningType.OVERUSED;
        }

        if (remainingSessions == 0) {
            return LearningProgressWarningType.DEPLETED;
        }

        if (remainingSessions <= 2) {
            return LearningProgressWarningType.LOW;
        }

        return LearningProgressWarningType.NONE;
    }

    private String buildWarningMessage(int remainingSessions, int overusedSessions) {
        if (overusedSessions > 0) {
            return "Vượt " + overusedSessions + " buổi - cần gia hạn";
        }

        if (remainingSessions == 0) {
            return "Hết buổi - cần gia hạn";
        }

        if (remainingSessions <= 2) {
            return "Sắp hết buổi";
        }

        return null;
    }
}
