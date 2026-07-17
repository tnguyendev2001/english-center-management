package com.englishcenter.attendance.mapper;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.dto.AttendanceResponse;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {
    public AttendanceResponse toResponse(Attendance attendance) {
        return new AttendanceResponse(
                attendance.getId(),
                attendance.getSession().getId(),
                attendance.getStudent().getId(),
                attendance.getStudent().getStudentCode(),
                attendance.getStudent().getFullName(),
                attendance.getStatus(),
                attendance.getNote(),
                attendance.getMarkedAt(),
                Boolean.TRUE.equals(attendance.getValid()),
                attendance.getVoidReason(),
                attendance.getVoidedAt()
        );
    }
}
