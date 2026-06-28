package com.englishcenter.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.enrollment.EnrollmentSessionService;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import com.englishcenter.classroom.mapper.ClassroomMapper;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassroomServiceTest {
    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private EnrollmentRepository enrollmentRepository;

    @Mock
    private ClassroomScheduleUpdateService classroomScheduleUpdateService;

    private final ClassroomMapper classroomMapper = new ClassroomMapper();
    private final EnrollmentSessionService enrollmentSessionService = new EnrollmentSessionService();

    @Test
    void createRejectsDuplicateClassCode() {
        ClassroomService classroomService = newService();
        ClassroomCreateRequest request = validCreateRequest();

        when(classroomRepository.existsByClassCode("CLS001")).thenReturn(true);

        assertThatThrownBy(() -> classroomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class code already exists");

        verify(classroomRepository).existsByClassCode("CLS001");
    }

    @Test
    void createSavesClassroom() {
        ClassroomService classroomService = newService();
        ClassroomCreateRequest request = validCreateRequest();

        when(classroomRepository.existsByClassCode("CLS001")).thenReturn(false);
        when(classroomRepository.save(any(Classroom.class))).thenAnswer(invocation -> {
            Classroom classroom = invocation.getArgument(0);
            classroom.setId(1L);
            return classroom;
        });

        ClassroomResponse response = classroomService.create(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.classCode()).isEqualTo("CLS001");
        assertThat(response.className()).isEqualTo("Starter A");
        assertThat(response.status()).isEqualTo(ClassroomStatus.PLANNED);
        assertThat(response.daysOfWeek()).containsExactly(
                ClassDayOfWeek.MONDAY,
                ClassDayOfWeek.WEDNESDAY
        );
        verify(classroomRepository).save(any(Classroom.class));
    }

    @Test
    void updateRejectsDuplicateClassCode() {
        ClassroomService classroomService = newService();
        Classroom classroom = existingClassroom();
        ClassroomUpdateRequest request = validUpdateRequest();

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassCodeAndIdNot("CLS002", 1L)).thenReturn(true);

        assertThatThrownBy(() -> classroomService.update(1L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Class code already exists");

        verify(classroomRepository).findById(1L);
        verify(classroomRepository).existsByClassCodeAndIdNot("CLS002", 1L);
    }

    @Test
    void updateSavesClassroom() {
        ClassroomService classroomService = newService();
        Classroom classroom = existingClassroom();
        ClassroomUpdateRequest request = validUpdateRequest();

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(classroomRepository.existsByClassCodeAndIdNot("CLS002", 1L)).thenReturn(false);
        when(classroomRepository.save(classroom)).thenReturn(classroom);

        ClassroomResponse response = classroomService.update(1L, request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.classCode()).isEqualTo("CLS002");
        assertThat(response.className()).isEqualTo("Starter B");
        assertThat(response.status()).isEqualTo(ClassroomStatus.ONGOING);
        assertThat(response.daysOfWeek()).containsExactly(
                ClassDayOfWeek.TUESDAY,
                ClassDayOfWeek.THURSDAY
        );
        verify(classroomRepository).save(classroom);
    }

    @Test
    void createRejectsStartDateNotMatchingDaysOfWeek() {
        ClassroomService classroomService = newService();
        ClassroomCreateRequest request = new ClassroomCreateRequest(
                "CLS001",
                "Starter A",
                "Starter",
                "Ms Hoa",
                "Room 1",
                LocalDate.of(2026, 6, 30),
                LocalDate.of(2026, 9, 1),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                ClassroomStatus.PLANNED,
                null
        );

        when(classroomRepository.existsByClassCode("CLS001")).thenReturn(false);

        assertThatThrownBy(() -> classroomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ClassroomScheduleValidator.START_DATE_MUST_MATCH_DAYS_MESSAGE);

        verify(classroomRepository, never()).save(any(Classroom.class));
    }

    @Test
    void createRejectsEndTimeBeforeStartTime() {
        ClassroomService classroomService = newService();
        ClassroomCreateRequest request = new ClassroomCreateRequest(
                "CLS001",
                "Starter A",
                "Starter",
                "Ms Hoa",
                "Room 1",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY),
                LocalTime.of(19, 30),
                LocalTime.of(18, 0),
                ClassroomStatus.PLANNED,
                null
        );

        when(classroomRepository.existsByClassCode("CLS001")).thenReturn(false);

        assertThatThrownBy(() -> classroomService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Giờ kết thúc phải sau giờ bắt đầu.");

        verify(classroomRepository).existsByClassCode("CLS001");
        verify(classroomRepository, never()).save(any(Classroom.class));
    }

    private ClassroomCreateRequest validCreateRequest() {
        return new ClassroomCreateRequest(
                "CLS001",
                "Starter A",
                "Starter",
                "Ms Hoa",
                "Room 1",
                LocalDate.of(2026, 7, 1),
                LocalDate.of(2026, 9, 1),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY),
                LocalTime.of(18, 0),
                LocalTime.of(19, 30),
                ClassroomStatus.PLANNED,
                "Evening class"
        );
    }

    private ClassroomService newService() {
        return new ClassroomService(
                classroomRepository,
                classroomMapper,
                enrollmentRepository,
                enrollmentSessionService,
                classroomScheduleUpdateService
        );
    }

    private ClassroomUpdateRequest validUpdateRequest() {
        return new ClassroomUpdateRequest(
                "CLS002",
                "Starter B",
                "Starter",
                "Ms Lan",
                "Room 2",
                LocalDate.of(2026, 7, 2),
                LocalDate.of(2026, 9, 2),
                Set.of(ClassDayOfWeek.TUESDAY, ClassDayOfWeek.THURSDAY),
                LocalTime.of(18, 30),
                LocalTime.of(20, 0),
                ClassroomStatus.ONGOING,
                "Updated class"
        );
    }

    private Classroom existingClassroom() {
        Classroom classroom = new Classroom();
        classroom.setId(1L);
        classroom.setClassCode("CLS001");
        classroom.setClassName("Starter A");
        classroom.setLevel("Starter");
        classroom.setTeacherName("Ms Hoa");
        classroom.setRoom("Room 1");
        classroom.setStartDate(LocalDate.of(2026, 7, 1));
        classroom.setExpectedEndDate(LocalDate.of(2026, 9, 1));
        classroom.setDaysOfWeek(Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY));
        classroom.setStartTime(LocalTime.of(18, 0));
        classroom.setEndTime(LocalTime.of(19, 30));
        classroom.setStatus(ClassroomStatus.PLANNED);
        return classroom;
    }
}
