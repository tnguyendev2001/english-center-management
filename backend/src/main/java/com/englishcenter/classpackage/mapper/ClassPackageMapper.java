package com.englishcenter.classpackage.mapper;

import com.englishcenter.classpackage.ClassPackage;
import com.englishcenter.classpackage.dto.ClassPackageResponse;
import com.englishcenter.tuitionpackage.TuitionPackage;
import org.springframework.stereotype.Component;

@Component
public class ClassPackageMapper {
    public ClassPackageResponse toResponse(ClassPackage classPackage) {
        TuitionPackage tuitionPackage = classPackage.getTuitionPackage();

        return new ClassPackageResponse(
                classPackage.getId(),
                classPackage.getClassroom().getId(),
                tuitionPackage.getId(),
                tuitionPackage.getName(),
                tuitionPackage.getSessionsPerWeek(),
                tuitionPackage.getTotalSessions(),
                tuitionPackage.getExpectedMonths(),
                tuitionPackage.getPrice(),
                tuitionPackage.getStatus(),
                classPackage.getActive(),
                classPackage.getCreatedAt(),
                classPackage.getUpdatedAt()
        );
    }
}
