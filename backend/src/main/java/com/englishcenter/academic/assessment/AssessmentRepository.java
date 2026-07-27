package com.englishcenter.academic.assessment;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssessmentRepository extends JpaRepository<Assessment, Long> {
    List<Assessment> findByClassroomIdOrderByAssessmentDateDescIdDesc(Long classroomId);

    List<Assessment> findByClassroomIdAndStatusNot(Long classroomId, AssessmentStatus status);

    @Query("""
            SELECT assessment
            FROM Assessment assessment
            WHERE (:classroomId IS NULL OR assessment.classroomId = :classroomId)
              AND (:status IS NULL OR assessment.status = :status)
              AND (:periodId IS NULL OR assessment.evaluationPeriodId = :periodId)
            ORDER BY assessment.assessmentDate DESC, assessment.id DESC
            """)
    List<Assessment> findFiltered(
            @Param("classroomId") Long classroomId,
            @Param("status") AssessmentStatus status,
            @Param("periodId") Long periodId
    );

    @Query("""
            SELECT assessment
            FROM Assessment assessment
            WHERE (:classroomId IS NULL OR assessment.classroomId = :classroomId)
              AND (:status IS NULL OR assessment.status = :status)
              AND (:periodId IS NULL OR assessment.evaluationPeriodId = :periodId)
            ORDER BY assessment.assessmentDate DESC, assessment.id DESC
            """)
    Page<Assessment> search(
            @Param("classroomId") Long classroomId,
            @Param("status") AssessmentStatus status,
            @Param("periodId") Long periodId,
            Pageable pageable
    );

    @Query("""
            SELECT assessment
            FROM Assessment assessment, com.englishcenter.classroom.Classroom classroom
            WHERE assessment.classroomId = classroom.id
              AND classroom.teacherId = :teacherId
              AND (:status IS NULL OR assessment.status = :status)
              AND (:periodId IS NULL OR assessment.evaluationPeriodId = :periodId)
            ORDER BY assessment.assessmentDate DESC, assessment.id DESC
            """)
    List<Assessment> findByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("status") AssessmentStatus status,
            @Param("periodId") Long periodId
    );

    @Query("""
            SELECT assessment
            FROM Assessment assessment, com.englishcenter.classroom.Classroom classroom
            WHERE assessment.classroomId = classroom.id
              AND classroom.teacherId = :teacherId
              AND (:status IS NULL OR assessment.status = :status)
              AND (:periodId IS NULL OR assessment.evaluationPeriodId = :periodId)
            ORDER BY assessment.assessmentDate DESC, assessment.id DESC
            """)
    Page<Assessment> findByTeacherId(
            @Param("teacherId") Long teacherId,
            @Param("status") AssessmentStatus status,
            @Param("periodId") Long periodId,
            Pageable pageable
    );

    long countByStatus(AssessmentStatus status);

    @Query("""
            SELECT COUNT(assessment)
            FROM Assessment assessment, com.englishcenter.classroom.Classroom classroom
            WHERE assessment.classroomId = classroom.id
              AND classroom.teacherId = :teacherId
              AND assessment.status = :status
            """)
    long countByTeacherIdAndStatus(
            @Param("teacherId") Long teacherId,
            @Param("status") AssessmentStatus status
    );
}
