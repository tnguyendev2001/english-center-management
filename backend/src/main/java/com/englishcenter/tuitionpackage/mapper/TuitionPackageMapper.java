package com.englishcenter.tuitionpackage.mapper;

import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.dto.TuitionPackageCreateRequest;
import com.englishcenter.tuitionpackage.dto.TuitionPackageResponse;
import com.englishcenter.tuitionpackage.dto.TuitionPackageUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class TuitionPackageMapper {
    public TuitionPackage toEntity(TuitionPackageCreateRequest request) {
        TuitionPackage tuitionPackage = new TuitionPackage();
        updateEntity(tuitionPackage, request);
        return tuitionPackage;
    }

    public void updateEntity(TuitionPackage tuitionPackage, TuitionPackageCreateRequest request) {
        tuitionPackage.setName(request.name().trim());
        tuitionPackage.setSessionsPerWeek(request.sessionsPerWeek());
        tuitionPackage.setTotalSessions(request.totalSessions());
        tuitionPackage.setExpectedMonths(request.expectedMonths());
        tuitionPackage.setPrice(request.price());
        tuitionPackage.setStatus(request.status());
    }

    public void updateEntity(TuitionPackage tuitionPackage, TuitionPackageUpdateRequest request) {
        tuitionPackage.setName(request.name().trim());
        tuitionPackage.setSessionsPerWeek(request.sessionsPerWeek());
        tuitionPackage.setTotalSessions(request.totalSessions());
        tuitionPackage.setExpectedMonths(request.expectedMonths());
        tuitionPackage.setPrice(request.price());
        tuitionPackage.setStatus(request.status());
    }

    public TuitionPackageResponse toResponse(TuitionPackage tuitionPackage) {
        return new TuitionPackageResponse(
                tuitionPackage.getId(),
                tuitionPackage.getName(),
                tuitionPackage.getSessionsPerWeek(),
                tuitionPackage.getTotalSessions(),
                tuitionPackage.getExpectedMonths(),
                tuitionPackage.getPrice(),
                tuitionPackage.getStatus(),
                tuitionPackage.getCreatedAt(),
                tuitionPackage.getUpdatedAt()
        );
    }
}
