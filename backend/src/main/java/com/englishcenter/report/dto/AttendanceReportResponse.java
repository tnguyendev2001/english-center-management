package com.englishcenter.report.dto;

import java.util.List;

public record AttendanceReportResponse(
        AttendanceReportSummary summary,
        List<AttendanceReportItemResponse> items
) {
}
