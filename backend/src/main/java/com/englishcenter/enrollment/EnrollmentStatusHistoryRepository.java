package com.englishcenter.enrollment;

import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EnrollmentStatusHistoryRepository
        extends JpaRepository<EnrollmentStatusHistory, Long> {

    List<EnrollmentStatusHistory> findByEnrollmentIdOrderByEffectiveFromAscIdAsc(Long enrollmentId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT history
            FROM EnrollmentStatusHistory history
            WHERE history.enrollment.id = :enrollmentId
            ORDER BY history.id DESC
            """)
    List<EnrollmentStatusHistory> findLatestForUpdate(@Param("enrollmentId") Long enrollmentId);

    @Query("""
            SELECT history
            FROM EnrollmentStatusHistory history
            WHERE history.enrollment.id = :enrollmentId
              AND history.effectiveFrom <= :date
              AND (history.effectiveTo IS NULL OR :date < history.effectiveTo)
            ORDER BY history.effectiveFrom DESC, history.id DESC
            """)
    List<EnrollmentStatusHistory> findEffectiveAt(
            @Param("enrollmentId") Long enrollmentId,
            @Param("date") LocalDate date
    );

    @Query("""
            SELECT COUNT(history) > 0
            FROM EnrollmentStatusHistory history
            WHERE history.enrollment.id = :enrollmentId
              AND history.status = com.englishcenter.enrollment.EnrollmentStatus.ACTIVE
              AND history.effectiveFrom <= :date
              AND (history.effectiveTo IS NULL OR :date < history.effectiveTo)
            """)
    boolean isActiveAt(
            @Param("enrollmentId") Long enrollmentId,
            @Param("date") LocalDate date
    );

    default Optional<EnrollmentStatusHistory> latestForUpdate(Long enrollmentId) {
        return findLatestForUpdate(enrollmentId).stream().findFirst();
    }
}
