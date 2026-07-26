package com.englishcenter.finance;

import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.config.FinanceProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.CompareDefaultsResponse;
import com.englishcenter.finance.dto.FinanceConfigResponse;
import com.englishcenter.finance.dto.FinancePeriodRange;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.settings.SystemSettingRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancePeriodRangeService {
    private static final DateTimeFormatter MONTH_LABEL = DateTimeFormatter.ofPattern("MM/yyyy");

    private final SystemSettingRepository systemSettingRepository;
    private final FinanceProperties financeProperties;
    private final AppTimeProperties appTimeProperties;
    private final CashTransactionRepository cashTransactionRepository;
    private final PaymentRepository paymentRepository;
    private final FinancialPeriodRepository financialPeriodRepository;
    private final Clock clock;

    @Autowired
    public FinancePeriodRangeService(
            SystemSettingRepository systemSettingRepository,
            FinanceProperties financeProperties,
            AppTimeProperties appTimeProperties,
            CashTransactionRepository cashTransactionRepository,
            PaymentRepository paymentRepository,
            FinancialPeriodRepository financialPeriodRepository
    ) {
        this(
                systemSettingRepository,
                financeProperties,
                appTimeProperties,
                cashTransactionRepository,
                paymentRepository,
                financialPeriodRepository,
                Clock.system(appTimeProperties.zoneId())
        );
    }

    /**
     * Test-only constructor with a fixed clock.
     */
    FinancePeriodRangeService(
            SystemSettingRepository systemSettingRepository,
            FinanceProperties financeProperties,
            AppTimeProperties appTimeProperties,
            CashTransactionRepository cashTransactionRepository,
            PaymentRepository paymentRepository,
            FinancialPeriodRepository financialPeriodRepository,
            Clock clock
    ) {
        this.systemSettingRepository = systemSettingRepository;
        this.financeProperties = financeProperties;
        this.appTimeProperties = appTimeProperties;
        this.cashTransactionRepository = cashTransactionRepository;
        this.paymentRepository = paymentRepository;
        this.financialPeriodRepository = financialPeriodRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public LocalDate getBusinessDate() {
        return LocalDate.now(clock.withZone(appTimeProperties.zoneId()));
    }

    @Transactional(readOnly = true)
    public LocalDate getFinanceStartDate() {
        return systemSettingRepository.findBySettingKey(FinanceConfigService.FINANCE_START_DATE_KEY)
                .map(setting -> parseDate(setting.getSettingValue()))
                .orElseGet(financeProperties::getFinanceStartDate);
    }

    @Transactional(readOnly = true)
    public FinancePeriodRange getFinancePeriodRange() {
        LocalDate financeStart = getFinanceStartDate();
        LocalDate businessDate = getBusinessDate();
        LocalDate latestTransactionDate = cashTransactionRepository.findLatestPostedTransactionDate();
        LocalDate latestPaymentDate = paymentRepository.findLatestValidPaymentDate();

        YearMonth latestPeriodMonth = financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()
                .map(period -> YearMonth.of(period.getYear(), period.getMonth()))
                .orElse(null);

        YearMonth businessMonth = YearMonth.from(businessDate);
        YearMonth minMonth = YearMonth.from(financeStart);
        YearMonth maxMonth = Stream.of(
                        businessMonth,
                        latestTransactionDate != null ? YearMonth.from(latestTransactionDate) : null,
                        latestPaymentDate != null ? YearMonth.from(latestPaymentDate) : null,
                        latestPeriodMonth
                )
                .filter(Objects::nonNull)
                .max(YearMonth::compareTo)
                .orElse(businessMonth);

        if (maxMonth.isBefore(minMonth)) {
            maxMonth = minMonth;
        }

        LocalDate latestFinancialDataDate = Stream.of(latestTransactionDate, latestPaymentDate)
                .filter(Objects::nonNull)
                .max(LocalDate::compareTo)
                .orElse(null);
        if (latestPeriodMonth != null) {
            LocalDate periodEnd = latestPeriodMonth.atEndOfMonth();
            if (latestFinancialDataDate == null || periodEnd.isAfter(latestFinancialDataDate)) {
                latestFinancialDataDate = periodEnd;
            }
        }

        return new FinancePeriodRange(
                financeStart,
                businessDate,
                minMonth,
                maxMonth,
                latestTransactionDate,
                latestPaymentDate,
                latestFinancialDataDate
        );
    }

    @Transactional(readOnly = true)
    public FinanceConfigResponse toConfigResponse() {
        FinancePeriodRange range = getFinancePeriodRange();
        return new FinanceConfigResponse(
                range.financeStartDate(),
                range.financeStartDate().getYear(),
                range.financeStartDate().getMonthValue(),
                range.businessDate(),
                range.minSelectableMonthLabel(),
                range.maxSelectableMonthLabel(),
                range.latestTransactionDate(),
                range.latestPaymentDate(),
                range.latestFinancialDataDate(),
                range.hasDataBeyondBusinessMonth()
        );
    }

    @Transactional(readOnly = true)
    public void validateMonthSelectable(int year, int month) {
        YearMonth selected = YearMonth.of(year, month);
        FinancePeriodRange range = getFinancePeriodRange();

        if (selected.isBefore(range.minSelectableMonth())) {
            throw new BusinessException(
                    "Dữ liệu tài chính chỉ được theo dõi từ tháng "
                            + range.minSelectableMonth().format(MONTH_LABEL) + "."
            );
        }
        if (selected.isAfter(range.maxSelectableMonth())) {
            throw new BusinessException("Không thể xem kỳ tài chính trong tương lai.");
        }
    }

    @Transactional(readOnly = true)
    public void validatePaymentNotAfterBusinessDate(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new BusinessException("Payment date is required");
        }
        LocalDate businessDate = getBusinessDate();
        if (paymentDate.isAfter(businessDate)) {
            throw new BusinessException("Ngày thanh toán không được lớn hơn ngày hiện tại.");
        }
    }

    /**
     * Months that contain POSTED transactions, VALID payments, or an existing FinancialPeriod row,
     * clamped to the selectable finance range.
     */
    @Transactional(readOnly = true)
    public List<YearMonth> listMonthsWithData() {
        FinancePeriodRange range = getFinancePeriodRange();
        Set<YearMonth> months = new LinkedHashSet<>();
        for (String value : cashTransactionRepository.findPostedTransactionYearMonths()) {
            months.add(YearMonth.parse(value));
        }
        for (String value : paymentRepository.findValidPaymentYearMonths()) {
            months.add(YearMonth.parse(value));
        }
        for (FinancialPeriod period : financialPeriodRepository.findAll()) {
            months.add(YearMonth.of(period.getYear(), period.getMonth()));
        }
        return months.stream()
                .filter(month -> !month.isBefore(range.minSelectableMonth())
                        && !month.isAfter(range.maxSelectableMonth()))
                .sorted()
                .toList();
    }

    @Transactional(readOnly = true)
    public YearMonth findNearestEarlierMonthWithData(YearMonth before) {
        return listMonthsWithData().stream()
                .filter(month -> month.isBefore(before))
                .max(Comparator.naturalOrder())
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public CompareDefaultsResponse getCompareDefaults() {
        List<YearMonth> monthsWithData = listMonthsWithData();
        FinancePeriodRange range = getFinancePeriodRange();

        YearMonth current;
        YearMonth comparison;
        if (!monthsWithData.isEmpty()) {
            current = monthsWithData.get(monthsWithData.size() - 1);
            comparison = monthsWithData.size() >= 2
                    ? monthsWithData.get(monthsWithData.size() - 2)
                    : current.minusMonths(1);
        } else {
            current = range.maxSelectableMonth();
            comparison = current.minusMonths(1);
        }

        if (comparison.isBefore(range.minSelectableMonth())) {
            comparison = range.minSelectableMonth();
        }
        if (comparison.equals(current) && current.isAfter(range.minSelectableMonth())) {
            comparison = current.minusMonths(1);
        }

        List<String> labels = new ArrayList<>();
        for (YearMonth month : monthsWithData) {
            labels.add(month.toString());
        }
        return new CompareDefaultsResponse(
                current.getYear(),
                current.getMonthValue(),
                comparison.getYear(),
                comparison.getMonthValue(),
                labels
        );
    }

    @Transactional(readOnly = true)
    public boolean monthHasTransactions(int year, int month) {
        LocalDate financeStart = getFinanceStartDate();
        LocalDate from = YearMonth.of(year, month).atDay(1);
        if (from.isBefore(financeStart)) {
            from = financeStart;
        }
        LocalDate to = YearMonth.of(year, month).atEndOfMonth();
        return cashTransactionRepository.countPostedInRange(from, to) > 0
                || paymentRepository.countValidInRange(from, to) > 0;
    }

    @Transactional(readOnly = true)
    public LocalDate resolvePeriodStart(int year, int month) {
        validateMonthSelectable(year, month);
        LocalDate monthStart = YearMonth.of(year, month).atDay(1);
        LocalDate financeStart = getFinanceStartDate();
        return monthStart.isBefore(financeStart) ? financeStart : monthStart;
    }

    /**
     * Period end for a selected month:
     * - business month → businessDate
     * - earlier months → month end
     * - later months that still contain existing data → month end (or latest data date in that month)
     */
    @Transactional(readOnly = true)
    public LocalDate resolvePeriodEnd(int year, int month) {
        validateMonthSelectable(year, month);
        FinancePeriodRange range = getFinancePeriodRange();
        YearMonth selected = YearMonth.of(year, month);
        YearMonth businessMonth = YearMonth.from(range.businessDate());

        if (selected.equals(businessMonth)) {
            return range.businessDate();
        }
        if (selected.isAfter(businessMonth)) {
            LocalDate latest = range.latestFinancialDataDate();
            if (latest != null && YearMonth.from(latest).equals(selected)) {
                return latest;
            }
            return selected.atEndOfMonth();
        }
        return selected.atEndOfMonth();
    }

    @Transactional(readOnly = true)
    public LocalDate resolveAllScopeEndDate() {
        FinancePeriodRange range = getFinancePeriodRange();
        LocalDate end = range.businessDate();
        if (range.latestFinancialDataDate() != null && range.latestFinancialDataDate().isAfter(end)) {
            end = range.latestFinancialDataDate();
        }
        return end;
    }

    private LocalDate parseDate(String value) {
        try {
            return LocalDate.parse(value.trim());
        } catch (DateTimeParseException ex) {
            throw new BusinessException("Invalid finance.start_date setting: " + value);
        }
    }
}
