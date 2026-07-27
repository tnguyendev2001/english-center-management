package com.englishcenter.classroom;

import com.englishcenter.auth.AccountRole;
import com.englishcenter.auth.security.AccountPrincipal;
import com.englishcenter.classroom.dto.AssignTeacherRequest;
import com.englishcenter.classroom.dto.ClassroomCreateRequest;
import com.englishcenter.classroom.dto.ClassroomRenewalCandidateResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalConfirmResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalPreviewResponse;
import com.englishcenter.classroom.dto.ClassroomRenewalRequest;
import com.englishcenter.classroom.dto.ClassroomResponse;
import com.englishcenter.classroom.dto.ClassroomUpdateRequest;
import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.enrollment.EnrollmentService;
import com.englishcenter.security.SecurityUtils;
import com.englishcenter.student.dto.StudentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/classrooms")
public class ClassroomController {
    private final ClassroomService classroomService;
    private final ClassroomRenewalService classroomRenewalService;
    private final EnrollmentService enrollmentService;

    public ClassroomController(
            ClassroomService classroomService,
            ClassroomRenewalService classroomRenewalService,
            EnrollmentService enrollmentService
    ) {
        this.classroomService = classroomService;
        this.classroomRenewalService = classroomRenewalService;
        this.enrollmentService = enrollmentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    public ApiResponse<List<ClassroomResponse>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long teacherId,
            @RequestParam(required = false) Boolean unassignedOnly,
            @RequestParam(required = false) Boolean assignedOnly,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        AccountPrincipal principal = SecurityUtils.requirePrincipal();
        if (principal.role() == AccountRole.TEACHER) {
            List<ClassroomResponse> mine = classroomService.listByTeacherId(principal.teacherId());
            return ApiResponse.success(mine, new PageMeta(0, mine.size(), mine.size(), 1));
        }

        Page<ClassroomResponse> classrooms = classroomService.search(
                keyword,
                teacherId,
                unassignedOnly,
                assignedOnly,
                page,
                size
        );
        PageMeta meta = new PageMeta(
                classrooms.getNumber(),
                classrooms.getSize(),
                classrooms.getTotalElements(),
                classrooms.getTotalPages()
        );
        return ApiResponse.success(classrooms.getContent(), meta);
    }

    @GetMapping("/unassigned")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ClassroomResponse>> unassigned() {
        return ApiResponse.success(classroomService.listUnassigned());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassroomResponse> create(@Valid @RequestBody ClassroomCreateRequest request) {
        return ApiResponse.success(classroomService.create(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@authorizationService.canAccessClassroom(authentication, #id)")
    public ApiResponse<ClassroomResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(classroomService.getById(id));
    }

    @GetMapping("/{id}/eligible-students")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<StudentResponse>> getEligibleStudents(@PathVariable Long id) {
        return ApiResponse.success(enrollmentService.getEligibleStudents(id));
    }

    @GetMapping("/{id}/renewal-candidates")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<ClassroomRenewalCandidateResponse>> getRenewalCandidates(
            @PathVariable Long id,
            @RequestParam(defaultValue = "2") int remainingThreshold
    ) {
        return ApiResponse.success(classroomRenewalService.getCandidates(id, remainingThreshold));
    }

    @PostMapping("/{id}/renewals/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassroomRenewalPreviewResponse> previewRenewals(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomRenewalRequest request
    ) {
        return ApiResponse.success(classroomRenewalService.preview(id, request));
    }

    @PostMapping("/{id}/renewals/confirm")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassroomRenewalConfirmResponse> confirmRenewals(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomRenewalRequest request
    ) {
        return ApiResponse.success(classroomRenewalService.confirm(id, request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassroomResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody ClassroomUpdateRequest request
    ) {
        return ApiResponse.success(classroomService.update(id, request));
    }

    @PatchMapping("/{id}/teacher")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<ClassroomResponse> assignTeacher(
            @PathVariable Long id,
            @Valid @RequestBody AssignTeacherRequest request
    ) {
        return ApiResponse.success(classroomService.assignTeacher(id, request));
    }
}
