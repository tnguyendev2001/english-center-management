package com.englishcenter.finance;

import com.englishcenter.common.config.FinanceProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.FinanceConfigResponse;
import com.englishcenter.settings.SystemSettingRepository;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceConfigService {
    public static final String FINANCE_START_DATE_KEY = "finance.start_date";

    private final SystemSettingRepository systemSettingRepository;
    private final FinanceProperties financeProperties;
    private final FinancePeriodRangeService financePeriodRangeService;

    public FinanceConfigService(
            SystemSettingRepository systemSettingRepository,
            FinanceProperties financeProperties,
            FinancePeriodRangeService financePeriodRangeService
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.financeProperties = financeProperties;
        this.financePeriodRangeService = financePeriodRangeService;
    }

    @Transactional(readOnly = true)
    public LocalDate getFinanceStartDate() {
        return systemSettingRepository.findBySettingKey(FINANCE_START_DATE_KEY)
                .map(setting -> parseDate(setting.getSettingValue()))
                .orElseGet(financeProperties::getFinanceStartDate);
    }

    @Transactional(readOnly = true)
    public FinanceConfigResponse getConfig() {
        return financePeriodRangeService.toConfigResponse();
    }

    public void validateMonthInFinanceRange(int year, int month) {
        financePeriodRangeService.validateMonthSelectable(year, month);
    }

    public void validatePeriodNotBeforeFinanceStart(YearMonth period, String message) {
        YearMonth start = YearMonth.from(getFinanceStartDate());
        if (period.isBefore(start)) {
            throw new BusinessException(
                    message != null
                            ? message
                            : "Kỳ so sánh nằm trước ngày bắt đầu theo dõi tài chính."
            );
        }
    }

    public LocalDate resolvePeriodStart(int year, int month) {
        return financePeriodRangeService.resolvePeriodStart(year, month);
    }

    public LocalDate resolvePeriodEnd(int year, int month) {
        return financePeriodRangeService.resolvePeriodEnd(year, month);
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid finance.start_date setting: " + value);
        }
    }
}
