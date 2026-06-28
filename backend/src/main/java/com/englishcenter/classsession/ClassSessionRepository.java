package com.englishcenter.classsession;

import com.englishcenter.classsession.ClassSessionStatus;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}
