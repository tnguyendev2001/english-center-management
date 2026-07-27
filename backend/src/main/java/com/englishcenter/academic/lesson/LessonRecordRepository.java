package com.englishcenter.academic.lesson;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonRecordRepository extends JpaRepository<LessonRecord, Long> {
    Optional<LessonRecord> findByClassSessionId(Long classSessionId);

    boolean existsByClassSessionId(Long classSessionId);

    @Query("""
            SELECT lesson
            FROM LessonRecord lesson, com.englishcenter.classsession.ClassSession session
            WHERE lesson.classSessionId = session.id
              AND session.classroom.id = :classroomId
              AND (:status IS NULL OR lesson.status = :status)
            ORDER BY session.sessionDate DESC, session.sessionNo DESC, lesson.id DESC
            """)
    List<LessonRecord> findByClassroomId(
            @Param("classroomId") Long classroomId,
            @Param("status") LessonStatus status
    );

    @Query("""
            SELECT lesson
            FROM LessonRecord lesson, com.englishcenter.classsession.ClassSession session
            WHERE lesson.classSessionId = session.id
              AND session.classroom.id = :classroomId
              AND (:status IS NULL OR lesson.status = :status)
            ORDER BY session.sessionDate DESC, session.sessionNo DESC, lesson.id DESC
            """)
    Page<LessonRecord> findByClassroomId(
            @Param("classroomId") Long classroomId,
            @Param("status") LessonStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT lesson
            FROM LessonRecord lesson, com.englishcenter.classsession.ClassSession session
            WHERE lesson.classSessionId = session.id
              AND (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:status IS NULL OR lesson.status = :status)
              AND (:teacherId IS NULL OR session.classroom.teacherId = :teacherId)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(lesson.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY session.sessionDate DESC, session.sessionNo DESC, lesson.id DESC
            """)
    Page<LessonRecord> search(
            @Param("classroomId") Long classroomId,
            @Param("status") LessonStatus status,
            @Param("keyword") String keyword,
            @Param("teacherId") Long teacherId,
            Pageable pageable
    );

    @Query("""
            SELECT lesson
            FROM LessonRecord lesson, com.englishcenter.classsession.ClassSession session
            WHERE lesson.classSessionId = session.id
              AND lesson.status IN :statuses
              AND EXISTS (
                  SELECT 1
                  FROM com.englishcenter.enrollment.Enrollment enrollment
                  WHERE enrollment.classroom.id = session.classroom.id
                    AND enrollment.student.id = :studentId
              )
            ORDER BY session.sessionDate DESC, session.sessionNo DESC, lesson.id DESC
            """)
    List<LessonRecord> findPublishedForStudent(
            @Param("studentId") Long studentId,
            @Param("statuses") List<LessonStatus> statuses
    );
}
