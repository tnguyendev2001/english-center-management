package com.englishcenter.classroom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.englishcenter.common.exception.BusinessException;
import java.time.LocalDate;
import java.util.Set;
import org.junit.jupiter.api.Test;

class ClassDayOfWeekTest {
    @Test
    void isDateMatchingDaysOfWeekReturnsTrueForMatchingDate() {
        assertThat(ClassDayOfWeek.isDateMatchingDaysOfWeek(
                LocalDate.of(2026, 7, 1),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        )).isTrue();
    }

    @Test
    void isDateMatchingDaysOfWeekReturnsFalseForNonMatchingDate() {
        assertThat(ClassDayOfWeek.isDateMatchingDaysOfWeek(
                LocalDate.of(2026, 6, 30),
                Set.of(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY)
        )).isFalse();
    }
}
