package com.englishcenter.classsession;

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

    int countByClassroomId(Long classroomId);

    boolean existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
            Long classroomId,
            LocalDate sessionDate,
            LocalTime startTime,
            LocalTime endTime
    );
}
