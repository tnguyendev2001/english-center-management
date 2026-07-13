package com.englishcenter.student.mapper;

import com.englishcenter.student.Student;
import com.englishcenter.student.dto.StudentCreateRequest;
import com.englishcenter.student.dto.StudentResponse;
import com.englishcenter.student.dto.StudentUpdateRequest;
import org.springframework.stereotype.Component;

@Component
public class StudentMapper {
    public Student toEntity(StudentCreateRequest request) {
        Student student = new Student();
        updateEntity(student, request);
        return student;
    }

    public void updateEntity(Student student, StudentCreateRequest request) {
        student.setFullName(request.fullName().trim());
        student.setDateOfBirth(request.dateOfBirth());
        student.setPhone(trimToNull(request.phone()));
        student.setParentName(trimToNull(request.parentName()));
        student.setParentPhone(trimToNull(request.parentPhone()));
        student.setAddress(trimToNull(request.address()));
        student.setStatus(request.status());
        student.setNote(trimToNull(request.note()));
    }

    public void updateEntity(Student student, StudentUpdateRequest request) {
        student.setFullName(request.fullName().trim());
        student.setDateOfBirth(request.dateOfBirth());
        student.setPhone(trimToNull(request.phone()));
        student.setParentName(trimToNull(request.parentName()));
        student.setParentPhone(trimToNull(request.parentPhone()));
        student.setAddress(trimToNull(request.address()));
        student.setStatus(request.status());
        student.setNote(trimToNull(request.note()));
    }

    public StudentResponse toResponse(Student student) {
        return new StudentResponse(
                student.getId(),
                student.getStudentCode(),
                student.getFullName(),
                student.getDateOfBirth(),
                student.getPhone(),
                student.getParentName(),
                student.getParentPhone(),
                student.getAddress(),
                student.getStatus(),
                student.getNote(),
                student.getCreatedAt(),
                student.getUpdatedAt()
        );
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        return value.trim();
    }
}
