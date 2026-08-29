package com.englishcenter.classsession;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.ClassSessionSearchResponse;
import com.englishcenter.classsession.dto.FocusSessionTargetResponse;
import com.englishcenter.classsession.dto.FocusSessionTargetsResponse;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.classsession.dto.SessionGenerationPlan;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.enrollment.Enrollment;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import com.englishcenter.studentpackage.PackageTimelineService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassSessionRepository classSessionRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceRepository attendanceRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final EnrollmentSessionService enrollmentSessionService;
    private final ClassSessionMapper classSessionMapper;
    private final PackageTimelineService packageTimelineService;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository,
            ClassroomRepository classroomRepository,
            AttendanceRepository attendanceRepository,
            MakeupCreditRepository makeupCreditRepository,
            EnrollmentRepository enrollmentRepository,
            EnrollmentSessionService enrollmentSessionService,
            ClassSessionMapper classSessionMapper,
            PackageTimelineService packageTimelineService
    ) {
        this.classSessionRepository = classSessionRepository;
        this.classroomRepository = classroomRepository;
        this.attendanceRepository = attendanceRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.enrollmentSessionService = enrollmentSessionService;
        this.classSessionMapper = classSessionMapper;
        this.packageTimelineService = packageTimelineService;
    }

    /**
     * Shared session generation used by the class-session API and legacy Excel import.
     * Existing sessions for the same classroom/date/time slot are reused (skipped), never overwritten.
     */
    @Transactional
    public GenerateClassSessionsResponse generate(GenerateClassSessionsRequest request) {
        Classroom classroom = classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
        Set<DayOfWeek> daysOfWeek = ClassDayOfWeek.toJavaDayOfWeekSet(classroom.getDaysOfWeek());
        if (daysOfWeek.isEmpty()) {
            throw new BusinessException("Classroom days of week is not configured");
        }
        LocalDate cursor = request.fromDate() == null ? classroom.getStartDate() : request.fromDate();
        LocalDate toDate = request.toDate();
        Integer numberOfSessions = request.numberOfSessions();

        if (numberOfSessions == null && toDate == null) {
            throw new BusinessException("Number of sessions or date range is required");
        }

        int nextSessionNo = classSessionRepository.countByClassroomId(classroom.getId()) + 1;
        List<ClassSession> created = new ArrayList<>();
        int skippedCount = 0;
        int weekdaySlotsProcessed = 0;

        while ((numberOfSessions == null || weekdaySlotsProcessed < numberOfSessions)
                && (toDate == null || !cursor.isAfter(toDate))) {
            if (daysOfWeek.contains(cursor.getDayOfWeek())) {
                weekdaySlotsProcessed++;

                if (classSessionRepository.existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
                        classroom.getId(),
                        cursor,
                        classroom.getStartTime(),
                        classroom.getEndTime()
                )) {
                    skippedCount++;
                } else {
                    ClassSession session = new ClassSession();
                    session.setClassroom(classroom);
                    session.setSessionNo(nextSessionNo++);
                    session.setSessionDate(cursor);
                    session.setStartTime(classroom.getStartTime());
                    session.setEndTime(classroom.getEndTime());
                    session.setStatus(ClassSessionStatus.SCHEDULED);
                    created.add(classSessionRepository.save(session));
                }
            }

            cursor = cursor.plusDays(1);
        }

        List<ClassSessionResponse> sessionResponses = created.stream()
                .map(classSessionMapper::toResponse)
                .toList();
        return new GenerateClassSessionsResponse(created.size(), skippedCount, sessionResponses);
    }

    /**
     * Convenience entry point for legacy import: generate sessions from classroom start through today.
     */
    @Transactional
    public GenerateClassSessionsResponse generateUpToDate(Long classroomId, LocalDate fromDate, LocalDate toDate) {
        return generate(new GenerateClassSessionsRequest(classroomId, null, fromDate, toDate));
    }

    @Transactional(readOnly = true)
    public SessionGenerationPlan planGeneration(Long classroomId, LocalDate fromDate, LocalDate toDate) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
        return planGeneration(classroom, fromDate, toDate);
    }

    @Transactional(readOnly = true)
    public SessionGenerationPlan planGeneration(Classroom classroom, LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> plannedDates = plannedSessionDates(classroom, fromDate, toDate);
        int existingInRange = fromDate == null || toDate == null
                ? 0
                : classSessionRepository.countByClassroomIdAndSessionDateBetween(
                        classroom.getId(),
                        fromDate,
                        toDate
                );
        int toCreate = 0;
        int toReuse = 0;
        for (LocalDate date : plannedDates) {
            if (classSessionRepository.existsByClassroomIdAndSessionDateAndStartTimeAndEndTime(
                    classroom.getId(),
                    date,
                    classroom.getStartTime(),
                    classroom.getEndTime()
            )) {
                toReuse++;
            } else {
                toCreate++;
            }
        }
        return new SessionGenerationPlan(
                classroom.getId(),
                existingInRange,
                toCreate,
                toReuse,
                plannedDates.isEmpty() ? null : plannedDates.getFirst(),
                plannedDates.isEmpty() ? null : plannedDates.getLast(),
                plannedDates
        );
    }

    /**
     * Computes study dates using the same weekday rules as {@link #generate}.
     * Used by legacy import preview before any classroom is persisted.
     */
    public List<LocalDate> plannedSessionDates(
            LocalDate classroomStartDate,
            Set<ClassDayOfWeek> daysOfWeek,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Set<DayOfWeek> javaDays = ClassDayOfWeek.toJavaDayOfWeekSet(daysOfWeek);
        if (javaDays.isEmpty()) {
            throw new BusinessException("Classroom days of week is not configured");
        }
        LocalDate cursor = fromDate == null ? classroomStartDate : fromDate;
        if (cursor == null || toDate == null) {
            throw new BusinessException("Number of sessions or date range is required");
        }
        List<LocalDate> dates = new ArrayList<>();
        while (!cursor.isAfter(toDate)) {
            if (javaDays.contains(cursor.getDayOfWeek())) {
                dates.add(cursor);
            }
            cursor = cursor.plusDays(1);
        }
        return dates;
    }

    private List<LocalDate> plannedSessionDates(Classroom classroom, LocalDate fromDate, LocalDate toDate) {
        return plannedSessionDates(
                classroom.getStartDate(),
                classroom.getDaysOfWeek(),
                fromDate == null ? classroom.getStartDate() : fromDate,
                toDate
        );
    }

    @Transactional(readOnly = true)
    public SearchResult search(
            Long classroomId,
            LocalDate fromDate,
            LocalDate toDate,
            ClassSessionStatus status,
            int page,
            int size,
            String sort,
            String direction
    ) {
        int normalizedSize = normalizePageSize(size);
        int normalizedPage = Math.max(page, 0);
        Sort sortSpec = resolveSort(sort, direction);
        boolean ascending = isAscending(sortSpec);

        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize, sortSpec);
        Page<ClassSession> sessions = classSessionRepository.search(
                classroomId,
                fromDate,
                toDate,
                status,
                pageable
        );

        // Quick-nav targets ignore list filters so "Hôm nay" / "Buổi tiếp theo" stay usable.
        FocusSessionTargetsResponse focusTargets = classroomId == null
                ? new FocusSessionTargetsResponse(null, null, null)
                : buildFocusTargets(classroomId, normalizedSize, ascending);

        return new SearchResult(
                new ClassSessionSearchResponse(
                        sessions.map(classSessionMapper::toResponse).getContent(),
                        resolveFocusSession(focusTargets),
                        focusTargets
                ),
                sessions.getNumber(),
                sessions.getSize(),
                sessions.getTotalElements(),
                sessions.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ClassSessionResponse getById(Long id) {
        ClassSession session = classSessionRepository.findByIdWithClassroom(id)
                .orElseThrow(() -> new NotFoundException("Class session not found"));
        return classSessionMapper.toResponse(session);
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getTodaySessions() {
        return classSessionRepository.findBySessionDateOrderByStartTimeAsc(LocalDate.now())
                .stream()
                .map(classSessionMapper::toResponse)
                .toList();
    }

    public record SearchResult(
            ClassSessionSearchResponse data,
            int page,
            int size,
            long totalElements,
            int totalPages
    ) {
    }

    private FocusSessionTargetsResponse buildFocusTargets(
            Long classroomId,
            int pageSize,
            boolean ascending
    ) {
        LocalDate today = LocalDate.now();
        Pageable single = PageRequest.of(0, 1);

        ClassSession todaySession = classSessionRepository
                .findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(classroomId, today)
                .stream()
                .findFirst()
                .orElse(null);
        ClassSession nextSession = classSessionRepository.findNextSessions(classroomId, today, single)
                .stream()
                .findFirst()
                .orElse(null);
        ClassSession latestSession = classSessionRepository.findLatestPastSessions(classroomId, today, single)
                .stream()
                .findFirst()
                .orElse(null);

        return new FocusSessionTargetsResponse(
                toFocusTarget(todaySession, FocusSessionType.TODAY, classroomId, pageSize, ascending),
                toFocusTarget(nextSession, FocusSessionType.NEXT, classroomId, pageSize, ascending),
                toFocusTarget(latestSession, FocusSessionType.LATEST, classroomId, pageSize, ascending)
        );
    }

    private FocusSessionTargetResponse resolveFocusSession(FocusSessionTargetsResponse targets) {
        if (targets.today() != null) {
            return targets.today();
        }
        if (targets.next() != null) {
            return targets.next();
        }
        if (targets.latest() != null) {
            return targets.latest();
        }
        return new FocusSessionTargetResponse(
                null,
                null,
                FocusSessionType.NONE,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private FocusSessionTargetResponse toFocusTarget(
            ClassSession session,
            FocusSessionType type,
            Long classroomId,
            int pageSize,
            boolean ascending
    ) {
        if (session == null) {
            return null;
        }

        long index = ascending
                ? classSessionRepository.countSessionsBeforeAscending(
                        classroomId,
                        null,
                        null,
                        null,
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getId()
                )
                : classSessionRepository.countSessionsBeforeDescending(
                        classroomId,
                        null,
                        null,
                        null,
                        session.getSessionDate(),
                        session.getStartTime(),
                        session.getId()
                );
        int focusPage = (int) (index / pageSize);
        int markedCount = (int) attendanceRepository.countBySessionIdAndValidTrue(session.getId());
        int totalStudents = (int) enrollmentRepository.countByClassroomIdAndStatus(
                classroomId,
                EnrollmentStatus.ACTIVE
        );

        return new FocusSessionTargetResponse(
                session.getId(),
                focusPage,
                type,
                session.getSessionDate(),
                session.getStartTime(),
                session.getEndTime(),
                session.getStatus(),
                session.getSessionNo(),
                markedCount,
                totalStudents
        );
    }

    private boolean isAscending(Sort sortSpec) {
        Sort.Order order = sortSpec.getOrderFor("sessionDate");
        if (order == null) {
            order = sortSpec.getOrderFor("sessionNo");
        }
        return order == null || order.getDirection().isAscending();
    }

    private Sort resolveSort(String sort, String direction) {
        String sortField = sort == null || sort.isBlank() ? "sessionDate" : sort.trim();
        if (!"sessionDate".equals(sortField) && !"sessionNo".equals(sortField)) {
            sortField = "sessionDate";
        }

        Sort.Direction sortDirection = Sort.Direction.DESC;
        if (direction != null && !direction.isBlank()) {
            try {
                sortDirection = Sort.Direction.fromString(direction.trim().toUpperCase(Locale.ROOT));
            } catch (IllegalArgumentException ignored) {
                sortDirection = Sort.Direction.DESC;
            }
        }

        if ("sessionNo".equals(sortField)) {
            return Sort.by(sortDirection, "sessionNo").and(Sort.by(sortDirection, "id"));
        }

        return Sort.by(sortDirection, "sessionDate")
                .and(Sort.by(sortDirection, "startTime"))
                .and(Sort.by(sortDirection, "id"));
    }

    @Transactional
    public ClassSessionResponse cancel(Long id, CancelClassSessionRequest request) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class session not found"));
        if (session.getStatus() == ClassSessionStatus.COMPLETED) {
            throw new BusinessException("Cannot cancel completed session");
        }
        if (session.getStatus() == ClassSessionStatus.CANCELED) {
            throw new BusinessException("Session is already canceled");
        }
        if (attendanceRepository.existsBySessionId(id)) {
            throw new BusinessException("Cannot cancel session with attendance records");
        }
        session.setStatus(ClassSessionStatus.CANCELED);
        session.setCancelReason(request.reason().trim());
        // TODO: Save ActivityLog for CANCEL_SESSION when ActivityLog exists.
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Transactional
    public ClassSessionResponse correctionCancel(Long id, CancelClassSessionRequest request) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class session not found"));
        if (session.getStatus() != ClassSessionStatus.COMPLETED) {
            throw new BusinessException("Only completed sessions can be correction-canceled");
        }
        if (!attendanceRepository.existsBySessionId(id)) {
            throw new BusinessException("Correction cancel requires attendance records");
        }

        List<MakeupCredit> linkedCredits = makeupCreditRepository.findBySourceSessionId(id);
        for (MakeupCredit credit : linkedCredits) {
            if (credit.getStatus() != MakeupCreditStatus.CANCELED) {
                credit.setStatus(MakeupCreditStatus.CANCELED);
                makeupCreditRepository.save(credit);
            }
        }

        String voidReason = request.reason().trim();
        LocalDateTime voidedAt = LocalDateTime.now();
        for (Attendance attendance : attendanceRepository.findBySessionId(id)) {
            Enrollment enrollment = findEnrollmentForAttendance(
                    attendance.getStudent().getId(),
                    session.getClassroom().getId()
            );
            if (enrollment != null
                    && enrollmentSessionService.consumesSession(attendance, session, enrollment)) {
                enrollmentSessionService.reverseConsumedSession(enrollment);
                enrollmentRepository.save(enrollment);
                packageTimelineService.markNeedsRecalculation(enrollment.getId());
            }

            attendance.setValid(false);
            attendance.setVoidReason(voidReason);
            attendance.setVoidedAt(voidedAt);
            attendanceRepository.save(attendance);
        }

        session.setStatus(ClassSessionStatus.CANCELED);
        session.setCancelReason(request.reason().trim());
        // TODO: Save ActivityLog for CORRECTION_CANCEL_SESSION when ActivityLog exists.
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    @Transactional
    public ClassSessionResponse restore(Long id) {
        ClassSession session = classSessionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Class session not found"));
        if (session.getStatus() != ClassSessionStatus.CANCELED) {
            throw new BusinessException("Only canceled sessions can be restored");
        }
        session.setStatus(ClassSessionStatus.SCHEDULED);
        session.setCancelReason(null);
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    private Enrollment findEnrollmentForAttendance(Long studentId, Long classroomId) {
        List<Enrollment> enrollments = enrollmentRepository
                .findByStudentIdAndClassroomIdOrderByStartDateAscIdAsc(studentId, classroomId);
        if (enrollments.isEmpty()) {
            return null;
        }

        return enrollments.stream()
                .filter(enrollment -> enrollment.getStatus() != EnrollmentStatus.CANCELED)
                .reduce((first, second) -> second)
                .orElse(enrollments.getFirst());
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
