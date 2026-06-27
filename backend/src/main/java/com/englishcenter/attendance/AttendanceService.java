package com.englishcenter.attendance;

import com.englishcenter.attendance.dto.AttendanceItemRequest;
import com.englishcenter.attendance.dto.AttendanceResponse;
import com.englishcenter.attendance.dto.MarkAttendanceRequest;
import com.englishcenter.attendance.mapper.AttendanceMapper;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditReason;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.student.Student;
import com.englishcenter.student.StudentRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {
    private static final int MAX_PAGE_SIZE = 100;

    private final AttendanceRepository attendanceRepository;
    private final ClassSessionRepository classSessionRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudentRepository studentRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final AttendanceMapper attendanceMapper;

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            ClassSessionRepository classSessionRepository,
            EnrollmentRepository enrollmentRepository,
            StudentRepository studentRepository,
            MakeupCreditRepository makeupCreditRepository,
            AttendanceMapper attendanceMapper
    ) {
        this.attendanceRepository = attendanceRepository;
        this.classSessionRepository = classSessionRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studentRepository = studentRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.attendanceMapper = attendanceMapper;
    }

    @Transactional
    public List<AttendanceResponse> mark(MarkAttendanceRequest request) {
        ClassSession session = classSessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new NotFoundException("Class session not found"));
        if (session.getStatus() == ClassSessionStatus.CANCELED) {
            throw new BusinessException("Cannot mark attendance for canceled session");
        }

        Map<Long, Student> activeStudents = activeStudentsById(session.getClassroom().getId());
        List<Attendance> saved = request.items().stream()
                .map(item -> markOne(session, activeStudents, item))
                .toList();

        session.setStatus(ClassSessionStatus.COMPLETED);
        classSessionRepository.save(session);
        // TODO: Save ActivityLog for MARK_ATTENDANCE when ActivityLog exists.
        return saved.stream().map(attendanceMapper::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Page<AttendanceResponse> getAttendance(Long sessionId, int page, int size) {
        if (sessionId != null) {
            List<AttendanceResponse> responses = attendanceRepository.findBySessionId(sessionId)
                    .stream()
                    .map(attendanceMapper::toResponse)
                    .toList();
            return new org.springframework.data.domain.PageImpl<>(responses);
        }
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));
        return attendanceRepository.findAllByOrderByMarkedAtDesc(pageable)
                .map(attendanceMapper::toResponse);
    }

    private Attendance markOne(
            ClassSession session,
            Map<Long, Student> activeStudents,
            AttendanceItemRequest item
    ) {
        Student student = activeStudents.get(item.studentId());
        if (student == null) {
            throw new BusinessException("Student is not actively enrolled in this classroom");
        }

        Attendance attendance = attendanceRepository.findBySessionIdAndStudentId(session.getId(), student.getId())
                .orElseGet(Attendance::new);
        attendance.setSession(session);
        attendance.setStudent(student);
        attendance.setStatus(item.status());
        attendance.setNote(trimToNull(item.note()));
        attendance.setMarkedAt(LocalDateTime.now());

        Attendance saved = attendanceRepository.save(attendance);
        if (item.status() == AttendanceStatus.EXCUSED) {
            createMakeupCreditIfNeeded(session, student);
        }
        return saved;
    }

    private Map<Long, Student> activeStudentsById(Long classroomId) {
        List<Enrollment> enrollments = enrollmentRepository.findByClassroomIdAndStatus(
                classroomId,
                EnrollmentStatus.ACTIVE
        );
        Set<Long> studentIds = enrollments.stream()
                .map(enrollment -> enrollment.getStudent().getId())
                .collect(Collectors.toSet());
        return studentRepository.findAllById(studentIds)
                .stream()
                .collect(Collectors.toMap(Student::getId, Function.identity()));
    }

    private void createMakeupCreditIfNeeded(ClassSession session, Student student) {
        makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                student.getId(),
                session.getId(),
                MakeupCreditReason.EXCUSED_ABSENCE
        ).orElseGet(() -> {
            MakeupCredit credit = new MakeupCredit();
            credit.setStudent(student);
            credit.setClassroom(session.getClassroom());
            credit.setSourceSession(session);
            credit.setReason(MakeupCreditReason.EXCUSED_ABSENCE);
            credit.setCreditSessions(1);
            credit.setUsedSessions(0);
            credit.setStatus(MakeupCreditStatus.AVAILABLE);
            credit.setNote("Created from excused absence");
            return makeupCreditRepository.save(credit);
        });
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
