package com.englishcenter.importdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.classroom.ClassDayOfWeek;
import org.junit.jupiter.api.Test;

class LegacyImportNormalizerTest {
    @Test
    void normalizePersonNameCollapsesSpacesAndPreservesVietnamese() {
        assertThat(LegacyImportNormalizer.normalizePersonName("  Nguyễn   Văn   A  "))
                .isEqualTo("Nguyễn Văn A");
    }

    @Test
    void normalizePhoneRemovesNoiseAndKeepsLeadingZero() {
        assertThat(LegacyImportNormalizer.normalizePhone("0901 234-567"))
                .isEqualTo("0901234567");
        assertThat(LegacyImportNormalizer.normalizePhone("0901.234.567"))
                .isEqualTo("0901234567");
    }

    @Test
    void toClassCodeIsStableAndUppercase() {
        assertThat(LegacyImportNormalizer.toClassCode("GRADE 4-A")).isEqualTo("GRADE4-A");
    }

    @Test
    void parseDaysOfWeekSupportsVietnameseAndEnglishTokens() {
        assertThat(LegacyImportNormalizer.parseDaysOfWeek("T2, T4"))
                .containsExactlyInAnyOrder(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY);
        assertThat(LegacyImportNormalizer.parseDaysOfWeek("MONDAY,FRIDAY"))
                .containsExactlyInAnyOrder(ClassDayOfWeek.MONDAY, ClassDayOfWeek.FRIDAY);
    }
}
