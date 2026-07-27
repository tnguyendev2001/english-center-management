package com.englishcenter.academic.assignment;

import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
    List<Assignment> findByClassroomIdOrderByAssignedDateDescIdDesc(Long classroomId);

    List<Assignment> findByClassroomIdAndStatusIn(Long classroomId, Collection<AssignmentStatus> statuses);

    long countByStatus(AssignmentStatus status);

    long countByClassroomIdAndStatus(Long classroomId, AssignmentStatus status);

    @Query("""
            SELECT assignment
            FROM Assignment assignment
            WHERE (:classroomId IS NULL OR assignment.classroomId = :classroomId)
              AND (:status IS NULL OR assignment.status = :status)
              AND (:teacherId IS NULL OR assignment.createdByTeacherId = :teacherId)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(assignment.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY assignment.assignedDate DESC, assignment.id DESC
            """)
    Page<Assignment> search(
            @Param("classroomId") Long classroomId,
            @Param("status") AssignmentStatus status,
            @Param("teacherId") Long teacherId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    @Query("""
            SELECT assignment
            FROM Assignment assignment
            WHERE assignment.classroomId = :classroomId
              AND (:status IS NULL OR assignment.status = :status)
              AND (
                  assignment.targetMode = com.englishcenter.academic.assignment.AssignmentTargetMode.ENTIRE_CLASS
                  OR EXISTS (
                      SELECT 1
                      FROM AssignmentTarget target
                      WHERE target.assignmentId = assignment.id
                        AND target.studentId = :studentId
                  )
              )
            ORDER BY assignment.dueDate ASC, assignment.assignedDate DESC, assignment.id DESC
            """)
    List<Assignment> findEligibleForStudent(
            @Param("classroomId") Long classroomId,
            @Param("studentId") Long studentId,
            @Param("status") AssignmentStatus status
    );

    @Query("""
            SELECT COUNT(assignment)
            FROM Assignment assignment
            WHERE assignment.status = :status
              AND assignment.allowSubmission = TRUE
              AND NOT EXISTS (
                  SELECT 1
                  FROM com.englishcenter.academic.submission.AssignmentSubmission submission
                  WHERE submission.assignmentId = assignment.id
              )
            """)
    long countPublishedWithoutSubmissions(@Param("status") AssignmentStatus status);

    @Query("""
            SELECT COUNT(assignment)
            FROM Assignment assignment
            WHERE assignment.classroomId = :classroomId
              AND assignment.status IN :statuses
              AND assignment.allowSubmission = TRUE
              AND NOT EXISTS (
                  SELECT 1
                  FROM com.englishcenter.academic.submission.AssignmentSubmission submission
                  WHERE submission.assignmentId = assignment.id
                    AND submission.studentId = :studentId
              )
            """)
    long countEligibleWithoutSubmissionByStudent(
            @Param("classroomId") Long classroomId,
            @Param("studentId") Long studentId,
            @Param("statuses") Collection<AssignmentStatus> statuses
    );

    @Query("""
            SELECT COUNT(assignment)
            FROM Assignment assignment
            WHERE assignment.status = com.englishcenter.academic.assignment.AssignmentStatus.PUBLISHED
              AND assignment.allowSubmission = TRUE
              AND assignment.dueDate IS NOT NULL
              AND assignment.dueDate >= :fromDate
              AND assignment.dueDate <= :toDate
              AND (
                  assignment.targetMode = com.englishcenter.academic.assignment.AssignmentTargetMode.ENTIRE_CLASS
                  OR EXISTS (
                      SELECT 1 FROM AssignmentTarget target
                      WHERE target.assignmentId = assignment.id AND target.studentId = :studentId
                  )
              )
              AND EXISTS (
                  SELECT 1 FROM com.englishcenter.enrollment.Enrollment enrollment
                  WHERE enrollment.student.id = :studentId
                    AND enrollment.classroom.id = assignment.classroomId
              )
              AND NOT EXISTS (
                  SELECT 1 FROM com.englishcenter.academic.submission.AssignmentSubmission submission
                  WHERE submission.assignmentId = assignment.id AND submission.studentId = :studentId
              )
            """)
    long countDueSoonWithoutSubmissionForStudent(
            @Param("studentId") Long studentId,
            @Param("fromDate") java.time.LocalDate fromDate,
            @Param("toDate") java.time.LocalDate toDate
    );

    @Query("""
            SELECT COUNT(assignment)
            FROM Assignment assignment
            WHERE assignment.status = com.englishcenter.academic.assignment.AssignmentStatus.PUBLISHED
              AND assignment.allowSubmission = TRUE
              AND assignment.dueDate IS NOT NULL
              AND assignment.dueDate < :today
              AND (
                  assignment.targetMode = com.englishcenter.academic.assignment.AssignmentTargetMode.ENTIRE_CLASS
                  OR EXISTS (
                      SELECT 1 FROM AssignmentTarget target
                      WHERE target.assignmentId = assignment.id AND target.studentId = :studentId
                  )
              )
              AND EXISTS (
                  SELECT 1 FROM com.englishcenter.enrollment.Enrollment enrollment
                  WHERE enrollment.student.id = :studentId
                    AND enrollment.classroom.id = assignment.classroomId
              )
              AND NOT EXISTS (
                  SELECT 1 FROM com.englishcenter.academic.submission.AssignmentSubmission submission
                  WHERE submission.assignmentId = assignment.id AND submission.studentId = :studentId
              )
            """)
    long countOverdueWithoutSubmissionForStudent(
            @Param("studentId") Long studentId,
            @Param("today") java.time.LocalDate today
    );
}
