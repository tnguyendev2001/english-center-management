package com.englishcenter.attendance;

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
}
