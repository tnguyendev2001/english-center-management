package com.englishcenter.invoice;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class BillingCycleLabelServiceTest {
    private final BillingCycleLabelService service = new BillingCycleLabelService();

    @Test
    void buildsPackageCycleLabel() {
        assertThat(service.buildBillingLabel("Gói 8 buổi", 2))
                .isEqualTo("Gói 8 buổi - Kỳ 2");
    }

    @Test
    void buildsUnknownCycleLabel() {
        assertThat(service.buildBillingLabel("Gói 8 buổi", null))
                .isEqualTo("Gói 8 buổi - Kỳ chưa xác định");
    }
}
