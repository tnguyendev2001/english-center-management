package com.englishcenter.classsession.dto;

import java.util.List;

public record GenerateClassSessionsResponse(
        int createdCount,
        int skippedCount,
        List<ClassSessionResponse> sessions
) {
}
