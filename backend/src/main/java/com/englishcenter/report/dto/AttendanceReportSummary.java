package com.englishcenter.report.dto;

public record AttendanceReportSummary(
        long totalCount,
        long presentCount,
        long absentCount,
        long excusedCount,
        double presentRate
) {
}
