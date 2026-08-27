package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.attendance.AttendanceRepository;
import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classsession.ClassSessionRepository;
import com.englishcenter.classsession.ClassSessionStatus;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.student.Student;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EnrollmentEligibilityServiceTest {
    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private EnrollmentStatusHistoryRepository statusHistoryRepository;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private ClassSessionRepository classSessionRepository;

    @Test
    void isActiveOnDateRequiresLearningStartAndActiveHistoryPeriod() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        LocalDate sessionDate = LocalDate.of(2026, 7, 10);

        when(statusHistoryRepository.isActiveAt(10L, sessionDate)).thenReturn(true);

        assertThat(service.isActiveOnDate(enrollment, sessionDate)).isTrue();
        assertThat(service.isActiveOnDate(enrollment, LocalDate.of(2026, 6, 30))).isFalse();
    }

    @Test
    void stopAfterLatestAttendanceIsAllowed() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(attendanceRepository.findLatestValidAttendanceDate(1L, 2L, LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 10)));

        service.validateInactiveEffectiveDate(enrollment, LocalDate.of(2026, 7, 11));
    }

    @Test
    void stopBeforeLatestAttendanceIsRejected() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(attendanceRepository.findLatestValidAttendanceDate(1L, 2L, LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 10)));

        assertThatThrownBy(() -> service.validateInactiveEffectiveDate(enrollment, LocalDate.of(2026, 7, 3)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentEligibilityService.inactiveDateAfterAttendanceMessage(
                        LocalDate.of(2026, 7, 10)
                ));
    }

    @Test
    void stopOnLatestAttendanceDateIsRejected() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(attendanceRepository.findLatestValidAttendanceDate(1L, 2L, LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 10)));

        assertThatThrownBy(() -> service.validateInactiveEffectiveDate(enrollment, LocalDate.of(2026, 7, 10)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("10/07/2026");
    }

    @Test
    void stopUsesLatestRemainingAttendanceAfterCorrection() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(attendanceRepository.findLatestValidAttendanceDate(1L, 2L, LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 2)));

        service.validateInactiveEffectiveDate(enrollment, LocalDate.of(2026, 7, 3));
    }

    @Test
    void learningStartDateCanMoveForwardWhenNoValidAttendance() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(
                        history(1L, EnrollmentStatus.ACTIVE, LocalDate.of(2026, 7, 1), null)
                ));
        when(attendanceRepository.findEarliestValidAttendanceDate(1L, 2L)).thenReturn(Optional.empty());
        when(classSessionRepository.countByClassroomId(2L)).thenReturn(0);

        service.validateLearningStartDateChange(enrollment, LocalDate.of(2026, 7, 8));
    }

    @Test
    void learningStartDateCannotMoveAfterEarliestValidAttendance() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(
                        history(1L, EnrollmentStatus.ACTIVE, LocalDate.of(2026, 7, 1), null)
                ));
        when(attendanceRepository.findEarliestValidAttendanceDate(1L, 2L))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 6)));

        assertThatThrownBy(() -> service.validateLearningStartDateChange(enrollment, LocalDate.of(2026, 7, 8)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentEligibilityService.learningStartAfterAttendanceMessage(
                        LocalDate.of(2026, 7, 6)
                ));
        verify(attendanceRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(attendanceRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void learningStartDateCanEqualEarliestValidAttendance() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(
                        history(1L, EnrollmentStatus.ACTIVE, LocalDate.of(2026, 7, 1), null)
                ));
        when(attendanceRepository.findEarliestValidAttendanceDate(1L, 2L))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 6)));
        when(classSessionRepository.countByClassroomId(2L)).thenReturn(0);

        service.validateLearningStartDateChange(enrollment, LocalDate.of(2026, 7, 6));
    }

    @Test
    void learningStartDateCannotReachOrPassFirstInactivePeriod() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(
                        history(1L, EnrollmentStatus.ACTIVE, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 11)),
                        history(2L, EnrollmentStatus.STOPPED, LocalDate.of(2026, 7, 11), null)
                ));

        assertThatThrownBy(() -> service.validateLearningStartDateChange(enrollment, LocalDate.of(2026, 7, 13)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentEligibilityService.learningStartBeforeInactiveMessage(
                        LocalDate.of(2026, 7, 11)
                ));
    }

    @Test
    void reactivateOnExistingSessionDateIsInclusive() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        EnrollmentStatusHistory stopped = history(
                2L,
                EnrollmentStatus.STOPPED,
                LocalDate.of(2026, 7, 11),
                null
        );
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(
                        history(1L, EnrollmentStatus.ACTIVE, LocalDate.of(2026, 7, 1), LocalDate.of(2026, 7, 11)),
                        stopped
                ));
        when(classSessionRepository.countByClassroomId(2L)).thenReturn(10);
        when(classSessionRepository.existsByClassroomIdAndSessionDateAndStatusNot(
                2L,
                LocalDate.of(2026, 7, 25),
                ClassSessionStatus.CANCELED
        )).thenReturn(true);

        service.validateReactivateEffectiveDate(enrollment, LocalDate.of(2026, 7, 25));
    }

    @Test
    void reactivateRejectsDateOnOrBeforeInactiveStart() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(history(
                        2L,
                        EnrollmentStatus.STOPPED,
                        LocalDate.of(2026, 7, 11),
                        null
                )));

        assertThatThrownBy(() -> service.validateReactivateEffectiveDate(
                enrollment,
                LocalDate.of(2026, 7, 11)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentEligibilityService.REACTIVATE_BEFORE_INACTIVE_MESSAGE);
    }

    @Test
    void reactivateRejectsDateThatIsNotAClassStudyDate() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        enrollment.setStatus(EnrollmentStatus.STOPPED);
        when(statusHistoryRepository.findByEnrollmentIdOrderByEffectiveFromAscIdAsc(10L))
                .thenReturn(List.of(history(
                        2L,
                        EnrollmentStatus.STOPPED,
                        LocalDate.of(2026, 7, 11),
                        null
                )));
        when(classSessionRepository.countByClassroomId(2L)).thenReturn(10);
        when(classSessionRepository.existsByClassroomIdAndSessionDateAndStatusNot(
                2L,
                LocalDate.of(2026, 7, 24),
                ClassSessionStatus.CANCELED
        )).thenReturn(false);

        assertThatThrownBy(() -> service.validateReactivateEffectiveDate(
                enrollment,
                LocalDate.of(2026, 7, 24)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(EnrollmentEligibilityService.REACTIVATE_INVALID_DATE_MESSAGE);
    }

    @Test
    void validateInactiveDoesNotTouchAttendanceRows() {
        EnrollmentEligibilityService service = newService();
        Enrollment enrollment = enrollment();
        when(attendanceRepository.findLatestValidAttendanceDate(1L, 2L, LocalDate.of(2026, 7, 1)))
                .thenReturn(Optional.of(LocalDate.of(2026, 7, 10)));

        service.validateInactiveEffectiveDate(enrollment, LocalDate.of(2026, 7, 11));

        verify(attendanceRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(attendanceRepository, never()).delete(org.mockito.ArgumentMatchers.any());
    }

    private EnrollmentEligibilityService newService() {
        return new EnrollmentEligibilityService(
                enrollmentRepository,
                statusHistoryRepository,
                attendanceRepository,
                classSessionRepository
        );
    }

    private Enrollment enrollment() {
        Student student = new Student();
        student.setId(1L);

        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setStartDate(LocalDate.of(2026, 7, 1));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY));

        Enrollment enrollment = new Enrollment();
        enrollment.setId(10L);
        enrollment.setStudent(student);
        enrollment.setClassroom(classroom);
        enrollment.setStartDate(LocalDate.of(2026, 7, 1));
        enrollment.setStatus(EnrollmentStatus.ACTIVE);
        return enrollment;
    }

    private EnrollmentStatusHistory history(
            Long id,
            EnrollmentStatus status,
            LocalDate effectiveFrom,
            LocalDate effectiveTo
    ) {
        EnrollmentStatusHistory history = new EnrollmentStatusHistory();
        history.setId(id);
        history.setStatus(status);
        history.setEffectiveFrom(effectiveFrom);
        history.setEffectiveTo(effectiveTo);
        return history;
    }
}
