package com.englishcenter.studentpackage;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPackagePeriodAdjustmentRepository
        extends JpaRepository<StudentPackagePeriodAdjustment, Long> {
    Optional<StudentPackagePeriodAdjustment> findTopByStudentPackageIdOrderByChangedAtDescIdDesc(
            Long studentPackageId
    );
}
