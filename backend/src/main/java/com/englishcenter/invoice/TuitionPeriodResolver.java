package com.englishcenter.invoice;

import com.englishcenter.studentpackage.StudentPackage;
import java.time.LocalDate;
import org.springframework.stereotype.Service;

@Service
public class TuitionPeriodResolver {
    public TuitionPeriod resolve(Invoice invoice) {
        StudentPackage studentPackage = invoice.getStudentPackage();
        return new TuitionPeriod(
                studentPackage.getEffectivePeriodStartDate(),
                studentPackage.getCalculatedPeriodEndDate()
        );
    }

    public record TuitionPeriod(LocalDate start, LocalDate end) {
    }
}
