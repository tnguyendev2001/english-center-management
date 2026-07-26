package com.englishcenter.classsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.classsession.dto.ClassSessionSearchResponse;
import com.englishcenter.classsession.mapper.ClassSessionMapper;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.enrollment.EnrollmentStatus;
import com.englishcenter.enrollment.EnrollmentStatusHistoryRepository;
import com.englishcenter.makeupcredit.MakeupCreditRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class ClassSessionFocusSearchTest {
    @Mock
    private ClassSessionRepository classSessionRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private MakeupCreditRepository makeupCreditRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    private final ClassSessionMapper classSessionMapper = new ClassSessionMapper();

    @Test
    void searchFocusesTodayAndComputesPageWithDescendingSort() {
        ClassSessionService service = newService();
        LocalDate today = LocalDate.now();
        ClassSession todaySession = session(30L, 30, today, LocalTime.of(18, 0));

        when(classSessionRepository.search(eq(2L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(todaySession), Pageable.ofSize(10), 35));
        when(classSessionRepository.findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(2L, today))
                .thenReturn(List.of(todaySession));
        when(classSessionRepository.findNextSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of());
        when(classSessionRepository.findLatestPastSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of(session(29L, 29, today.minusDays(2), LocalTime.of(18, 0))));
        when(classSessionRepository.countSessionsBeforeDescending(
                eq(2L), isNull(), isNull(), isNull(), eq(today), eq(LocalTime.of(18, 0)), eq(30L)
        )).thenReturn(5L);
        when(classSessionRepository.countSessionsBeforeDescending(
                eq(2L),
                isNull(),
                isNull(),
                isNull(),
                eq(today.minusDays(2)),
                eq(LocalTime.of(18, 0)),
                eq(29L)
        )).thenReturn(6L);
        when(attendanceRepository.countBySessionIdAndValidTrue(30L)).thenReturn(3L);
        when(attendanceRepository.countBySessionIdAndValidTrue(29L)).thenReturn(8L);
        when(enrollmentRepository.countByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE)).thenReturn(10L);

        ClassSessionService.SearchResult result = service.search(
                2L,
                null,
                null,
                null,
                0,
                10,
                "sessionDate",
                "DESC"
        );

        ClassSessionSearchResponse data = result.data();
        assertThat(data.focusSession().type()).isEqualTo(FocusSessionType.TODAY);
        assertThat(data.focusSession().sessionId()).isEqualTo(30L);
        assertThat(data.focusSession().page()).isEqualTo(0);
        assertThat(data.focusSession().markedCount()).isEqualTo(3);
        assertThat(data.focusSession().totalStudents()).isEqualTo(10);
        assertThat(data.focusTargets().today().page()).isEqualTo(0);
        assertThat(data.focusTargets().latest().sessionId()).isEqualTo(29L);
        assertThat(data.focusTargets().latest().page()).isEqualTo(0);
        assertThat(data.focusTargets().next()).isNull();
    }

    @Test
    void searchFallsBackToNextWhenNoTodaySession() {
        ClassSessionService service = newService();
        LocalDate today = LocalDate.now();
        ClassSession nextSession = session(40L, 40, today.plusDays(1), LocalTime.of(18, 0));

        when(classSessionRepository.search(eq(2L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(nextSession), Pageable.ofSize(10), 12));
        when(classSessionRepository.findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(2L, today))
                .thenReturn(List.of());
        when(classSessionRepository.findNextSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of(nextSession));
        when(classSessionRepository.findLatestPastSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of());
        when(classSessionRepository.countSessionsBeforeDescending(
                eq(2L), isNull(), isNull(), isNull(), eq(today.plusDays(1)), eq(LocalTime.of(18, 0)), eq(40L)
        )).thenReturn(0L);
        when(attendanceRepository.countBySessionIdAndValidTrue(40L)).thenReturn(0L);
        when(enrollmentRepository.countByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE)).thenReturn(5L);

        ClassSessionSearchResponse data = service.search(
                2L,
                null,
                null,
                null,
                0,
                10,
                "sessionDate",
                "DESC"
        ).data();

        assertThat(data.focusSession().type()).isEqualTo(FocusSessionType.NEXT);
        assertThat(data.focusSession().sessionId()).isEqualTo(40L);
        assertThat(data.focusTargets().today()).isNull();
        assertThat(data.focusTargets().next().sessionId()).isEqualTo(40L);
    }

    @Test
    void searchFallsBackToLatestWhenNoTodayOrNext() {
        ClassSessionService service = newService();
        LocalDate today = LocalDate.now();
        ClassSession latest = session(20L, 20, today.minusDays(3), LocalTime.of(18, 0));

        when(classSessionRepository.search(eq(2L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(latest), Pageable.ofSize(10), 20));
        when(classSessionRepository.findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(2L, today))
                .thenReturn(List.of());
        when(classSessionRepository.findNextSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of());
        when(classSessionRepository.findLatestPastSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of(latest));
        when(classSessionRepository.countSessionsBeforeDescending(
                eq(2L), isNull(), isNull(), isNull(), eq(today.minusDays(3)), eq(LocalTime.of(18, 0)), eq(20L)
        )).thenReturn(0L);
        when(attendanceRepository.countBySessionIdAndValidTrue(20L)).thenReturn(4L);
        when(enrollmentRepository.countByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE)).thenReturn(4L);

        ClassSessionSearchResponse data = service.search(
                2L,
                null,
                null,
                null,
                0,
                10,
                "sessionDate",
                "DESC"
        ).data();

        assertThat(data.focusSession().type()).isEqualTo(FocusSessionType.LATEST);
        assertThat(data.focusSession().sessionId()).isEqualTo(20L);
    }

    @Test
    void searchComputesFocusPageBeyondFirstPage() {
        ClassSessionService service = newService();
        LocalDate today = LocalDate.now();
        ClassSession todaySession = session(30L, 30, today, LocalTime.of(18, 0));

        when(classSessionRepository.search(eq(2L), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), Pageable.ofSize(10), 45));
        when(classSessionRepository.findByClassroomIdAndSessionDateOrderByStartTimeAscIdAsc(2L, today))
                .thenReturn(List.of(todaySession));
        when(classSessionRepository.findNextSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of());
        when(classSessionRepository.findLatestPastSessions(eq(2L), eq(today), any(Pageable.class)))
                .thenReturn(List.of());
        when(classSessionRepository.countSessionsBeforeDescending(
                eq(2L), isNull(), isNull(), isNull(), eq(today), eq(LocalTime.of(18, 0)), eq(30L)
        )).thenReturn(25L);
        when(attendanceRepository.countBySessionIdAndValidTrue(30L)).thenReturn(0L);
        when(enrollmentRepository.countByClassroomIdAndStatus(2L, EnrollmentStatus.ACTIVE)).thenReturn(8L);

        ClassSessionSearchResponse data = service.search(
                2L,
                null,
                null,
                null,
                0,
                10,
                "sessionDate",
                "DESC"
        ).data();

        assertThat(data.focusSession().page()).isEqualTo(2);
    }

    private ClassSessionService newService() {
        return new ClassSessionService(
                classSessionRepository,
                classroomRepository,
                attendanceRepository,
                makeupCreditRepository,
                enrollmentRepository,
                new EnrollmentSessionService(statusHistoryRepository, enrollmentRepository, attendanceRepository),
                classSessionMapper
        );
    }

    private ClassSession session(Long id, int sessionNo, LocalDate date, LocalTime startTime) {
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("Starter A");

        ClassSession session = new ClassSession();
        session.setId(id);
        session.setClassroom(classroom);
        session.setSessionNo(sessionNo);
        session.setSessionDate(date);
        session.setStartTime(startTime);
        session.setEndTime(startTime.plusHours(1).plusMinutes(30));
        session.setStatus(ClassSessionStatus.SCHEDULED);
        return session;
    }
}
