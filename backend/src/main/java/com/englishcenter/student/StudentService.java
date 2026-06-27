package com.englishcenter.student;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.student.dto.StudentCreateRequest;
import com.englishcenter.student.dto.StudentResponse;
import com.englishcenter.student.dto.StudentUpdateRequest;
import com.englishcenter.student.mapper.StudentMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StudentService {
    private static final int MAX_PAGE_SIZE = 100;

    private final StudentRepository studentRepository;
    private final StudentMapper studentMapper;

    public StudentService(StudentRepository studentRepository, StudentMapper studentMapper) {
        this.studentRepository = studentRepository;
        this.studentMapper = studentMapper;
    }

    @Transactional(readOnly = true)
    public Page<StudentResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<Student> students = isBlank(keyword)
                ? studentRepository.findAll(pageable)
                : studentRepository.search(keyword.trim(), pageable);

        return students.map(studentMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public StudentResponse getById(Long id) {
        return studentMapper.toResponse(findStudent(id));
    }

    @Transactional
    public StudentResponse create(StudentCreateRequest request) {
        String studentCode = request.studentCode().trim();
        if (studentRepository.existsByStudentCode(studentCode)) {
            throw new BusinessException("Student code already exists");
        }

        Student student = studentMapper.toEntity(request);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    @Transactional
    public StudentResponse update(Long id, StudentUpdateRequest request) {
        Student student = findStudent(id);
        String studentCode = request.studentCode().trim();

        if (studentRepository.existsByStudentCodeAndIdNot(studentCode, id)) {
            throw new BusinessException("Student code already exists");
        }

        studentMapper.updateEntity(student, request);
        return studentMapper.toResponse(studentRepository.save(student));
    }

    private Student findStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Student not found"));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
