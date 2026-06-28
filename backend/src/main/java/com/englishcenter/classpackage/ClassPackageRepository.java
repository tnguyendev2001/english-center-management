package com.englishcenter.classpackage;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClassPackageRepository extends JpaRepository<ClassPackage, Long> {
    @EntityGraph(attributePaths = {"tuitionPackage"})
    List<ClassPackage> findByClassroomIdAndActiveTrueOrderByCreatedAtDesc(Long classroomId);

    Optional<ClassPackage> findByClassroomIdAndTuitionPackageId(Long classroomId, Long tuitionPackageId);

    boolean existsByClassroomIdAndTuitionPackageIdAndActiveTrue(Long classroomId, Long tuitionPackageId);
}
