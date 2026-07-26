package com.englishcenter.importdata;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class LegacyImportCycleCalculatorTest {
    @Test
    void packageCyclesExamples() {
        assertThat(LegacyImportCycleCalculator.packageCycles(0)).isEqualTo(1);
        assertThat(LegacyImportCycleCalculator.packageCycles(5)).isEqualTo(1);
        assertThat(LegacyImportCycleCalculator.packageCycles(8)).isEqualTo(1);
        assertThat(LegacyImportCycleCalculator.packageCycles(9)).isEqualTo(2);
        assertThat(LegacyImportCycleCalculator.packageCycles(16)).isEqualTo(2);
        assertThat(LegacyImportCycleCalculator.packageCycles(17)).isEqualTo(3);
        assertThat(LegacyImportCycleCalculator.packageCycles(21)).isEqualTo(3);
    }

    @Test
    void remainingSessionsFor21Eligible() {
        assertThat(LegacyImportCycleCalculator.totalSessions(3)).isEqualTo(24);
        assertThat(LegacyImportCycleCalculator.remainingSessions(3, 21)).isEqualTo(3);
    }

    @Test
    void cycleEffectiveDatesUseEligibleSessionIndexes() {
        List<LocalDate> dates = IntStream.rangeClosed(1, 21)
                .mapToObj(i -> LocalDate.of(2026, 5, 1).plusDays(i - 1))
                .toList();
        LocalDate learningStart = dates.getFirst();

        assertThat(LegacyImportCycleCalculator.cycleEffectiveDate(learningStart, dates, 1))
                .isEqualTo(dates.get(0));
        assertThat(LegacyImportCycleCalculator.cycleEffectiveDate(learningStart, dates, 2))
                .isEqualTo(dates.get(8));
        assertThat(LegacyImportCycleCalculator.cycleEffectiveDate(learningStart, dates, 3))
                .isEqualTo(dates.get(16));
    }
}
