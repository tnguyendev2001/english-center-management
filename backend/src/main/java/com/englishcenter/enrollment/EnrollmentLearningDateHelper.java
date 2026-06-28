package com.englishcenter.enrollment;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomScheduleValidator;
import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;

public final class EnrollmentLearningDateHelper {
    public static final String LEARNING_START_BEFORE_CLASSROOM_MESSAGE =
            "Ngày bắt đầu học không được trước ngày bắt đầu lớp.";
    public static final String LEARNING_START_MUST_MATCH_DAYS_MESSAGE =
            "Ngày bắt đầu học phải trùng với lịch học của lớp.";
    public static final String LEARNING_START_MUST_MATCH_SESSION_MESSAGE =
            "Ngày bắt đầu học phải trùng với một buổi học đã tạo của lớp.";

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

        return ClassDayOfWeek.isDateMatchingDaysOfWeek(date, classroom.getDaysOfWeek());
    }

    public static void validateLearningStartDate(Classroom classroom, LocalDate learningStartDate) {
        if (learningStartDate == null) {
            throw new BusinessException(LEARNING_START_MUST_MATCH_DAYS_MESSAGE);
        }

        if (learningStartDate.isBefore(classroom.getStartDate())) {
            throw new BusinessException(LEARNING_START_BEFORE_CLASSROOM_MESSAGE);
        }

        if (!ClassDayOfWeek.isDateMatchingDaysOfWeek(learningStartDate, classroom.getDaysOfWeek())) {
            throw new BusinessException(LEARNING_START_MUST_MATCH_DAYS_MESSAGE);
        }
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

        throw new BusinessException(LEARNING_START_MUST_MATCH_DAYS_MESSAGE);
    }

    private static void validateClassroomStudyDays(Classroom classroom) {
        if (classroom.getDaysOfWeek() == null || classroom.getDaysOfWeek().isEmpty()) {
            throw new BusinessException(ClassroomScheduleValidator.DAYS_OF_WEEK_REQUIRED_MESSAGE);
        }
    }
}
