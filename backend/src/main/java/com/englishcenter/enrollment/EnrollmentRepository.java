package com.englishcenter.enrollment;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    long countByStatus(EnrollmentStatus status);

    @Query("""
            SELECT COUNT(enrollment)
            FROM Enrollment enrollment
            WHERE enrollment.status = :status
              AND (enrollment.totalSessions - enrollment.usedSessions) <= 0
            """)
    long countDepletedByStatus(@Param("status") EnrollmentStatus status);

    @Query("""
            SELECT COUNT(enrollment)
            FROM Enrollment enrollment
            WHERE enrollment.status = :status
              AND (enrollment.totalSessions - enrollment.usedSessions) > 0
              AND (enrollment.totalSessions - enrollment.usedSessions) <= :threshold
            """)
    long countLowSessionsByStatus(
            @Param("status") EnrollmentStatus status,
            @Param("threshold") int threshold
    );

    @Query("""
            SELECT COUNT(enrollment)
            FROM Enrollment enrollment
            WHERE enrollment.classroom.id = :classroomId
              AND enrollment.status = :status
            """)
    long countByClassroomIdAndStatus(
            @Param("classroomId") Long classroomId,
            @Param("status") EnrollmentStatus status
    );

    @Query("""
            SELECT COUNT(enrollment)
            FROM Enrollment enrollment
            WHERE enrollment.classroom.id = :classroomId
              AND enrollment.status = :status
              AND (enrollment.totalSessions - enrollment.usedSessions) <= 0
            """)
    long countDepletedByClassroomIdAndStatus(
            @Param("classroomId") Long classroomId,
            @Param("status") EnrollmentStatus status
    );

    @Query("""
            SELECT COUNT(enrollment)
            FROM Enrollment enrollment
            WHERE enrollment.classroom.id = :classroomId
              AND enrollment.status = :status
              AND (enrollment.totalSessions - enrollment.usedSessions) > 0
              AND (enrollment.totalSessions - enrollment.usedSessions) <= :threshold
            """)
    long countLowSessionsByClassroomIdAndStatus(
            @Param("classroomId") Long classroomId,
            @Param("status") EnrollmentStatus status,
            @Param("threshold") int threshold
    );

    @EntityGraph(attributePaths = {"student", "classroom"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.status = com.englishcenter.enrollment.EnrollmentStatus.ACTIVE
              AND (enrollment.totalSessions - enrollment.usedSessions) <= :threshold
            ORDER BY (enrollment.totalSessions - enrollment.usedSessions) ASC,
                     enrollment.student.fullName ASC
            """)
    List<Enrollment> findSessionWarnings(@Param("threshold") int threshold);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.status = com.englishcenter.enrollment.EnrollmentStatus.ACTIVE
            ORDER BY enrollment.classroom.className ASC, enrollment.student.fullName ASC
            """)
    List<Enrollment> findAllActiveWithRelations();
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
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.classroom.id = :classroomId
              AND enrollment.status = :status
              AND enrollment.startDate <= :sessionDate
              AND (enrollment.endDate IS NULL OR enrollment.endDate >= :sessionDate)
            ORDER BY enrollment.student.fullName ASC
            """)
    List<Enrollment> findEligibleForAttendanceBySessionDate(
            @Param("classroomId") Long classroomId,
            @Param("status") EnrollmentStatus status,
            @Param("sessionDate") LocalDate sessionDate
    );

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);
}
