package com.englishcenter.student;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<Student, Long> {
    long countByStatus(StudentStatus status);

    boolean existsByStudentCode(String studentCode);

    boolean existsByStudentCodeAndIdNot(String studentCode, Long id);

    @Query("""
            SELECT student
            FROM Student student
            WHERE LOWER(student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(student.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Student> search(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
            SELECT student
            FROM Student student
            WHERE student.status = com.englishcenter.student.StudentStatus.ACTIVE
              AND NOT EXISTS (
                  SELECT 1
                  FROM Enrollment enrollment
                  WHERE enrollment.student.id = student.id
                    AND enrollment.classroom.id = :classroomId
                    AND enrollment.status IN (
                        com.englishcenter.enrollment.EnrollmentStatus.ACTIVE,
                        com.englishcenter.enrollment.EnrollmentStatus.ON_HOLD
                    )
              )
            ORDER BY student.fullName ASC
            """)
    List<Student> findEligibleForEnrollment(@Param("classroomId") Long classroomId);
}
