package com.englishcenter.enrollment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.classroom.Classroom;
import com.englishcenter.classroom.ClassroomScheduleValidator;
import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EnrollmentLearningDateHelperTest {
    @Test
    void findFirstValidLearningDateReturnsRequestedDateWhenValid() {
        Classroom classroom = classroom(
                LocalDate.of(2026, 6, 30),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        );

        LocalDate result = EnrollmentLearningDateHelper.findFirstValidLearningDate(
                classroom,
                LocalDate.of(2026, 7, 1)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void findFirstValidLearningDateSkipsClassroomStartDateWhenItDoesNotMatchStudyDays() {
        Classroom classroom = classroom(
                LocalDate.of(2026, 6, 30),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        );

        LocalDate result = EnrollmentLearningDateHelper.findFirstValidLearningDate(
                classroom,
                LocalDate.of(2026, 6, 30)
        );

        assertThat(result).isEqualTo(LocalDate.of(2026, 7, 1));
    }

    @Test
    void isValidLearningDateRejectsDateBeforeClassroomStartDate() {
        Classroom classroom = classroom(
                LocalDate.of(2026, 6, 30),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        );

        assertThat(EnrollmentLearningDateHelper.isValidLearningDate(
                classroom,
                LocalDate.of(2026, 6, 29)
        )).isFalse();
    }

    @Test
    void isValidLearningDateRejectsDateThatDoesNotMatchStudyDays() {
        Classroom classroom = classroom(
                LocalDate.of(2026, 6, 30),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        );

        assertThat(EnrollmentLearningDateHelper.isValidLearningDate(
                classroom,
                LocalDate.of(2026, 6, 30)
        )).isFalse();
    }

    @Test
    void findFirstValidLearningDateThrowsWhenStudyDaysMissing() {
        Classroom classroom = classroom(LocalDate.of(2026, 6, 30), Set.of());

        assertThatThrownBy(() -> EnrollmentLearningDateHelper.findFirstValidLearningDate(
                classroom,
                LocalDate.of(2026, 6, 30)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage(ClassroomScheduleValidator.DAYS_OF_WEEK_REQUIRED_MESSAGE);
    }

    private Classroom classroom(LocalDate startDate, Set<ClassDayOfWeek> daysOfWeek) {
        Classroom classroom = new Classroom();
        classroom.setStartDate(startDate);
        classroom.setDaysOfWeek(daysOfWeek);
        return classroom;
    }
}
