package com.englishcenter.attendance;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
    Optional<Attendance> findBySessionIdAndStudentId(Long sessionId, Long studentId);

    List<Attendance> findBySessionId(Long sessionId);

    Page<Attendance> findAllByOrderByMarkedAtDesc(Pageable pageable);
}
