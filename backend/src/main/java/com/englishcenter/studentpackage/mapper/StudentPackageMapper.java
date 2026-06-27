package com.englishcenter.studentpackage.mapper;

import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.dto.StudentPackageResponse;
import org.springframework.stereotype.Component;

@Component
public class StudentPackageMapper {
    public StudentPackageResponse toResponse(StudentPackage studentPackage) {
        return new StudentPackageResponse(
                studentPackage.getId(),
                studentPackage.getStudent().getId(),
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
}
