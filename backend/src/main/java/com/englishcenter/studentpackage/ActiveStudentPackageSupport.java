package com.englishcenter.studentpackage;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class ActiveStudentPackageSupport {
    private ActiveStudentPackageSupport() {
    }

    public static Optional<StudentPackage> pickCurrentActive(List<StudentPackage> packages) {
        return packages.stream()
                .max(Comparator.comparing(StudentPackage::getCycleNo)
                        .thenComparing(StudentPackage::getId));
    }

    public static List<StudentPackage> dedupeByEnrollment(List<StudentPackage> packages) {
        Map<Long, StudentPackage> byEnrollment = new LinkedHashMap<>();
        for (StudentPackage studentPackage : packages) {
            byEnrollment.merge(
                    studentPackage.getEnrollment().getId(),
                    studentPackage,
                    ActiveStudentPackageSupport::preferNewerPackage
            );
        }
        return List.copyOf(byEnrollment.values());
    }

    private static StudentPackage preferNewerPackage(StudentPackage left, StudentPackage right) {
        if (!left.getCycleNo().equals(right.getCycleNo())) {
            return left.getCycleNo() > right.getCycleNo() ? left : right;
        }

        return left.getId() > right.getId() ? left : right;
    }
}
