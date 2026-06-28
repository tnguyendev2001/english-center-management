package com.englishcenter.enrollment;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.common.exception.BusinessException;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.Set;

public final class EnrollmentLearningDateHelper {
    private static final int MAX_SCAN_DAYS = 366;

    private EnrollmentLearningDateHelper() {
    }

    public static boolean isValidLearningDate(Classroom classroom, LocalDate date) {
        if (date == null) {
            return false;
        }

        if (date.isBefore(classroom.getStartDate())) {
            return false;
        }

        Set<DayOfWeek> studyDays = ClassDayOfWeek.toJavaDayOfWeekSet(classroom.getDaysOfWeek());
        if (studyDays.isEmpty()) {
            return false;
        }

        return studyDays.contains(date.getDayOfWeek());
    }

    public static LocalDate findFirstValidLearningDate(Classroom classroom, LocalDate requestedDate) {
        validateClassroomStudyDays(classroom);

        if (requestedDate != null && isValidLearningDate(classroom, requestedDate)) {
            return requestedDate;
        }

        LocalDate cursor = requestedDate != null && requestedDate.isAfter(classroom.getStartDate())
                ? requestedDate
                : classroom.getStartDate();

        for (int i = 0; i < MAX_SCAN_DAYS; i++) {
            if (isValidLearningDate(classroom, cursor)) {
                return cursor;
            }
            cursor = cursor.plusDays(1);
        }

        throw new BusinessException("No valid learning date found within one year");
    }

    private static void validateClassroomStudyDays(Classroom classroom) {
        if (classroom.getDaysOfWeek() == null || classroom.getDaysOfWeek().isEmpty()) {
            throw new BusinessException("Classroom days of week is not configured");
        }
    }
}
