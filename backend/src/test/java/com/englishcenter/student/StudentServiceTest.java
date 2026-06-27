package com.englishcenter.student;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.student.dto.StudentCreateRequest;
import com.englishcenter.student.mapper.StudentMapper;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {
    @Mock
    private StudentRepository studentRepository;

    @Mock
    private StudentMapper studentMapper;

    @Test
    void createRejectsDuplicateStudentCode() {
        StudentService studentService = new StudentService(studentRepository, studentMapper);
        StudentCreateRequest request = new StudentCreateRequest(
                "STU001",
                "Nguyen Van A",
                LocalDate.of(2015, 1, 1),
                "0900000000",
                "Nguyen Van B",
                "0911111111",
                "Ha Noi",
                StudentStatus.ACTIVE,
                null
        );

        when(studentRepository.existsByStudentCode("STU001")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Student code already exists");

        verify(studentRepository).existsByStudentCode("STU001");
        verifyNoInteractions(studentMapper);
    }
}
