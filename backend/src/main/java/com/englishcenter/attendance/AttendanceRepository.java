package com.englishcenter.attendance;

import com.englishcenter.attendance.AttendanceStatus;
import com.englishcenter.classsession.ClassSessionStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Attendance> findBySessionId(Long sessionId);

    Page<Attendance> findAllByOrderByMarkedAtDesc(Pageable pageable);

    boolean existsBySessionId(Long sessionId);

    @Query("""
            SELECT COUNT(a)
            FROM Attendance a
            JOIN a.session s
            WHERE a.student.id = :studentId
              AND s.classroom.id = :classroomId
              AND s.status <> :canceledStatus
              AND a.valid = true
              AND a.status IN (com.englishcenter.attendance.AttendanceStatus.PRESENT,
                               com.englishcenter.attendance.AttendanceStatus.ABSENT)
              AND s.sessionDate >= :startDate
              AND (:endDate IS NULL OR s.sessionDate <= :endDate)
            """)
    long countUsedSessions(
            @Param("studentId") Long studentId,
            @Param("classroomId") Long classroomId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("canceledStatus") ClassSessionStatus canceledStatus
    );

    @Query("""
            SELECT attendance
            FROM Attendance attendance
            JOIN attendance.session session
            JOIN attendance.student student
            JOIN session.classroom classroom
            WHERE session.status <> com.englishcenter.classsession.ClassSessionStatus.CANCELED
              AND attendance.valid = true
              AND (:classroomId IS NULL OR classroom.id = :classroomId)
              AND (:sessionDate IS NULL OR session.sessionDate = :sessionDate)
              AND (:status IS NULL OR attendance.status = :status)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            ORDER BY session.sessionDate DESC, student.fullName ASC
            """)
    Page<Attendance> searchReport(
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("status") AttendanceStatus status,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(attendance)
            FROM Attendance attendance
            JOIN attendance.session session
            JOIN attendance.student student
            WHERE session.status <> com.englishcenter.classsession.ClassSessionStatus.CANCELED
              AND attendance.valid = true
              AND (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:sessionDate IS NULL OR session.sessionDate = :sessionDate)
              AND (:status IS NULL OR attendance.status = :status)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    long countReportFiltered(
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword,
            @Param("sessionDate") LocalDate sessionDate,
            @Param("status") AttendanceStatus status
    );

    @Query("""
            SELECT COUNT(attendance)
            FROM Attendance attendance
            JOIN attendance.session session
            JOIN attendance.student student
            WHERE session.status <> com.englishcenter.classsession.ClassSessionStatus.CANCELED
              AND attendance.valid = true
              AND attendance.status = :attendanceStatus
              AND (:classroomId IS NULL OR session.classroom.id = :classroomId)
              AND (:sessionDate IS NULL OR session.sessionDate = :sessionDate)
              AND (
                  :keyword IS NULL OR :keyword = ''
                  OR LOWER(student.studentCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  OR LOWER(student.phone) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    long countReportByStatus(
            @Param("attendanceStatus") AttendanceStatus attendanceStatus,
            @Param("classroomId") Long classroomId,
            @Param("keyword") String keyword,
            @Param("sessionDate") LocalDate sessionDate
    );
}
