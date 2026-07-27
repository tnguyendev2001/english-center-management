package com.englishcenter.academic.assignment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentTargetRepository extends JpaRepository<AssignmentTarget, Long> {
    List<AssignmentTarget> findByAssignmentId(Long assignmentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    void deleteByAssignmentId(Long assignmentId);

    List<AssignmentTarget> findByStudentIdAndAssignmentIdIn(Long studentId, Collection<Long> assignmentIds);

    @Query("""
            SELECT target.studentId
            FROM AssignmentTarget target
            WHERE target.assignmentId = :assignmentId
            ORDER BY target.studentId ASC
            """)
    List<Long> findStudentIdsByAssignmentId(@Param("assignmentId") Long assignmentId);
}
