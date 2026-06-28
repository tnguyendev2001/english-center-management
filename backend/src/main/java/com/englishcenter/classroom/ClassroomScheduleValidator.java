package com.englishcenter.classroom;

import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

public final class ClassroomScheduleValidator {
    public static final String START_DATE_MUST_MATCH_DAYS_MESSAGE =
            "Ngày bắt đầu lớp phải trùng với lịch học đã chọn.";
    public static final String DAYS_OF_WEEK_REQUIRED_MESSAGE = "Vui lòng chọn ít nhất một ngày học.";

    private ClassroomScheduleValidator() {
    }

    public static void validateSchedule(
            LocalDate startDate,
            LocalDate expectedEndDate,
            Set<ClassDayOfWeek> daysOfWeek,
            LocalTime startTime,
            LocalTime endTime
    ) {
        if (daysOfWeek == null || daysOfWeek.isEmpty()) {
            throw new BusinessException(DAYS_OF_WEEK_REQUIRED_MESSAGE);
        }

        if (startDate != null && !ClassDayOfWeek.isDateMatchingDaysOfWeek(startDate, daysOfWeek)) {
            throw new BusinessException(START_DATE_MUST_MATCH_DAYS_MESSAGE);
        }

        if (expectedEndDate != null && startDate != null && expectedEndDate.isBefore(startDate)) {
            throw new BusinessException("Ngày kết thúc dự kiến không được trước ngày bắt đầu.");
        }

        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new BusinessException("Giờ kết thúc phải sau giờ bắt đầu.");
        }
    }
}
