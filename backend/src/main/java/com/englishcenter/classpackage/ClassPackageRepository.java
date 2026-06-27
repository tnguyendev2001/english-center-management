package com.englishcenter.classpackage;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassPackageRepository extends JpaRepository<ClassPackage, Long> {
    List<ClassPackage> findByClassroomIdAndActiveTrueOrderByCreatedAtDesc(Long classroomId);

    Optional<ClassPackage> findByClassroomIdAndTuitionPackageId(Long classroomId, Long tuitionPackageId);

    boolean existsByClassroomIdAndTuitionPackageIdAndActiveTrue(Long classroomId, Long tuitionPackageId);
}
