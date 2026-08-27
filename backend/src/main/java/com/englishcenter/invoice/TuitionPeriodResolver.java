package com.englishcenter.invoice;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classsession.ClassSession;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentEligibilityService;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.studentpackage.StudentPackage;
import com.englishcenter.studentpackage.StudentPackageRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class TuitionPeriodResolver {
    private final ClassSessionRepository classSessionRepository;
    private final AttendanceRepository attendanceRepository;
    private final StudentPackageRepository studentPackageRepository;
    private final EnrollmentEligibilityService enrollmentEligibilityService;
    private final EnrollmentSessionService enrollmentSessionService;

    public TuitionPeriodResolver(
            ClassSessionRepository classSessionRepository,
            AttendanceRepository attendanceRepository,
            StudentPackageRepository studentPackageRepository,
            EnrollmentEligibilityService enrollmentEligibilityService,
            EnrollmentSessionService enrollmentSessionService
    ) {
        this.classSessionRepository = classSessionRepository;
        this.attendanceRepository = attendanceRepository;
        this.studentPackageRepository = studentPackageRepository;
        this.enrollmentEligibilityService = enrollmentEligibilityService;
        this.enrollmentSessionService = enrollmentSessionService;
    }

    public TuitionPeriod resolve(Invoice invoice) {
        StudentPackage studentPackage = invoice.getStudentPackage();
        Enrollment enrollment = invoice.getEnrollment();
        int requiredSessions = invoice.getTotalSessionsSnapshot() == null
                ? studentPackage.getTotalSessions()
                : invoice.getTotalSessionsSnapshot();
        if (requiredSessions <= 0) {
            return new TuitionPeriod(null, null);
        }

        LocalDate cycleStart = studentPackage.getStartDate() != null
                ? studentPackage.getStartDate()
                : enrollment.getStartDate();
        LocalDate nextCycleStart = findNextCycleStart(studentPackage);
        Map<Long, Attendance> attendanceBySessionId = attendanceRepository
                .findValidByStudentIdAndClassroomId(
                        invoice.getStudent().getId(),
                        invoice.getClassroom().getId()
                )
                .stream()
                .collect(Collectors.toMap(
                        attendance -> attendance.getSession().getId(),
                        Function.identity(),
                        (first, second) -> second
                ));

        List<ClassSession> sessions = classSessionRepository
                .findByClassroomIdOrderBySessionDateAscStartTimeAsc(invoice.getClassroom().getId());

        LocalDate periodStart = null;
        LocalDate periodEnd = null;
        int countedSessions = 0;
        for (ClassSession session : sessions) {
            if (!belongsToCycle(session, enrollment, studentPackage, cycleStart, nextCycleStart)) {
                continue;
            }

            Attendance attendance = attendanceBySessionId.get(session.getId());
            boolean consumesSession = attendance != null
                    ? enrollmentSessionService.consumesSession(attendance, session, enrollment)
                    : enrollmentEligibilityService.isActiveOnDate(enrollment, session.getSessionDate());
            if (!consumesSession) {
                continue;
            }

            if (periodStart == null) {
                periodStart = session.getSessionDate();
            }
            countedSessions++;
            if (countedSessions == requiredSessions) {
                periodEnd = session.getSessionDate();
                break;
            }
        }

        return new TuitionPeriod(periodStart, periodEnd);
    }

    private boolean belongsToCycle(
            ClassSession session,
            Enrollment enrollment,
            StudentPackage studentPackage,
            LocalDate cycleStart,
            LocalDate nextCycleStart
    ) {
        LocalDate sessionDate = session.getSessionDate();
        if (session.getStatus() == ClassSessionStatus.CANCELED || sessionDate.isBefore(cycleStart)) {
            return false;
        }
        if (studentPackage.getEndDate() != null && sessionDate.isAfter(studentPackage.getEndDate())) {
            return false;
        }
        if (nextCycleStart != null && !sessionDate.isBefore(nextCycleStart)) {
            return false;
        }
        if (enrollment.getEndDate() != null && sessionDate.isAfter(enrollment.getEndDate())) {
            return false;
        }
        return true;
    }

    private LocalDate findNextCycleStart(StudentPackage currentPackage) {
        return studentPackageRepository.findAllByEnrollmentId(currentPackage.getEnrollment().getId())
                .stream()
                .filter(candidate -> !candidate.getId().equals(currentPackage.getId()))
                .filter(candidate -> candidate.getCycleNo() > currentPackage.getCycleNo())
                .map(StudentPackage::getStartDate)
                .filter(startDate -> startDate != null && startDate.isAfter(currentPackage.getStartDate()))
                .min(LocalDate::compareTo)
                .orElse(null);
    }

    public record TuitionPeriod(LocalDate start, LocalDate end) {
    }
}
