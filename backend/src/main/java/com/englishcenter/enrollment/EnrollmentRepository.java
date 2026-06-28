package com.englishcenter.enrollment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndClassroomIdAndStatus(Long studentId, Long classroomId, EnrollmentStatus status);

    boolean existsByStudentIdAndClassroomIdAndStatusIn(
            Long studentId,
            Long classroomId,
            Collection<EnrollmentStatus> statuses
    );

    Page<Enrollment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByClassroomIdAndStatus(Long classroomId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
}
