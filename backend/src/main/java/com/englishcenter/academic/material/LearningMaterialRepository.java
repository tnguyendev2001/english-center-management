package com.englishcenter.academic.material;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LearningMaterialRepository extends JpaRepository<LearningMaterial, Long> {
    List<LearningMaterial> findByClassroomIdAndActiveTrue(Long classroomId);

    List<LearningMaterial> findByLessonRecordIdAndActiveTrue(Long lessonRecordId);

    List<LearningMaterial> findByAssignmentIdAndActiveTrue(Long assignmentId);

    List<LearningMaterial> findByAssessmentIdAndActiveTrue(Long assessmentId);

    @Query("""
            SELECT material
            FROM LearningMaterial material
            WHERE material.classroomId = :classroomId
              AND material.active = TRUE
              AND (:visibility IS NULL OR material.visibility = :visibility)
            ORDER BY material.uploadedAt DESC, material.id DESC
            """)
    List<LearningMaterial> findActiveByClassroomId(
            @Param("classroomId") Long classroomId,
            @Param("visibility") MaterialVisibility visibility
    );
}
