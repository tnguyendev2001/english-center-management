package com.englishcenter.academic.submission;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentSubmissionRepository extends JpaRepository<AssignmentSubmission, Long> {
    Optional<AssignmentSubmission> findByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    List<AssignmentSubmission> findByAssignmentIdOrderBySubmittedAtDescIdDesc(Long assignmentId);

    boolean existsByAssignmentIdAndStudentId(Long assignmentId, Long studentId);

    long countByAssignmentIdAndStatusIn(Long assignmentId, Collection<SubmissionStatus> statuses);

    List<AssignmentSubmission> findByStudentIdOrderBySubmittedAtDescIdDesc(Long studentId);

    List<AssignmentSubmission> findByStudentIdAndAssignmentIdIn(Long studentId, Collection<Long> assignmentIds);

    long countByStatusIn(Collection<SubmissionStatus> statuses);

    @Query("""
            SELECT COUNT(submission)
            FROM AssignmentSubmission submission,
                 com.englishcenter.academic.assignment.Assignment assignment,
                 com.englishcenter.classroom.Classroom classroom
            WHERE submission.assignmentId = assignment.id
              AND assignment.classroomId = classroom.id
              AND classroom.teacherId = :teacherId
              AND submission.status IN :statuses
            """)
    long countByTeacherIdAndStatusIn(
            @Param("teacherId") Long teacherId,
            @Param("statuses") Collection<SubmissionStatus> statuses
    );
}
