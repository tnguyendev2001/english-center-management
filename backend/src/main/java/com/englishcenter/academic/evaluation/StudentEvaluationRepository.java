package com.englishcenter.academic.evaluation;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentEvaluationRepository extends JpaRepository<StudentEvaluation, Long> {
    Optional<StudentEvaluation> findByEvaluationPeriodIdAndClassroomIdAndStudentId(
            Long evaluationPeriodId,
            Long classroomId,
            Long studentId
    );

    List<StudentEvaluation> findByEvaluationPeriodIdAndClassroomIdOrderByStudentIdAsc(
            Long evaluationPeriodId,
            Long classroomId
    );

    List<StudentEvaluation> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);

    long countByStatus(StudentEvaluationStatus status);

    long countByClassroomIdAndStatus(Long classroomId, StudentEvaluationStatus status);

    @Query("""
            SELECT COUNT(evaluation)
            FROM StudentEvaluation evaluation
            WHERE evaluation.teacherId = :teacherId
              AND evaluation.status = :status
            """)
    long countByTeacherIdAndStatus(
            @Param("teacherId") Long teacherId,
            @Param("status") StudentEvaluationStatus status
    );

    @Query("""
            SELECT COUNT(evaluation)
            FROM StudentEvaluation evaluation, EvaluationPeriod period
            WHERE evaluation.evaluationPeriodId = period.id
              AND evaluation.teacherId = :teacherId
              AND evaluation.status = :status
              AND period.status = com.englishcenter.academic.evaluation.EvaluationPeriodStatus.OPEN
              AND period.endDate >= :fromDate
              AND period.endDate <= :toDate
            """)
    long countDraftNearPeriodEndByTeacher(
            @Param("teacherId") Long teacherId,
            @Param("status") StudentEvaluationStatus status,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate
    );
}
