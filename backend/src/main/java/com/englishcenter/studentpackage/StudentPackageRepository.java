package com.englishcenter.studentpackage;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, Long> {
    Optional<StudentPackage> findByEnrollmentId(Long enrollmentId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByStudentIdOrderByStartDateDesc(Long studentId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByClassroomIdOrderByStartDateDesc(Long classroomId);
}
