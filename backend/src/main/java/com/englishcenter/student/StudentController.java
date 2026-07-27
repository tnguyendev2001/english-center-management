package com.englishcenter.student;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.UserAccountRepository;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.enrollment.EnrollmentRepository;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.dto.StudentCreateRequest;
import com.englishcenter.student.dto.StudentResponse;
import com.englishcenter.student.dto.StudentUpdateRequest;
import jakarta.validation.Valid;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/students")
public class StudentController {
    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final UserAccountRepository userAccountRepository;
    private final EnrollmentRepository enrollmentRepository;

    public StudentController(
            StudentService studentService,
            StudentRepository studentRepository,
            UserAccountRepository userAccountRepository,
            EnrollmentRepository enrollmentRepository
    ) {
        this.studentService = studentService;
        this.studentRepository = studentRepository;
        this.userAccountRepository = userAccountRepository;
        this.enrollmentRepository = enrollmentRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<StudentResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.TEACHER) {
            LinkedHashMap<Long, StudentResponse> students = new LinkedHashMap<>();
            enrollmentRepository.findByClassroomTeacherId(principal.teacherId()).forEach(enrollment -> {
                Long studentId = enrollment.getStudent().getId();
                students.computeIfAbsent(studentId, studentService::getById);
            });
            List<StudentResponse> content = List.copyOf(students.values());
            return ApiResponse.success(content, new PageMeta(0, content.size(), content.size(), 1));
        }

        Page<StudentResponse> students = studentService.search(keyword, page, size);
        PageMeta meta = new PageMeta(
                students.getNumber(),
                students.getSize(),
                students.getTotalElements(),
                students.getTotalPages()
        );

        return ApiResponse.success(students.getContent(), meta);
    }

    @GetMapping("/without-account")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StudentResponse>> withoutAccount() {
        List<StudentResponse> students = studentRepository.findAll().stream()
                .filter(student -> !userAccountRepository.existsByStudentId(student.getId()))
                .map(student -> studentService.getById(student.getId()))
                .toList();
        return ApiResponse.success(students);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> create(@Valid @RequestBody StudentCreateRequest request) {
        return ApiResponse.success(studentService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessStudent(authentication, #id)")
    public ApiResponse<StudentResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(studentService.getById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<StudentResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody StudentUpdateRequest request
    ) {
        return ApiResponse.success(studentService.update(id, request));
    }
}
