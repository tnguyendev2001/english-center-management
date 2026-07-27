package com.englishcenter.enrollment;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT enrollment FROM Enrollment enrollment WHERE enrollment.id = :id")
    Optional<Enrollment> findByIdForUpdate(@Param("id") Long id);

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
    boolean existsByStudentIdAndClassroomId(Long studentId, Long classroomId);

    boolean existsByStudentIdAndClassroomIdAndStatus(Long studentId, Long classroomId, EnrollmentStatus status);

    boolean existsByStudentIdAndClassroomIdAndStatusIn(
            Long studentId,
            Long classroomId,
            Collection<EnrollmentStatus> statuses
    );

    boolean existsByStudentIdAndClassroomIdAndStatusAndIdNot(
            Long studentId,
            Long classroomId,
            EnrollmentStatus status,
            Long id
    );

    Optional<Enrollment> findFirstByStudentIdAndClassroomIdAndStatusInOrderByIdDesc(
            Long studentId,
            Long classroomId,
            Collection<EnrollmentStatus> statuses
    );

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(
            Long studentId,
            Long classroomId
    );

    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE EXISTS (
                SELECT 1
                FROM Enrollment duplicate
                WHERE duplicate.student.id = enrollment.student.id
                  AND duplicate.classroom.id = enrollment.classroom.id
                  AND duplicate.id <> enrollment.id
            )
            ORDER BY enrollment.student.id, enrollment.classroom.id, enrollment.startDate, enrollment.id
            """)
    List<Enrollment> findDuplicateStudentClassroomEnrollments();

    Page<Enrollment> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByClassroomIdAndStatus(Long classroomId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByClassroomIdOrderByStartDateDescIdDesc(Long classroomId);

    /**
     * Attendance eligibility uses ACTIVE history periods only (half-open):
     * effectiveFrom &lt;= sessionDate and (effectiveTo is null or sessionDate &lt; effectiveTo).
     * Current Enrollment.status is intentionally not used.
     */
    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    @Query("""
            SELECT DISTINCT enrollment
            FROM EnrollmentStatusHistory history
            JOIN history.enrollment enrollment
            WHERE enrollment.classroom.id = :classroomId
              AND enrollment.startDate <= :sessionDate
              AND history.status = com.englishcenter.enrollment.EnrollmentStatus.ACTIVE
              AND history.effectiveFrom <= :sessionDate
              AND (history.effectiveTo IS NULL OR :sessionDate < history.effectiveTo)
            ORDER BY enrollment.student.fullName ASC
            """)
    List<Enrollment> findEligibleForAttendanceBySessionDate(
            @Param("classroomId") Long classroomId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByStudentIdAndStatus(Long studentId, EnrollmentStatus status);

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    List<Enrollment> findByStudentIdOrderByStartDateDescIdDesc(Long studentId);

    @Query("""
            SELECT CASE WHEN COUNT(enrollment) > 0 THEN TRUE ELSE FALSE END
            FROM Enrollment enrollment
            WHERE enrollment.student.id = :studentId
              AND enrollment.classroom.teacherId = :teacherId
            """)
    boolean existsByStudentIdAndClassroomTeacherId(
            @Param("studentId") Long studentId,
            @Param("teacherId") Long teacherId
    );

    @EntityGraph(attributePaths = {"student", "classroom", "selectedPackage"})
    @Query("""
            SELECT enrollment
            FROM Enrollment enrollment
            WHERE enrollment.classroom.teacherId = :teacherId
            ORDER BY enrollment.classroom.className ASC, enrollment.student.fullName ASC
            """)
    List<Enrollment> findByClassroomTeacherId(@Param("teacherId") Long teacherId);
}
