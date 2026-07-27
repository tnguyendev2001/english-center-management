package com.englishcenter.invoice;

import com.englishcenter.settings.SystemSettingRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvoiceDueDateService {
    public static final String SETTING_KEY = "invoice.due_days";
    public static final int DEFAULT_DUE_DAYS = 7;

    private final SystemSettingRepository systemSettingRepository;

    public InvoiceDueDateService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @Transactional(readOnly = true)
    public int getInvoiceDueDays() {
        return systemSettingRepository.findBySettingKey(SETTING_KEY)
                .map(setting -> parseDueDays(setting.getSettingValue()))
                .orElse(DEFAULT_DUE_DAYS);
    }

    /**
     * Persisted due date for a newly issued invoice: issueDate + configured due days.
     * Does not use "today" at read time — callers must pass the issue date of the invoice.
     */
    @Transactional(readOnly = true)
    public LocalDate calculateDueDate(LocalDate issueDate) {
        if (issueDate == null) {
            throw new IllegalArgumentException("issueDate is required");
        }
        return issueDate.plusDays(getInvoiceDueDays());
    }

    private int parseDueDays(String value) {
        if (value == null || value.isBlank()) {
            return DEFAULT_DUE_DAYS;
        }
        try {
            int days = Integer.parseInt(value.trim());
            return days >= 0 ? days : DEFAULT_DUE_DAYS;
        } catch (NumberFormatException ex) {
            return DEFAULT_DUE_DAYS;
        }
    }
}
