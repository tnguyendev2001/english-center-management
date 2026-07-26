package com.englishcenter.classsession;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClassSessionRepository extends JpaRepository<ClassSession, Long> {
    Page<ClassSession> findAllByOrderBySessionDateAscStartTimeAsc(Pageable pageable);

    Page<ClassSession> findByClassroomIdOrderBySessionDateAscStartTimeAsc(Long classroomId, Pageable pageable);

    List<ClassSession> findBySessionDateOrderByStartTimeAsc(LocalDate sessionDate);

    int countBySessionDateAndStatusNot(LocalDate sessionDate, ClassSessionStatus status);

    int countBySessionDateBetweenAndStatus(LocalDate fromDate, LocalDate toDate, ClassSessionStatus status);

    int countByClassroomIdAndSessionDateGreaterThanEqualAndStatusNot(
            Long classroomId,
            LocalDate fromDate,
            ClassSessionStatus status
    );

    int countByClassroomId(Long classroomId);

    boolean existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
            Long classroomId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime
    );

    boolean existsByClassroomIdAndSessionDateAndStatusNot(
            Long classroomId,
            LocalDate sessionDate,
            ClassSessionStatus status
    );

    List<ClassSession> findByClassroomIdOrderBySessionDateAscStartTimeAsc(Long classroomId);

    List<ClassSession> findByClassroomIdAndSessionDateBetweenAndStatusNotOrderBySessionDateAscStartTimeAsc(
            Long classroomId,
            LocalDate fromDate,
            LocalDate toDate,
            ClassSessionStatus status
    );

    int countByClassroomIdAndSessionDateBetween(
            Long classroomId,
            LocalDate fromDate,
            LocalDate toDate
    );

    @Query("""
            SELECT session
            FROM ClassSession session
            WHERE (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:fromDate IS NULL OR session.sessionDate >= :fromDate)
              AND (:toDate IS NULL OR session.sessionDate <= :toDate)
              AND (:status IS NULL OR session.status = :status)
            """)
    Page<ClassSession> search(
            @Param("classroomId") Long classroomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") ClassSessionStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT session
            FROM ClassSession session
            WHERE session.classroom.id = :classroomId
              AND session.sessionDate = :sessionDate
            ORDER BY session.startTime ASC, session.id ASC
            """)
    List<ClassSession> findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(
            @Param("classroomId") Long classroomId,
            @Param("sessionDate") LocalDate sessionDate
    );

    @Query("""
            SELECT session
            FROM ClassSession session
            WHERE session.classroom.id = :classroomId
              AND session.sessionDate > :today
              AND session.status <> com.englishcenter.classsession.ClassSessionStatus.CANCELED
            ORDER BY session.sessionDate ASC, session.startTime ASC, session.id ASC
            """)
    List<ClassSession> findNextSessions(
            @Param("classroomId") Long classroomId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
            SELECT session
            FROM ClassSession session
            WHERE session.classroom.id = :classroomId
              AND session.sessionDate < :today
              AND session.status <> com.englishcenter.classsession.ClassSessionStatus.CANCELED
            ORDER BY session.sessionDate DESC, session.startTime DESC, session.id DESC
            """)
    List<ClassSession> findLatestPastSessions(
            @Param("classroomId") Long classroomId,
            @Param("today") LocalDate today,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(session)
            FROM ClassSession session
            WHERE (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:fromDate IS NULL OR session.sessionDate >= :fromDate)
              AND (:toDate IS NULL OR session.sessionDate <= :toDate)
              AND (:status IS NULL OR session.status = :status)
              AND (
                  session.sessionDate < :sessionDate
                  OR (session.sessionDate = :sessionDate AND session.startTime < :startTime)
                  OR (session.sessionDate = :sessionDate AND session.startTime = :startTime AND session.id < :sessionId)
              )
            """)
    long countSessionsBeforeAscending(
            @Param("classroomId") Long classroomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") ClassSessionStatus status,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("sessionId") Long sessionId
    );

    @Query("""
            SELECT COUNT(session)
            FROM ClassSession session
            WHERE (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:fromDate IS NULL OR session.sessionDate >= :fromDate)
              AND (:toDate IS NULL OR session.sessionDate <= :toDate)
              AND (:status IS NULL OR session.status = :status)
              AND (
                  session.sessionDate > :sessionDate
                  OR (session.sessionDate = :sessionDate AND session.startTime > :startTime)
                  OR (session.sessionDate = :sessionDate AND session.startTime = :startTime AND session.id > :sessionId)
              )
            """)
    long countSessionsBeforeDescending(
            @Param("classroomId") Long classroomId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("status") ClassSessionStatus status,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("startTime") LocalTime startTime,
            @Param("sessionId") Long sessionId
    );

    @Query("""
            SELECT session
            FROM ClassSession session
            JOIN FETCH session.classroom
            WHERE session.id = :id
            """)
    Optional<ClassSession> findByIdWithClassroom(@Param("id") Long id);
}
