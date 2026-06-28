package com.englishcenter.classroom;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

public enum ClassDayOfWeek {
    MONDAY,
    TUESDAY,
    WEDNESDAY,
    THURSDAY,
    FRIDAY,
    SATURDAY,
    SUNDAY;

    public DayOfWeek toJavaDayOfWeek() {
        return DayOfWeek.valueOf(name());
    }

    public static Set<DayOfWeek> toJavaDayOfWeekSet(Set<ClassDayOfWeek> days) {
        if (days == null || days.isEmpty()) {
            return Set.of();
        }

        return days.stream()
                .map(ClassDayOfWeek::toJavaDayOfWeek)
                .collect(Collectors.toCollection(HashSet::new));
    }

    public static boolean isDateMatchingDaysOfWeek(LocalDate date, Set<ClassDayOfWeek> daysOfWeek) {
        if (date == null || daysOfWeek == null || daysOfWeek.isEmpty()) {
            return false;
        }

        DayOfWeek dayOfWeek = date.getDayOfWeek();
        return daysOfWeek.stream()
                .map(ClassDayOfWeek::toJavaDayOfWeek)
                .anyMatch(dayOfWeek::equals);
    }
}
