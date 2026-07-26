package com.englishcenter.common.config;

import java.time.LocalDate;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.finance")
public class FinanceProperties {
    /**
     * YAML/env fallback when system_settings.finance.start_date is missing.
     */
    private LocalDate financeStartDate = LocalDate.of(2026, 5, 1);

    public LocalDate getFinanceStartDate() {
        return financeStartDate;
    }

    public void setFinanceStartDate(LocalDate financeStartDate) {
        this.financeStartDate = financeStartDate;
    }
}
