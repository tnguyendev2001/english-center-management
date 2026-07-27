package com.englishcenter.academic.score;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentScoreRepository extends JpaRepository<AssessmentScore, Long> {
    Optional<AssessmentScore> findByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    List<AssessmentScore> findByAssessmentIdOrderByStudentIdAsc(Long assessmentId);

    List<AssessmentScore> findByStudentIdOrderByCreatedAtDescIdDesc(Long studentId);

    boolean existsByAssessmentIdAndStudentId(Long assessmentId, Long studentId);

    List<AssessmentScore> findByAssessmentIdIn(Collection<Long> assessmentIds);

    List<AssessmentScore> findByStudentIdAndAssessmentIdIn(Long studentId, Collection<Long> assessmentIds);

    @Query("""
            SELECT COUNT(score)
            FROM AssessmentScore score
            WHERE score.studentId = :studentId
              AND score.publishedAt IS NOT NULL
              AND score.publishedAt >= :since
            """)
    long countPublishedSinceForStudent(
            @Param("studentId") Long studentId,
            @Param("since") LocalDateTime since
    );
}
