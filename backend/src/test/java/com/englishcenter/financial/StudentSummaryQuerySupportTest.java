package com.englishcenter.financial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;

class StudentSummaryQuerySupportTest {
    @Test
    void matchesKeywordIgnoresBlankAndComparesCaseInsensitively() {
        assertThat(StudentSummaryQuerySupport.matchesKeyword("  ", "ST00012")).isTrue();
        assertThat(StudentSummaryQuerySupport.matchesKeyword("han", "ST00012", "Duong My Han", "0909")).isTrue();
        assertThat(StudentSummaryQuerySupport.matchesKeyword("0909", "ST00012", "Duong My Han", "0909123456")).isTrue();
        assertThat(StudentSummaryQuerySupport.matchesKeyword("xyz", "ST00012", "Duong My Han", "0909")).isFalse();
    }

    @Test
    void paginateReturnsRequestedSlice() {
        Page<Integer> page = StudentSummaryQuerySupport.paginate(List.of(1, 2, 3, 4, 5), 1, 2, 100);

        assertThat(page.getContent()).containsExactly(3, 4);
        assertThat(page.getTotalElements()).isEqualTo(5);
        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
    }
}
