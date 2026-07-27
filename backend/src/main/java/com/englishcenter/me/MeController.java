package com.englishcenter.me;

import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.me.dto.StudentAttendanceItemResponse;
import com.englishcenter.me.dto.StudentClassItemResponse;
import com.englishcenter.me.dto.StudentDashboardResponse;
import com.englishcenter.me.dto.StudentScheduleItemResponse;
import com.englishcenter.me.dto.TeacherDashboardResponse;
import com.englishcenter.me.dto.TeacherStudentItemResponse;
import com.englishcenter.payment.dto.PaymentResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class MeController {
    private final MeService meService;

    public MeController(MeService meService) {
        this.meService = meService;
    }

    @GetMapping("/profile")
    @PreAuthorize("hasAnyRole('STUDENT', 'TEACHER')")
    public ApiResponse<Object> profile() {
        return ApiResponse.success(meService.profile());
    }

    @GetMapping("/classes")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StudentClassItemResponse>> classes() {
        return ApiResponse.success(meService.myClasses());
    }

    @GetMapping("/schedule")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StudentScheduleItemResponse>> schedule() {
        return ApiResponse.success(meService.mySchedule());
    }

    @GetMapping("/attendance")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<StudentAttendanceItemResponse>> attendance() {
        return ApiResponse.success(meService.myAttendance());
    }

    @GetMapping("/progress")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<EnrollmentResponse>> progress() {
        return ApiResponse.success(meService.myProgress());
    }

    @GetMapping("/invoices")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<InvoiceResponse>> invoices() {
        return ApiResponse.success(meService.myInvoices());
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<PaymentResponse>> payments() {
        return ApiResponse.success(meService.myPayments());
    }

    @GetMapping("/debt")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<List<InvoiceResponse>> debt() {
        return ApiResponse.success(meService.myDebt());
    }

    @GetMapping("/student-dashboard")
    @PreAuthorize("hasRole('STUDENT')")
    public ApiResponse<StudentDashboardResponse> studentDashboard() {
        return ApiResponse.success(meService.studentDashboard());
    }

    @GetMapping("/classrooms")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<ClassroomResponse>> classrooms() {
        return ApiResponse.success(meService.teacherClassrooms());
    }

    @GetMapping("/classrooms/{id}")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<ClassroomResponse> classroom(@PathVariable Long id) {
        return ApiResponse.success(meService.teacherClassroom(id));
    }

    @GetMapping("/sessions")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<ClassSessionResponse>> sessions() {
        return ApiResponse.success(meService.teacherSessions());
    }

    @GetMapping("/students")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<List<TeacherStudentItemResponse>> students() {
        return ApiResponse.success(meService.teacherStudentItems());
    }

    @GetMapping("/teacher-dashboard")
    @PreAuthorize("hasRole('TEACHER')")
    public ApiResponse<TeacherDashboardResponse> teacherDashboard() {
        return ApiResponse.success(meService.teacherDashboard());
    }
}
