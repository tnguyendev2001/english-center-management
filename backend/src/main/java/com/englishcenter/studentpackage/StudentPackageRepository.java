package com.englishcenter.studentpackage;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, Long> {
    Optional<StudentPackage> findByEnrollmentId(Long enrollmentId);
}
