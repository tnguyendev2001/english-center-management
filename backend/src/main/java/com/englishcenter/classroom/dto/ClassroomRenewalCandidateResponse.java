package com.englishcenter.classroom.dto;

public record ClassroomRenewalCandidateResponse(
        Long studentId,
        Long enrollmentId,
        Long studentPackageId,
        String studentName,
        String currentPackageName,
        Integer currentPackageTotalSessions,
        int usedSessions,
        int remainingSessions,
        boolean hasPendingPackage,
        String pendingPackageName,
        Long suggestedRenewalPackageId,
        boolean eligibleForRenewal,
        String reason
) {
}
