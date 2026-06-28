package com.englishcenter.attendance;

import com.englishcenter.attendance.dto.AttendanceResponse;
import com.englishcenter.attendance.dto.AttendanceReadinessRequest;
import com.englishcenter.attendance.dto.AttendanceReadinessResponse;
import com.englishcenter.attendance.dto.MarkAttendanceRequest;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AttendanceController {
    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @PostMapping("/api/attendance/mark")
    public ApiResponse<List<AttendanceResponse>> mark(@Valid @RequestBody MarkAttendanceRequest request) {
        return ApiResponse.success(attendanceService.mark(request));
    }

    @PostMapping("/api/attendance/readiness")
    public ApiResponse<AttendanceReadinessResponse> checkReadiness(
            @Valid @RequestBody AttendanceReadinessRequest request
    ) {
        return ApiResponse.success(attendanceService.checkReadiness(request.sessionId()));
    }

    @GetMapping("/api/attendance")
    public ApiResponse<List<AttendanceResponse>> getAttendance(
            @RequestParam(required = false) Long sessionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<AttendanceResponse> attendance = attendanceService.getAttendance(sessionId, page, size);
        PageMeta meta = new PageMeta(
                attendance.getNumber(),
                attendance.getSize(),
                attendance.getTotalElements(),
                attendance.getTotalPages()
        );
        return ApiResponse.success(attendance.getContent(), meta);
    }
}
