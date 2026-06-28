package com.englishcenter.studentpackage;

import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentPackageRepository extends JpaRepository<StudentPackage, Long> {
    Optional<StudentPackage> findByEnrollmentId(Long enrollmentId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByEnrollmentIdAndStatusOrderByStartDateDesc(
            Long enrollmentId,
            StudentPackageStatus status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    @Query("""
            SELECT studentPackage
            FROM StudentPackage studentPackage
            WHERE studentPackage.enrollment.id = :enrollmentId
              AND studentPackage.status = :status
            ORDER BY studentPackage.startDate DESC, studentPackage.id DESC
            """)
    List<StudentPackage> findByEnrollmentIdAndStatusForUpdate(
            @Param("enrollmentId") Long enrollmentId,
            @Param("status") StudentPackageStatus status
    );

    boolean existsByEnrollmentIdAndStatus(Long enrollmentId, StudentPackageStatus status);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    @Query("""
            SELECT studentPackage
            FROM StudentPackage studentPackage
            WHERE studentPackage.id = :id
            """)
    Optional<StudentPackage> findWithRelationsById(@Param("id") Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    @Query("""
            SELECT studentPackage
            FROM StudentPackage studentPackage
            WHERE studentPackage.id = :id
            """)
    Optional<StudentPackage> findWithRelationsForUpdateById(@Param("id") Long id);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByStudentIdOrderByStartDateDesc(Long studentId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByStudentIdAndStatusOrderByStartDateDesc(
            Long studentId,
            StudentPackageStatus status
    );

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByClassroomIdOrderByStartDateDesc(Long classroomId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    List<StudentPackage> findByClassroomIdAndStatusOrderByStartDateDesc(
            Long classroomId,
            StudentPackageStatus status
    );

    @Query("""
            SELECT COALESCE(MAX(studentPackage.cycleNo), 0)
            FROM StudentPackage studentPackage
            WHERE studentPackage.enrollment.id = :enrollmentId
            """)
    int findMaxCycleNoByEnrollmentId(@Param("enrollmentId") Long enrollmentId);

    @EntityGraph(attributePaths = {"student", "classroom", "enrollment", "tuitionPackage"})
    Optional<StudentPackage> findTopByEnrollmentIdOrderByCycleNoDescIdDesc(Long enrollmentId);
}
