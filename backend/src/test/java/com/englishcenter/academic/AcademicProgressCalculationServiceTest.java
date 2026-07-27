package com.englishcenter.academic;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.academic.progress.AcademicProgressCalculationService;
import com.englishcenter.academic.score.AssessmentScore;
import com.englishcenter.academic.score.AssessmentScoreStatus;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class AcademicProgressCalculationServiceTest {

    @Test
    void normalizePercentageReturnsEightyForEightOfTen() {
        assertThat(AcademicProgressCalculationService.normalizePercentage(
                new BigDecimal("8"),
                new BigDecimal("10")
        )).isEqualByComparingTo("80.00");
    }

    @Test
    void normalizePercentageReturnsEightyForFortyOfFifty() {
        assertThat(AcademicProgressCalculationService.normalizePercentage(
                new BigDecimal("40"),
                new BigDecimal("50")
        )).isEqualByComparingTo("80.00");
    }

    @Test
    void normalizePercentageReturnsNullForInvalidInputs() {
        assertThat(AcademicProgressCalculationService.normalizePercentage(null, new BigDecimal("10")))
                .isNull();
        assertThat(AcademicProgressCalculationService.normalizePercentage(new BigDecimal("8"), null))
                .isNull();
        assertThat(AcademicProgressCalculationService.normalizePercentage(
                new BigDecimal("8"),
                BigDecimal.ZERO
        )).isNull();
    }

    @Test
    void computeUnweightedAverageReturnsEighty() {
        assertThat(AcademicProgressCalculationService.computeUnweightedAverage(List.of(
                new BigDecimal("80.00"),
                new BigDecimal("80.00")
        ))).isEqualByComparingTo("80.00");
    }

    @Test
    void computeWeightedAverageUsesAssessmentWeights() {
        assertThat(AcademicProgressCalculationService.computeWeightedAverage(
                List.of(new BigDecimal("80.00"), new BigDecimal("60.00")),
                List.of(new BigDecimal("2"), new BigDecimal("1"))
        )).isEqualByComparingTo("73.33");
    }

    @Test
    void isIncludedInAssessmentAverageExcludesExemptAndAbsentWithoutScore() {
        AssessmentScore exempt = new AssessmentScore();
        exempt.setStatus(AssessmentScoreStatus.EXEMPT);

        AssessmentScore absentWithoutScore = new AssessmentScore();
        absentWithoutScore.setStatus(AssessmentScoreStatus.ABSENT);

        AssessmentScore absentWithScore = new AssessmentScore();
        absentWithScore.setStatus(AssessmentScoreStatus.ABSENT);
        absentWithScore.setScore(new BigDecimal("0"));

        AssessmentScore graded = new AssessmentScore();
        graded.setStatus(AssessmentScoreStatus.GRADED);
        graded.setScore(new BigDecimal("8"));

        assertThat(AcademicProgressCalculationService.isIncludedInAssessmentAverage(exempt)).isFalse();
        assertThat(AcademicProgressCalculationService.isIncludedInAssessmentAverage(absentWithoutScore)).isFalse();
        assertThat(AcademicProgressCalculationService.isIncludedInAssessmentAverage(absentWithScore)).isTrue();
        assertThat(AcademicProgressCalculationService.isIncludedInAssessmentAverage(graded)).isTrue();
    }

    @Test
    void computeWeightedAverageReturnsNullWhenNoPositiveWeights() {
        assertThat(AcademicProgressCalculationService.computeWeightedAverage(
                List.of(new BigDecimal("80.00"), new BigDecimal("60.00")),
                Arrays.asList(null, BigDecimal.ZERO)
        )).isNull();

        assertThat(AcademicProgressCalculationService.computeWeightedAverage(
                List.of(new BigDecimal("80.00")),
                List.of(new BigDecimal("-1"))
        )).isNull();
    }

    @Test
    void computeUnweightedAverageReturnsNullForEmptyList() {
        assertThat(AcademicProgressCalculationService.computeUnweightedAverage(List.of())).isNull();
    }
}
