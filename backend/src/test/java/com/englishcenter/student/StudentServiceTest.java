package com.englishcenter.student;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.student.dto.StudentCreateRequest;
import com.englishcenter.student.dto.StudentResponse;
import com.englishcenter.student.mapper.StudentMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @Test
    void createGeneratesStudentCodeFromDatabaseId() {
        StudentService studentService = new StudentService(studentRepository, studentMapper);
        StudentCreateRequest request = new StudentCreateRequest(
                "Nguyen Van A",
                LocalDate.of(2015, 1, 1),
                "0900000000",
                "Nguyen Van B",
                "0911111111",
                "Ha Noi",
                StudentStatus.ACTIVE,
                null
        );

        Student student = new Student();
        student.setFullName("Nguyen Van A");
        student.setStatus(StudentStatus.ACTIVE);

        when(studentMapper.toEntity(request)).thenReturn(student);
        when(studentRepository.saveAndFlush(student)).thenAnswer(invocation -> {
            student.setId(12L);
            student.setCreatedAt(LocalDateTime.now());
            student.setUpdatedAt(LocalDateTime.now());
            return student;
        });
        when(studentRepository.save(student)).thenAnswer(invocation -> invocation.getArgument(0));
        when(studentMapper.toResponse(student)).thenAnswer(invocation -> {
            Student saved = invocation.getArgument(0);
            return new StudentResponse(
                    saved.getId(),
                    saved.getStudentCode(),
                    saved.getFullName(),
                    saved.getDateOfBirth(),
                    saved.getPhone(),
                    saved.getParentName(),
                    saved.getParentPhone(),
                    saved.getAddress(),
                    saved.getStatus(),
                    saved.getNote(),
                    saved.getCreatedAt(),
                    saved.getUpdatedAt()
            );
        });

        StudentResponse response = studentService.create(request);

        ArgumentCaptor<Student> saveCaptor = ArgumentCaptor.forClass(Student.class);
        verify(studentRepository).save(saveCaptor.capture());
        assertThat(saveCaptor.getValue().getStudentCode()).isEqualTo("ST00012");
        assertThat(response.studentCode()).isEqualTo("ST00012");
    }

    @Test
    void formatStudentCodePadsToFiveDigits() {
        assertThat(StudentService.formatStudentCode(1L)).isEqualTo("ST00001");
        assertThat(StudentService.formatStudentCode(25L)).isEqualTo("ST00025");
        assertThat(StudentService.formatStudentCode(99_999L)).isEqualTo("ST99999");
        assertThat(StudentService.formatStudentCode(100_000L)).isEqualTo("ST100000");
    }
}
