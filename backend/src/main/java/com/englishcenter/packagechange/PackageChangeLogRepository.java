package com.englishcenter.packagechange;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PackageChangeLogRepository extends JpaRepository<PackageChangeLog, Long> {
    boolean existsByOldStudentPackageId(Long oldStudentPackageId);
}
