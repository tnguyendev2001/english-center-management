package com.englishcenter.classsession;

import com.englishcenter.attendance.Attendance;
import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.classsession.dto.GenerateClassSessionsResponse;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.makeupcredit.MakeupCredit;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import com.englishcenter.makeupcredit.MakeupCreditStatus;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClassSessionService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ClassSessionRepository classSessionRepository;
    private final ClassroomRepository classroomRepository;
    private final AttendanceRepository attendanceRepository;
    private final MakeupCreditRepository makeupCreditRepository;
    private final ClassSessionMapper classSessionMapper;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository,
            ClassroomRepository classroomRepository,
            AttendanceRepository attendanceRepository,
            MakeupCreditRepository makeupCreditRepository,
            ClassSessionMapper classSessionMapper
    ) {
        this.classSessionRepository = classSessionRepository;
        this.classroomRepository = classroomRepository;
        this.attendanceRepository = attendanceRepository;
        this.makeupCreditRepository = makeupCreditRepository;
        this.classSessionMapper = classSessionMapper;
    }

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

    @Transactional(readOnly = true)
    public Page<ClassSessionResponse> search(Long classroomId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));
        Page<ClassSession> sessions = classroomId == null
                ? classSessionRepository.findAllByOrderBySessionDateAscStartTimeAsc(pageable)
                : classSessionRepository.findByClassroomIdOrderBySessionDateAscStartTimeAsc(classroomId, pageable);

        return sessions.map(classSessionMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public List<ClassSessionResponse> getTodaySessions() {
        return classSessionRepository.findBySessionDateOrderByStartTimeAsc(LocalDate.now())
                .stream()
                .map(classSessionMapper::toResponse)
                .toList();
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
        boolean hasUsedMakeupCredit = linkedCredits.stream()
                .anyMatch(credit -> credit.getStatus() == MakeupCreditStatus.USED
                        || credit.getUsedSessions() > 0);
        if (hasUsedMakeupCredit) {
            throw new BusinessException(
                    "Cannot correction-cancel session: makeup credit from this session has already been used"
            );
        }

        for (MakeupCredit credit : linkedCredits) {
            if (credit.getStatus() == MakeupCreditStatus.AVAILABLE) {
                credit.setStatus(MakeupCreditStatus.CANCELED);
                makeupCreditRepository.save(credit);
            }
        }

        String voidReason = request.reason().trim();
        LocalDateTime voidedAt = LocalDateTime.now();
        for (Attendance attendance : attendanceRepository.findBySessionId(id)) {
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

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
