package com.englishcenter.enrollment;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    boolean existsByStudentIdAndClassroomIdAndStatus(Long studentId, Long classroomId, EnrollmentStatus status);

    Page<Enrollment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<Enrollment> findByClassroomIdAndStatus(Long classroomId, EnrollmentStatus status);
}
