package com.englishcenter.enrollment.dto;

import java.time.LocalDate;

public record EnrollmentLifecycleContextResponse(
        LocalDate latestAttendanceDate,
        LocalDate earliestInactiveDate,
        LocalDate inactiveFrom,
        LocalDate learningStartDate,
        LocalDate earliestValidAttendanceDate,
        LocalDate firstPeriodEndDate
) {
}
