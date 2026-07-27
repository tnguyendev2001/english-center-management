package com.englishcenter.academic.evaluation;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EvaluationPeriodRepository extends JpaRepository<EvaluationPeriod, Long> {
    @Query("""
            SELECT period
            FROM EvaluationPeriod period
            WHERE period.status = com.englishcenter.academic.evaluation.EvaluationPeriodStatus.OPEN
              AND (period.classroomId IS NULL OR period.classroomId = :classroomId)
            ORDER BY period.startDate DESC, period.id DESC
            """)
    List<EvaluationPeriod> findOpenPeriodsForClassroom(@Param("classroomId") Long classroomId);

    @Query("""
            SELECT period
            FROM EvaluationPeriod period
            WHERE (:classroomId IS NULL OR period.classroomId = :classroomId)
              AND (:status IS NULL OR period.status = :status)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(period.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY period.startDate DESC, period.id DESC
            """)
    Page<EvaluationPeriod> search(
            @Param("classroomId") Long classroomId,
            @Param("status") EvaluationPeriodStatus status,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(period)
            FROM EvaluationPeriod period
            WHERE period.status = com.englishcenter.academic.evaluation.EvaluationPeriodStatus.OPEN
              AND period.endDate >= :fromDate
              AND period.endDate <= :toDate
            """)
    long countOpenEndingBetween(
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate
    );
}
