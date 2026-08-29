package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.studentpackage.StudentPackage;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class TuitionPeriodResolverTest {
    private final TuitionPeriodResolver resolver = new TuitionPeriodResolver();

    @Test
    void usesCalculatedPackagePeriod() {
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setCalculatedPeriodStartDate(LocalDate.of(2026, 8, 7));
        studentPackage.setCalculatedPeriodEndDate(LocalDate.of(2026, 8, 31));
        Invoice invoice = new Invoice();
        invoice.setStudentPackage(studentPackage);

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 7));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 8, 31));
    }

    @Test
    void manualStartOverridesCalculatedStartWithoutChangingEnd() {
        StudentPackage studentPackage = new StudentPackage();
        studentPackage.setCalculatedPeriodStartDate(LocalDate.of(2026, 8, 7));
        studentPackage.setCalculatedPeriodEndDate(LocalDate.of(2026, 9, 1));
        studentPackage.setManualPeriodStartDate(LocalDate.of(2026, 8, 10));
        Invoice invoice = new Invoice();
        invoice.setStudentPackage(studentPackage);

        TuitionPeriodResolver.TuitionPeriod period = resolver.resolve(invoice);

        assertThat(period.start()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(period.end()).isEqualTo(LocalDate.of(2026, 9, 1));
    }
}
