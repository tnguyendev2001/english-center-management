package com.englishcenter.classsession;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.dto.CancelClassSessionRequest;
import com.englishcenter.classsession.dto.ClassSessionResponse;
import com.englishcenter.classsession.dto.GenerateClassSessionsRequest;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import java.time.DayOfWeek;
import java.time.LocalDate;
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
    private final ClassSessionMapper classSessionMapper;

    public ClassSessionService(
            ClassSessionRepository classSessionRepository,
            ClassroomRepository classroomRepository,
            ClassSessionMapper classSessionMapper
    ) {
        this.classSessionRepository = classSessionRepository;
        this.classroomRepository = classroomRepository;
        this.classSessionMapper = classSessionMapper;
    }

    @Transactional
    public List<ClassSessionResponse> generate(GenerateClassSessionsRequest request) {
        Classroom classroom = classroomRepository.findById(request.classroomId())
                .orElseThrow(() -> new NotFoundException("Classroom not found"));
        Set<DayOfWeek> daysOfWeek = ClassDayOfWeek.toJavaDayOfWeekSet(classroom.getDaysOfWeek());
        if (daysOfWeek.isEmpty()) {
            throw new BusinessException("Classroom days of week is not configured");
        }
        LocalDate cursor = request.fromDate() == null ? classroom.getStartDate() : request.fromDate();
        LocalDate toDate = request.toDate();
        int targetCount = request.numberOfSessions() == null ? Integer.MAX_VALUE : request.numberOfSessions();

        if (request.numberOfSessions() == null && toDate == null) {
            throw new BusinessException("Number of sessions or date range is required");
        }

        int nextSessionNo = classSessionRepository.countByClassroomId(classroom.getId()) + 1;
        List<ClassSession> created = new ArrayList<>();

        while (created.size() < targetCount && (toDate == null || !cursor.isAfter(toDate))) {
            if (daysOfWeek.contains(cursor.getDayOfWeek())) {
                ClassSession session = new ClassSession();
                session.setClassroom(classroom);
                session.setSessionNo(nextSessionNo++);
                session.setSessionDate(cursor);
                session.setStartTime(classroom.getStartTime());
                session.setEndTime(classroom.getEndTime());
                session.setStatus(ClassSessionStatus.SCHEDULED);
                created.add(classSessionRepository.save(session));
            }

            cursor = cursor.plusDays(1);
        }

        return created.stream().map(classSessionMapper::toResponse).toList();
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
        session.setStatus(ClassSessionStatus.CANCELED);
        session.setCancelReason(request.reason().trim());
        // TODO: Save ActivityLog for CANCEL_SESSION when ActivityLog exists.
        return classSessionMapper.toResponse(classSessionRepository.save(session));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
