package com.englishcenter.academic.report;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentProgressReportRepository extends JpaRepository<StudentProgressReport, Long> {
    Optional<StudentProgressReport> findByEvaluationId(Long evaluationId);

    List<StudentProgressReport> findByStudentIdOrderByGeneratedAtDescIdDesc(Long studentId);

    Optional<StudentProgressReport> findByIdAndStudentId(Long id, Long studentId);

    @Query("""
            SELECT report
            FROM StudentProgressReport report
            WHERE (:classroomId IS NULL OR report.classroomId = :classroomId)
              AND (:studentId IS NULL OR report.studentId = :studentId)
              AND (:periodId IS NULL OR report.evaluationPeriodId = :periodId)
              AND (:status IS NULL OR report.status = :status)
            ORDER BY report.generatedAt DESC, report.id DESC
            """)
    Page<StudentProgressReport> search(
            @Param("classroomId") Long classroomId,
            @Param("studentId") Long studentId,
            @Param("periodId") Long periodId,
            @Param("status") ProgressReportStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(report)
            FROM StudentProgressReport report
            WHERE report.studentId = :studentId
              AND report.status IN :statuses
              AND report.publishedAt IS NOT NULL
              AND report.publishedAt >= :since
            """)
    long countPublishedSinceForStudent(
            @Param("studentId") Long studentId,
            @Param("statuses") java.util.Collection<ProgressReportStatus> statuses,
            @Param("since") java.time.LocalDateTime since
    );
}
