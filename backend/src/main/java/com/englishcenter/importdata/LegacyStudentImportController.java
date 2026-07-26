package com.englishcenter.importdata;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.importdata.dto.LegacyImportConfirmResponse;
import com.englishcenter.importdata.dto.LegacyImportPreviewResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/imports/legacy-students")
public class LegacyStudentImportController {
    private final LegacyStudentImportService legacyStudentImportService;

    public LegacyStudentImportController(LegacyStudentImportService legacyStudentImportService) {
        this.legacyStudentImportService = legacyStudentImportService;
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<LegacyImportPreviewResponse> preview(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tuitionPackageId") Long tuitionPackageId
    ) {
        return ApiResponse.success(legacyStudentImportService.preview(file, tuitionPackageId));
    }

    @PostMapping(value = "/confirm", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<LegacyImportConfirmResponse> confirm(
            @RequestParam("file") MultipartFile file,
            @RequestParam("tuitionPackageId") Long tuitionPackageId
    ) {
        return ApiResponse.success(legacyStudentImportService.confirm(file, tuitionPackageId));
    }
}
