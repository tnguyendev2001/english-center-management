package com.englishcenter.classpackage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.classpackage.dto.AddClassPackageRequest;
import com.englishcenter.classpackage.dto.ClassPackageResponse;
import com.englishcenter.classpackage.mapper.ClassPackageMapper;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomRepository;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.tuitionpackage.TuitionPackage;
import com.englishcenter.tuitionpackage.TuitionPackageRepository;
import com.englishcenter.tuitionpackage.TuitionPackageStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClassPackageServiceTest {
    @Mock
    private ClassPackageRepository classPackageRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private TuitionPackageRepository tuitionPackageRepository;

    private final ClassPackageMapper classPackageMapper = new ClassPackageMapper();

    @Test
    void addPackageLinksActiveTuitionPackageToClassroom() {
        ClassPackageService service = newService();
        Classroom classroom = classroom();
        TuitionPackage tuitionPackage = tuitionPackage(TuitionPackageStatus.ACTIVE);
        AddClassPackageRequest request = new AddClassPackageRequest(2L);

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(tuitionPackageRepository.findById(2L)).thenReturn(Optional.of(tuitionPackage));
        when(classPackageRepository.findByClassroomIdAndTuitionPackageId(1L, 2L))
                .thenReturn(Optional.empty());
        when(classPackageRepository.save(any(ClassPackage.class))).thenAnswer(invocation -> {
            ClassPackage classPackage = invocation.getArgument(0);
            classPackage.setId(10L);
            classPackage.setCreatedAt(LocalDateTime.now());
            classPackage.setUpdatedAt(LocalDateTime.now());
            return classPackage;
        });

        ClassPackageResponse response = service.addPackage(1L, request);

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.classroomId()).isEqualTo(1L);
        assertThat(response.tuitionPackageId()).isEqualTo(2L);
        assertThat(response.packageName()).isEqualTo("8 sessions");
        assertThat(response.active()).isTrue();
        verify(classPackageRepository).save(any(ClassPackage.class));
    }

    @Test
    void addPackageRejectsDuplicateActivePackage() {
        ClassPackageService service = newService();
        Classroom classroom = classroom();
        TuitionPackage tuitionPackage = tuitionPackage(TuitionPackageStatus.ACTIVE);
        ClassPackage classPackage = classPackage(classroom, tuitionPackage, true);

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(tuitionPackageRepository.findById(2L)).thenReturn(Optional.of(tuitionPackage));
        when(classPackageRepository.findByClassroomIdAndTuitionPackageId(1L, 2L))
                .thenReturn(Optional.of(classPackage));

        assertThatThrownBy(() -> service.addPackage(1L, new AddClassPackageRequest(2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tuition package is already linked to this classroom");

        verify(classPackageRepository, never()).save(any(ClassPackage.class));
    }

    @Test
    void addPackageRejectsInactiveTuitionPackage() {
        ClassPackageService service = newService();
        Classroom classroom = classroom();
        TuitionPackage tuitionPackage = tuitionPackage(TuitionPackageStatus.INACTIVE);

        when(classroomRepository.findById(1L)).thenReturn(Optional.of(classroom));
        when(tuitionPackageRepository.findById(2L)).thenReturn(Optional.of(tuitionPackage));

        assertThatThrownBy(() -> service.addPackage(1L, new AddClassPackageRequest(2L)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tuition package must be active");

        verify(classPackageRepository, never()).save(any(ClassPackage.class));
    }

    @Test
    void deactivatePackageSetsActiveFalse() {
        ClassPackageService service = newService();
        Classroom classroom = classroom();
        TuitionPackage tuitionPackage = tuitionPackage(TuitionPackageStatus.ACTIVE);
        ClassPackage classPackage = classPackage(classroom, tuitionPackage, true);

        when(classroomRepository.existsById(1L)).thenReturn(true);
        when(tuitionPackageRepository.existsById(2L)).thenReturn(true);
        when(classPackageRepository.findByClassroomIdAndTuitionPackageId(1L, 2L))
                .thenReturn(Optional.of(classPackage));
        when(classPackageRepository.save(classPackage)).thenReturn(classPackage);

        ClassPackageResponse response = service.deactivatePackage(1L, 2L);

        assertThat(response.active()).isFalse();
        verify(classPackageRepository).save(classPackage);
        verify(classPackageRepository, never()).delete(any(ClassPackage.class));
    }

    @Test
    void getActivePackagesListsPackagesByClassroom() {
        ClassPackageService service = newService();
        Classroom classroom = classroom();
        TuitionPackage tuitionPackage = tuitionPackage(TuitionPackageStatus.ACTIVE);
        ClassPackage classPackage = classPackage(classroom, tuitionPackage, true);

        when(classroomRepository.existsById(1L)).thenReturn(true);
        when(classPackageRepository.findByClassroomIdAndActiveTrueOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(classPackage));

        List<ClassPackageResponse> responses = service.getActivePackages(1L);

        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().classroomId()).isEqualTo(1L);
        assertThat(responses.getFirst().tuitionPackageId()).isEqualTo(2L);
        assertThat(responses.getFirst().active()).isTrue();
    }

    private ClassPackageService newService() {
        return new ClassPackageService(
                classPackageRepository,
                classroomRepository,
                tuitionPackageRepository,
                classPackageMapper
        );
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setId(1L);
        classroom.setClassCode("CLS001");
        classroom.setClassName("Starter A");
        return classroom;
    }

    private TuitionPackage tuitionPackage(TuitionPackageStatus status) {
        TuitionPackage tuitionPackage = new TuitionPackage();
        tuitionPackage.setId(2L);
        tuitionPackage.setName("8 sessions");
        tuitionPackage.setSessionsPerWeek(2);
        tuitionPackage.setTotalSessions(8);
        tuitionPackage.setExpectedMonths(1);
        tuitionPackage.setPrice(new BigDecimal("500000"));
        tuitionPackage.setStatus(status);
        return tuitionPackage;
    }

    private ClassPackage classPackage(Classroom classroom, TuitionPackage tuitionPackage, boolean active) {
        ClassPackage classPackage = new ClassPackage();
        classPackage.setId(10L);
        classPackage.setClassroom(classroom);
        classPackage.setTuitionPackage(tuitionPackage);
        classPackage.setActive(active);
        classPackage.setCreatedAt(LocalDateTime.now());
        classPackage.setUpdatedAt(LocalDateTime.now());
        return classPackage;
    }
}
