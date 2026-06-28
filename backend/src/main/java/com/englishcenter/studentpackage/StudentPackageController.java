package com.englishcenter.studentpackage;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.studentpackage.dto.StudentPackageProgressResponse;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class StudentPackageController {
    private final StudentPackageService studentPackageService;

    public StudentPackageController(StudentPackageService studentPackageService) {
        this.studentPackageService = studentPackageService;
    }

    @GetMapping("/api/students/{studentId}/packages")
    public ApiResponse<List<StudentPackageProgressResponse>> getByStudentId(@PathVariable Long studentId) {
        return ApiResponse.success(studentPackageService.getByStudentId(studentId));
    }

    @GetMapping("/api/classrooms/{classroomId}/student-packages")
    public ApiResponse<List<StudentPackageProgressResponse>> getByClassroomId(@PathVariable Long classroomId) {
        return ApiResponse.success(studentPackageService.getByClassroomId(classroomId));
    }
}
