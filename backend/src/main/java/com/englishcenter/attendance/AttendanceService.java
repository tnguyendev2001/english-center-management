package com.englishcenter.attendance;

import com.englishcenter.attendance.dto.AttendanceItemRequest;
import com.englishcenter.attendance.dto.AttendanceResponse;
import com.englishcenter.attendance.dto.MarkAttendanceRequest;
import com.englishcenter.attendance.mapper.AttendanceMapper;
import com.englishcenter.classroom.ClassroomStatus;
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
import java.util.Optional;
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
        validateClassroomAllowsAttendance(session);
        validateSessionAllowsAttendance(session);

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

        Optional<Attendance> existingOptional = attendanceRepository.findBySessionIdAndStudentId(
                session.getId(),
                student.getId()
        );
        AttendanceStatus previousStatus = existingOptional.map(Attendance::getStatus).orElse(null);

        if (previousStatus == AttendanceStatus.EXCUSED
                && (item.status() == AttendanceStatus.PRESENT || item.status() == AttendanceStatus.ABSENT)) {
            String correctionReason = trimToNull(item.correctionReason());
            if (correctionReason == null) {
                throw new BusinessException("Correction reason is required when changing excused attendance");
            }
            cancelMakeupCreditForExcusedCorrection(session, student, correctionReason);
        }

        Attendance attendance = existingOptional.orElseGet(Attendance::new);
        attendance.setSession(session);
        attendance.setStudent(student);
        attendance.setStatus(item.status());
        attendance.setNote(resolveAttendanceNote(item, previousStatus));
        attendance.setMarkedAt(LocalDateTime.now());
        attendance.setValid(true);
        attendance.setVoidReason(null);
        attendance.setVoidedAt(null);

        Attendance saved = attendanceRepository.save(attendance);

        if (item.status() == AttendanceStatus.EXCUSED) {
            ensureMakeupCredit(session, student);
        }

        return saved;
    }

    private String resolveAttendanceNote(AttendanceItemRequest item, AttendanceStatus previousStatus) {
        String correctionReason = trimToNull(item.correctionReason());
        if (correctionReason != null && previousStatus != null && previousStatus != item.status()) {
            return correctionReason;
        }
        return trimToNull(item.note());
    }

    private void cancelMakeupCreditForExcusedCorrection(
            ClassSession session,
            Student student,
            String correctionReason
    ) {
        Optional<MakeupCredit> creditOptional = makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                student.getId(),
                session.getId(),
                MakeupCreditReason.EXCUSED_ABSENCE
        );
        if (creditOptional.isEmpty()) {
            return;
        }

        MakeupCredit credit = creditOptional.get();
        if (credit.getStatus() == MakeupCreditStatus.USED || credit.getUsedSessions() > 0) {
            throw new BusinessException(
                    "Cannot change excused attendance: makeup credit from this session has already been used"
            );
        }

        if (credit.getStatus() == MakeupCreditStatus.AVAILABLE) {
            credit.setStatus(MakeupCreditStatus.CANCELED);
            credit.setNote("Canceled: attendance corrected (" + correctionReason + ")");
            makeupCreditRepository.save(credit);
        }
    }

    private void ensureMakeupCredit(ClassSession session, Student student) {
        Optional<MakeupCredit> creditOptional = makeupCreditRepository.findByStudentIdAndSourceSessionIdAndReason(
                student.getId(),
                session.getId(),
                MakeupCreditReason.EXCUSED_ABSENCE
        );

        if (creditOptional.isEmpty()) {
            MakeupCredit credit = new MakeupCredit();
            credit.setStudent(student);
            credit.setClassroom(session.getClassroom());
            credit.setSourceSession(session);
            credit.setReason(MakeupCreditReason.EXCUSED_ABSENCE);
            credit.setCreditSessions(1);
            credit.setUsedSessions(0);
            credit.setStatus(MakeupCreditStatus.AVAILABLE);
            credit.setNote("Created from excused absence");
            makeupCreditRepository.save(credit);
            return;
        }

        MakeupCredit credit = creditOptional.get();
        if (credit.getStatus() == MakeupCreditStatus.AVAILABLE) {
            return;
        }

        if (credit.getStatus() == MakeupCreditStatus.CANCELED && credit.getUsedSessions() == 0) {
            credit.setStatus(MakeupCreditStatus.AVAILABLE);
            credit.setNote("Reactivated from excused absence");
            makeupCreditRepository.save(credit);
        }
    }

    private void validateClassroomAllowsAttendance(ClassSession session) {
        if (session.getClassroom().getStatus() != ClassroomStatus.ONGOING) {
            throw new BusinessException("Cannot mark attendance for classroom that is not ongoing");
        }
    }

    private void validateSessionAllowsAttendance(ClassSession session) {
        if (session.getStatus() == ClassSessionStatus.CANCELED) {
            throw new BusinessException("Cannot mark attendance for canceled session");
        }

        if (session.getStatus() != ClassSessionStatus.SCHEDULED
                && session.getStatus() != ClassSessionStatus.COMPLETED) {
            throw new BusinessException("Cannot mark attendance for this session status");
        }
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
