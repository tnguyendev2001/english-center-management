package com.englishcenter.me;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomService;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.enrollment.dto.EnrollmentResponse;
import com.englishcenter.enrollment.mapper.EnrollmentMapper;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.dto.InvoiceResponse;
import com.englishcenter.invoice.mapper.InvoiceMapper;
import com.englishcenter.me.dto.StudentAttendanceItemResponse;
import com.englishcenter.me.dto.StudentClassItemResponse;
import com.englishcenter.me.dto.StudentDashboardResponse;
import com.englishcenter.me.dto.StudentScheduleItemResponse;
import com.englishcenter.me.dto.TeacherDashboardResponse;
import com.englishcenter.me.dto.TeacherStudentItemResponse;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.dto.PaymentResponse;
import com.englishcenter.payment.mapper.PaymentMapper;
import com.englishcenter.security.CurrentUserService;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.StudentService;
import com.englishcenter.teacher.TeacherService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MeService {
    private final StudentService studentService;
    private final TeacherService teacherService;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentMapper enrollmentMapper;
    private final EnrollmentSessionService enrollmentSessionService;
    private final ClassroomService classroomService;
    private final ClassSessionRepository classSessionRepository;
    private final ClassSessionMapper classSessionMapper;
    private final AttendanceRepository attendanceRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceMapper invoiceMapper;
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final CurrentUserService currentUserService;

    public MeService(
            StudentService studentService,
            TeacherService teacherService,
            EnrollmentRepository enrollmentRepository,
            EnrollmentMapper enrollmentMapper,
            EnrollmentSessionService enrollmentSessionService,
            ClassroomService classroomService,
            ClassSessionRepository classSessionRepository,
            ClassSessionMapper classSessionMapper,
            AttendanceRepository attendanceRepository,
            InvoiceRepository invoiceRepository,
            InvoiceMapper invoiceMapper,
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            CurrentUserService currentUserService
    ) {
        this.studentService = studentService;
        this.teacherService = teacherService;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentMapper = enrollmentMapper;
        this.enrollmentSessionService = enrollmentSessionService;
        this.classroomService = classroomService;
        this.classSessionRepository = classSessionRepository;
        this.classSessionMapper = classSessionMapper;
        this.attendanceRepository = attendanceRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceMapper = invoiceMapper;
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.currentUserService = currentUserService;
    }

    @Transactional(readOnly = true)
    public Object profile() {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.STUDENT) {
            return studentService.getById(currentUserService.requireCurrentStudentId());
        }
        if (principal.role() == AccountRole.TEACHER) {
            return teacherService.getById(currentUserService.requireCurrentTeacherId());
        }
        throw new BusinessException("Hồ sơ cá nhân chỉ hỗ trợ học viên và giáo viên.");
    }

    @Transactional(readOnly = true)
    public List<StudentClassItemResponse> myClasses() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId).stream()
                .map(this::toStudentClassItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentScheduleItemResponse> mySchedule() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return classSessionRepository.findByEnrolledStudentId(studentId).stream()
                .map(this::toStudentScheduleItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StudentAttendanceItemResponse> myAttendance() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return attendanceRepository.findByStudentIdOrderByMarkedAtDesc(studentId, PageRequest.of(0, 200))
                .stream()
                .map(this::toStudentAttendanceItem)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<EnrollmentResponse> myProgress() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId).stream()
                .map(enrollmentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDashboardResponse studentDashboard() {
        Long studentId = currentUserService.requireCurrentStudentId();
        LocalDate today = LocalDate.now();

        List<Enrollment> enrollments = enrollmentRepository.findByStudentIdOrderByStartDateDescIdDesc(studentId);
        List<Enrollment> activeEnrollments = enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .toList();

        int usedSessions = activeEnrollments.stream()
                .mapToInt(enrollment -> enrollment.getUsedSessions() == null ? 0 : enrollment.getUsedSessions())
                .sum();
        int remainingSessions = activeEnrollments.stream()
                .mapToInt(enrollmentSessionService::remainingSessions)
                .sum();

        List<StudentScheduleItemResponse> schedule = classSessionRepository.findByEnrolledStudentId(studentId).stream()
                .map(this::toStudentScheduleItem)
                .toList();
        List<StudentScheduleItemResponse> upcomingSessions = schedule.stream()
                .filter(session -> !session.sessionDate().isBefore(today))
                .filter(session -> session.status() != ClassSessionStatus.CANCELED)
                .sorted(Comparator
                        .comparing(StudentScheduleItemResponse::sessionDate)
                        .thenComparing(StudentScheduleItemResponse::startTime))
                .limit(10)
                .toList();
        StudentScheduleItemResponse nextSession = upcomingSessions.isEmpty() ? null : upcomingSessions.getFirst();

        List<InvoiceResponse> attentionInvoices = myDebt();
        BigDecimal totalDebt = attentionInvoices.stream()
                .map(InvoiceResponse::remainingAmount)
                .filter(amount -> amount != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<StudentAttendanceItemResponse> recentAttendance = myAttendance().stream()
                .limit(10)
                .toList();

        List<EnrollmentResponse> progressItems = activeEnrollments.stream()
                .map(enrollmentMapper::toResponse)
                .toList();

        return new StudentDashboardResponse(
                activeEnrollments.size(),
                nextSession,
                usedSessions,
                remainingSessions,
                totalDebt,
                upcomingSessions,
                progressItems,
                recentAttendance,
                attentionInvoices
        );
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> myInvoices() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return invoiceRepository.search(
                        null,
                        studentId,
                        null,
                        PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .map(invoiceMapper::toResponse)
                .getContent();
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> myPayments() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return paymentRepository.findByStudentIdOrderByPaymentDateDescCreatedAtDesc(studentId).stream()
                .map(paymentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<InvoiceResponse> myDebt() {
        Long studentId = currentUserService.requireCurrentStudentId();
        return invoiceRepository.findDebtInvoicesByStudentId(studentId).stream()
                .map(invoiceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ClassroomResponse> teacherClassrooms() {
        return classroomService.listByTeacherId(currentUserService.requireCurrentTeacherId());
    }

    @Transactional(readOnly = true)
    public ClassroomResponse teacherClassroom(Long classroomId) {
        Long teacherId = currentUserService.requireCurrentTeacherId();
        ClassroomResponse classroom = classroomService.getById(classroomId);
        if (!teacherId.equals(classroom.teacherId())) {
            throw new NotFoundException("Classroom not found");
        }
        return classroom;
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> teacherSessions() {
        Long teacherId = currentUserService.requireCurrentTeacherId();
        return classSessionRepository.findByClassroomTeacherId(teacherId).stream()
                .map(classSessionMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherStudentItemResponse> teacherStudentItems() {
        Long teacherId = currentUserService.requireCurrentTeacherId();
        LinkedHashMap<Long, TeacherStudentItemResponse> students = new LinkedHashMap<>();

        for (Enrollment enrollment : enrollmentRepository.findByClassroomTeacherId(teacherId)) {
            Long studentId = enrollment.getStudent().getId();
            TeacherStudentItemResponse existing = students.get(studentId);
            List<String> classroomNames = existing == null
                    ? new ArrayList<>()
                    : new ArrayList<>(existing.classroomNames());
            String className = enrollment.getClassroom().getClassName();
            if (!classroomNames.contains(className)) {
                classroomNames.add(className);
            }

            EnrollmentStatus status = existing == null
                    ? enrollment.getStatus()
                    : preferredStatus(existing.learningStatus(), enrollment.getStatus());
            int used = existing == null
                    ? enrollment.getUsedSessions()
                    : Math.max(existing.usedSessions(), enrollment.getUsedSessions());
            int remaining = existing == null
                    ? enrollmentSessionService.remainingSessions(enrollment)
                    : Math.min(existing.remainingSessions(), enrollmentSessionService.remainingSessions(enrollment));

            students.put(studentId, new TeacherStudentItemResponse(
                    studentId,
                    enrollment.getStudent().getStudentCode(),
                    enrollment.getStudent().getFullName(),
                    classroomNames,
                    status,
                    used,
                    remaining,
                    null
            ));
        }
        return List.copyOf(students.values());
    }

    @Transactional(readOnly = true)
    public TeacherDashboardResponse teacherDashboard() {
        Long teacherId = currentUserService.requireCurrentTeacherId();
        LocalDate today = LocalDate.now();

        List<ClassroomResponse> classrooms = classroomService.listByTeacherId(teacherId);
        List<ClassSession> todaySessions = classSessionRepository.findTodayByTeacherId(teacherId, today);
        List<ClassSession> upcoming = classSessionRepository.findUpcomingByTeacherId(teacherId, today).stream()
                .filter(session -> !session.getSessionDate().isEqual(today) || session.getStartTime() != null)
                .limit(10)
                .toList();
        List<ClassSession> incomplete = classSessionRepository.findIncompleteAttendanceByTeacherId(teacherId, today);

        int activeStudents = enrollmentRepository.findByClassroomTeacherId(teacherId).stream()
                .filter(enrollment -> enrollment.getStatus() == EnrollmentStatus.ACTIVE)
                .map(enrollment -> enrollment.getStudent().getId())
                .distinct()
                .mapToInt(id -> 1)
                .sum();

        List<TeacherDashboardResponse.TodayClassItem> todayClasses = todaySessions.stream()
                .map(session -> new TeacherDashboardResponse.TodayClassItem(
                        session.getId(),
                        session.getClassroom().getId(),
                        session.getClassroom().getClassName(),
                        session.getStartTime().toString(),
                        session.getEndTime().toString(),
                        session.getClassroom().getRoom(),
                        (int) enrollmentRepository.countByClassroomIdAndStatus(
                                session.getClassroom().getId(),
                                EnrollmentStatus.ACTIVE
                        )
                ))
                .toList();

        List<TeacherDashboardResponse.ActionItem> actions = incomplete.stream()
                .limit(20)
                .map(session -> new TeacherDashboardResponse.ActionItem(
                        "MISSING_ATTENDANCE",
                        "Chưa hoàn tất điểm danh: " + session.getClassroom().getClassName()
                                + " (" + session.getSessionDate() + ")",
                        session.getId(),
                        session.getClassroom().getId()
                ))
                .toList();

        return new TeacherDashboardResponse(
                classrooms.size(),
                activeStudents,
                todaySessions.size(),
                incomplete.size(),
                todayClasses,
                upcoming.stream().map(classSessionMapper::toResponse).toList(),
                actions,
                classrooms
        );
    }

    private EnrollmentStatus preferredStatus(EnrollmentStatus left, EnrollmentStatus right) {
        if (left == EnrollmentStatus.ACTIVE || right == EnrollmentStatus.ACTIVE) {
            return EnrollmentStatus.ACTIVE;
        }
        return left;
    }

    private StudentClassItemResponse toStudentClassItem(Enrollment enrollment) {
        Classroom classroom = enrollment.getClassroom();
        List<ClassDayOfWeek> days = classroom.getDaysOfWeek() == null
                ? List.of()
                : classroom.getDaysOfWeek().stream().sorted().toList();
        return new StudentClassItemResponse(
                enrollment.getId(),
                classroom.getId(),
                classroom.getClassCode(),
                classroom.getClassName(),
                classroom.getLevel(),
                classroom.getTeacherName(),
                classroom.getRoom(),
                days,
                classroom.getStartTime(),
                classroom.getEndTime(),
                enrollment.getStartDate(),
                enrollment.getStatus(),
                enrollment.getTotalSessions(),
                enrollment.getUsedSessions(),
                enrollmentSessionService.remainingSessions(enrollment)
        );
    }

    private StudentScheduleItemResponse toStudentScheduleItem(ClassSession session) {
        return new StudentScheduleItemResponse(
                session.getId(),
                session.getClassroom().getId(),
                session.getClassroom().getClassName(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getClassroom().getRoom(),
                session.getStatus()
        );
    }

    private StudentAttendanceItemResponse toStudentAttendanceItem(Attendance attendance) {
        ClassSession session = attendance.getSession();
        return new StudentAttendanceItemResponse(
                attendance.getId(),
                session.getId(),
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getClassroom().getId(),
                session.getClassroom().getClassName(),
                attendance.getStatus(),
                attendance.getNote(),
                attendance.getMarkedAt(),
                Boolean.TRUE.equals(attendance.getValid())
        );
    }
}
