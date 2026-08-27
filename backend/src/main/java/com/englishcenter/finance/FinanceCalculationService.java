package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.finance.dto.AccountBalanceResponse;
import com.englishcenter.finance.dto.CashTransactionResponse;
import com.englishcenter.finance.dto.CategoryBreakdownItemResponse;
import com.englishcenter.finance.dto.CompareDefaultsResponse;
import com.englishcenter.finance.dto.CompareMetricDifference;
import com.englishcenter.finance.dto.ComparePeriodInfo;
import com.englishcenter.finance.dto.ComparePeriodMetrics;
import com.englishcenter.finance.dto.ComparePeriodsResponse;
import com.englishcenter.finance.dto.DailyCashFlowItemResponse;
import com.englishcenter.finance.dto.FinanceOverviewResponse;
import com.englishcenter.finance.dto.FinancePeriodRange;
import com.englishcenter.finance.dto.MonthlyReportResponse;
import com.englishcenter.finance.dto.PaymentReconciliationMismatchItem;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.dto.StudentDebtItemResponse;
import com.englishcenter.finance.dto.YearlyMonthRowResponse;
import com.englishcenter.finance.dto.YearlyReportResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import com.englishcenter.financial.StudentFinancialSummaryAggregator;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.invoice.InvoiceStatus;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceCalculationService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final int MAX_PAGE_SIZE = 100;

    private final CashTransactionRepository cashTransactionRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final FinancialPeriodRepository financialPeriodRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final FinanceConfigService financeConfigService;
    private final FinancePeriodRangeService financePeriodRangeService;
    private final FinanceMapper financeMapper;

    public FinanceCalculationService(
            CashTransactionRepository cashTransactionRepository,
            FinancialAccountRepository financialAccountRepository,
            TransactionCategoryRepository transactionCategoryRepository,
            FinancialPeriodRepository financialPeriodRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            FinanceConfigService financeConfigService,
            FinancePeriodRangeService financePeriodRangeService,
            FinanceMapper financeMapper
    ) {
        this.cashTransactionRepository = cashTransactionRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.financialPeriodRepository = financialPeriodRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.financeConfigService = financeConfigService;
        this.financePeriodRangeService = financePeriodRangeService;
        this.financeMapper = financeMapper;
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateAccountBalance(Long accountId, LocalDate asOfDate) {
        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();

        BigDecimal opening = !account.getOpeningBalanceDate().isAfter(effectiveDate)
                ? account.getOpeningBalance()
                : ZERO;
        BigDecimal inflow = cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                accountId, TransactionDirection.IN, effectiveDate
        );
        BigDecimal outflow = cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                accountId, TransactionDirection.OUT, effectiveDate
        );

        return scale(opening.add(inflow).subtract(outflow));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateTotalBalance(LocalDate asOfDate) {
        return scale(financialAccountRepository.findAll().stream()
                .map(account -> calculateAccountBalance(account.getId(), asOfDate))
                .reduce(ZERO, BigDecimal::add));
    }

    @Transactional(readOnly = true)
    public AccountBalanceResponse getAccountBalance(Long accountId, LocalDate asOfDate) {
        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();
        BigDecimal opening = !account.getOpeningBalanceDate().isAfter(effectiveDate)
                ? account.getOpeningBalance()
                : ZERO;
        BigDecimal inflow = cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                accountId, TransactionDirection.IN, effectiveDate
        );
        BigDecimal outflow = cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                accountId, TransactionDirection.OUT, effectiveDate
        );
        BigDecimal balance = scale(opening.add(inflow).subtract(outflow));
        return new AccountBalanceResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType(),
                account.getOpeningBalance(),
                account.getOpeningBalanceDate(),
                scale(inflow),
                scale(outflow),
                balance,
                balance.compareTo(ZERO) < 0,
                account.isActive(),
                effectiveDate
        );
    }

    @Transactional(readOnly = true)
    public List<AccountBalanceResponse> calculateAccountBalances(LocalDate asOfDate) {
        LocalDate effectiveDate = asOfDate != null ? asOfDate : LocalDate.now();
        return financialAccountRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(account -> getAccountBalance(account.getId(), effectiveDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePeriodIncome(LocalDate fromDate, LocalDate toDate, Long accountId, Long categoryId) {
        return scale(cashTransactionRepository.sumPeriodAmountExcludingTransfers(
                TransactionDirection.IN, fromDate, toDate, accountId, categoryId
        ));
    }

    @Transactional(readOnly = true)
    public BigDecimal calculatePeriodExpense(LocalDate fromDate, LocalDate toDate, Long accountId, Long categoryId) {
        return scale(cashTransactionRepository.sumPeriodAmountExcludingTransfers(
                TransactionDirection.OUT, fromDate, toDate, accountId, categoryId
        ));
    }

    @Transactional(readOnly = true)
    public PeriodSummaryResponse calculatePeriodSummary(int year, int month, boolean preferSnapshotIfClosed) {
        validateMonth(month);
        financeConfigService.validateMonthInFinanceRange(year, month);
        LocalDate financeStart = financeConfigService.getFinanceStartDate();
        LocalDate fromDate = financeConfigService.resolvePeriodStart(year, month);
        LocalDate toDate = financeConfigService.resolvePeriodEnd(year, month);

        FinancialPeriod period = financialPeriodRepository.findByYearAndMonth(year, month).orElse(null);
        boolean closed = period != null && period.getStatus() == FinancialPeriodStatus.CLOSED;

        if (preferSnapshotIfClosed && closed
                && period.getOpeningBalanceSnapshot() != null
                && period.getClosingBalanceSnapshot() != null) {
            BigDecimal opening = scale(period.getOpeningBalanceSnapshot());
            BigDecimal income = scale(nullToZero(period.getTotalIncomeSnapshot()));
            BigDecimal expense = scale(nullToZero(period.getTotalExpenseSnapshot()));
            BigDecimal closing = scale(period.getClosingBalanceSnapshot());
            BigDecimal net = scale(income.subtract(expense));
            boolean reconciles = closing.compareTo(scale(opening.add(income).subtract(expense))) == 0;
            return new PeriodSummaryResponse(
                    year,
                    month,
                    fromDate,
                    toDate,
                    FinancialPeriodStatus.CLOSED.name(),
                    opening,
                    income,
                    ZERO,
                    ZERO,
                    expense,
                    net,
                    closing,
                    reconciles,
                    ZERO,
                    ZERO,
                    ZERO,
                    scale(nullToZero(period.getOutstandingDebtSnapshot())),
                    true,
                    List.of()
            );
        }

        LocalDate openingAsOf = fromDate.minusDays(1);
        if (openingAsOf.isBefore(financeStart.minusDays(1))) {
            openingAsOf = financeStart.minusDays(1);
        }
        BigDecimal openingBalance = calculateTotalBalance(openingAsOf);
        BigDecimal totalIncome = calculatePeriodIncome(fromDate, toDate, null, null);
        BigDecimal totalExpense = calculatePeriodExpense(fromDate, toDate, null, null);
        BigDecimal tuitionIncome = scale(cashTransactionRepository.sumTuitionLedgerAmount(fromDate, toDate));
        BigDecimal otherIncome = scale(totalIncome.subtract(tuitionIncome));
        BigDecimal netCashFlow = scale(totalIncome.subtract(totalExpense));
        BigDecimal closingBalance = scale(openingBalance.add(totalIncome).subtract(totalExpense));
        BigDecimal ledgerClosing = calculateTotalBalance(toDate);
        boolean reconciles = closingBalance.compareTo(ledgerClosing) == 0
                && closingBalance.compareTo(scale(openingBalance.add(totalIncome).subtract(totalExpense))) == 0;

        BigDecimal openingDebt = calculateStudentDebtAsOf(openingAsOf);
        BigDecimal closingDebt = calculateStudentDebtAsOf(toDate);
        BigDecimal paymentsCollected = scale(paymentRepository.sumValidAmountBetween(fromDate, toDate));
        BigDecimal newInvoicesAmount = ZERO;

        List<String> warnings = new ArrayList<>();
        if (!reconciles) {
            warnings.add("Closing balance formula does not match ledger balances at period end");
        }

        return new PeriodSummaryResponse(
                year,
                month,
                fromDate,
                toDate,
                closed ? FinancialPeriodStatus.CLOSED.name() : FinancialPeriodStatus.OPEN.name(),
                openingBalance,
                totalIncome,
                tuitionIncome,
                otherIncome,
                totalExpense,
                netCashFlow,
                closingBalance,
                reconciles,
                openingDebt,
                newInvoicesAmount,
                paymentsCollected,
                closingDebt,
                false,
                warnings
        );
    }

    @Transactional(readOnly = true)
    public MonthlyReportResponse calculateMonthlyReport(int year, int month) {
        financeConfigService.validateMonthInFinanceRange(year, month);
        PeriodSummaryResponse summary = calculatePeriodSummary(year, month, true);
        LocalDate fromDate = summary.fromDate();
        LocalDate toDate = summary.toDate();

        List<CategoryBreakdownItemResponse> expenseBreakdown = calculateCategoryBreakdown(
                fromDate, toDate, CategoryDirection.EXPENSE
        );
        List<CategoryBreakdownItemResponse> incomeBreakdown = calculateCategoryBreakdown(
                fromDate, toDate, CategoryDirection.INCOME
        );
        List<AccountBalanceResponse> accountBalances = calculateAccountBalances(toDate);
        List<DailyCashFlowItemResponse> daily = buildDailyCashFlow(fromDate, toDate);
        List<CashTransaction> txs = cashTransactionRepository.findFinalizedInRange(fromDate, toDate);
        List<CashTransactionResponse> txResponses = mapTransactionsWithRunningBalance(txs);

        Map<String, BigDecimal> expenseByCode = new LinkedHashMap<>();
        for (CategoryBreakdownItemResponse item : expenseBreakdown) {
            expenseByCode.put(item.categoryCode(), item.amount());
        }

        return new MonthlyReportResponse(
                summary,
                expenseBreakdown,
                incomeBreakdown,
                accountBalances,
                daily,
                txResponses,
                expenseByCode
        );
    }

    @Transactional(readOnly = true)
    public YearlyReportResponse calculateYearlyReport(int year) {
        FinancePeriodRange range = financePeriodRangeService.getFinancePeriodRange();
        LocalDate financeStart = range.financeStartDate();
        if (year < financeStart.getYear()) {
            throw new BusinessException(
                    "Dữ liệu tài chính chỉ được theo dõi từ tháng "
                            + String.format("%02d/%d", financeStart.getMonthValue(), financeStart.getYear()) + "."
            );
        }
        if (year > range.maxSelectableMonth().getYear()) {
            throw new BusinessException("Không thể xem kỳ tài chính trong tương lai.");
        }

        List<YearlyMonthRowResponse> months = new ArrayList<>();
        BigDecimal totalIncome = ZERO;
        BigDecimal totalExpense = ZERO;
        Integer highestIncomeMonth = null;
        Integer highestExpenseMonth = null;
        BigDecimal maxIncome = null;
        BigDecimal maxExpense = null;

        int startMonth = year == financeStart.getYear() ? financeStart.getMonthValue() : 1;
        int endMonth = year == range.maxSelectableMonth().getYear()
                ? range.maxSelectableMonth().getMonthValue()
                : 12;

        for (int month = startMonth; month <= endMonth; month++) {
            PeriodSummaryResponse summary = calculatePeriodSummary(year, month, true);
            YearlyMonthRowResponse row = new YearlyMonthRowResponse(
                    month,
                    summary.totalIncome(),
                    summary.totalExpense(),
                    summary.netCashFlow(),
                    summary.closingBalance(),
                    summary.closingDebt()
            );
            months.add(row);
            totalIncome = totalIncome.add(summary.totalIncome());
            totalExpense = totalExpense.add(summary.totalExpense());

            if (maxIncome == null || summary.totalIncome().compareTo(maxIncome) > 0) {
                maxIncome = summary.totalIncome();
                highestIncomeMonth = month;
            }
            if (maxExpense == null || summary.totalExpense().compareTo(maxExpense) > 0) {
                maxExpense = summary.totalExpense();
                highestExpenseMonth = month;
            }
        }

        LocalDate yearStart = year == financeStart.getYear() ? financeStart : LocalDate.of(year, 1, 1);
        LocalDate yearEnd = year == range.maxSelectableMonth().getYear()
                ? financePeriodRangeService.resolvePeriodEnd(year, endMonth)
                : LocalDate.of(year, 12, 31);
        List<CategoryBreakdownItemResponse> expenseBreakdown = calculateCategoryBreakdown(
                yearStart, yearEnd, CategoryDirection.EXPENSE
        );
        CategoryBreakdownItemResponse largest = expenseBreakdown.stream()
                .max(Comparator.comparing(CategoryBreakdownItemResponse::amount))
                .orElse(null);

        BigDecimal prevIncome = ZERO;
        BigDecimal prevExpense = ZERO;
        int prevYear = year - 1;
        if (prevYear >= financeStart.getYear()) {
            int prevStartMonth = prevYear == financeStart.getYear() ? financeStart.getMonthValue() : 1;
            for (int month = prevStartMonth; month <= 12; month++) {
                PeriodSummaryResponse prev = calculatePeriodSummary(prevYear, month, true);
                prevIncome = prevIncome.add(prev.totalIncome());
                prevExpense = prevExpense.add(prev.totalExpense());
            }
        }

        BigDecimal annualNet = scale(totalIncome.subtract(totalExpense));
        BigDecimal prevNet = scale(prevIncome.subtract(prevExpense));
        BigDecimal monthCount = BigDecimal.valueOf(Math.max(months.size(), 1));

        return new YearlyReportResponse(
                year,
                months,
                scale(totalIncome),
                scale(totalExpense),
                annualNet,
                highestIncomeMonth,
                highestExpenseMonth,
                largest != null ? largest.categoryCode() : null,
                largest != null ? largest.categoryName() : null,
                scale(totalIncome.divide(monthCount, 2, RoundingMode.HALF_UP)),
                scale(totalExpense.divide(monthCount, 2, RoundingMode.HALF_UP)),
                calculateTotalBalance(yearEnd),
                calculateStudentDebtAsOf(yearEnd),
                scale(totalIncome.subtract(prevIncome)),
                scale(totalExpense.subtract(prevExpense)),
                scale(annualNet.subtract(prevNet)),
                expenseBreakdown
        );
    }

    @Transactional(readOnly = true)
    public List<CategoryBreakdownItemResponse> calculateCategoryBreakdown(
            LocalDate fromDate,
            LocalDate toDate,
            CategoryDirection direction
    ) {
        TransactionDirection txDirection = direction == CategoryDirection.INCOME
                ? TransactionDirection.IN
                : TransactionDirection.OUT;

        return transactionCategoryRepository.findAllByOrderByDirectionAscDisplayOrderAscNameAsc().stream()
                .filter(category -> category.getDirection() == direction)
                .map(category -> {
                    BigDecimal amount = cashTransactionRepository.sumByCategoryAndDirectionInRange(
                            category.getId(), txDirection, fromDate, toDate
                    );
                    return new CategoryBreakdownItemResponse(
                            category.getId(),
                            category.getCode(),
                            category.getName(),
                            category.getDirection(),
                            scale(amount)
                    );
                })
                .filter(item -> item.amount().compareTo(ZERO) != 0)
                .toList();
    }

    @Transactional(readOnly = true)
    public CompareDefaultsResponse getCompareDefaults() {
        return financePeriodRangeService.getCompareDefaults();
    }

    @Transactional(readOnly = true)
    public ComparePeriodsResponse comparePeriods(
            int currentYear,
            int currentMonth,
            int comparisonYear,
            int comparisonMonth
    ) {
        validateMonth(currentMonth);
        validateMonth(comparisonMonth);
        financePeriodRangeService.validateMonthSelectable(currentYear, currentMonth);

        YearMonth currentYm = YearMonth.of(currentYear, currentMonth);
        YearMonth comparisonYm = YearMonth.of(comparisonYear, comparisonMonth);
        if (currentYm.equals(comparisonYm)) {
            throw new BusinessException("Kỳ hiện tại và kỳ so sánh phải khác nhau.");
        }

        FinancePeriodRange range = financePeriodRangeService.getFinancePeriodRange();
        YearMonth financeStartMonth = range.minSelectableMonth();
        if (comparisonYm.isBefore(financeStartMonth)) {
            throw new BusinessException("Kỳ so sánh nằm trước ngày bắt đầu theo dõi tài chính.");
        }
        financePeriodRangeService.validateMonthSelectable(comparisonYear, comparisonMonth);

        PeriodMetrics current = metricsForMonth(currentYear, currentMonth);
        PeriodMetrics comparison = metricsForMonth(comparisonYear, comparisonMonth);

        boolean currentHasTx = financePeriodRangeService.monthHasTransactions(currentYear, currentMonth);
        boolean comparisonHasTx = financePeriodRangeService.monthHasTransactions(comparisonYear, comparisonMonth);
        // Within selectable range a month always has a financial state (balances/debt as-of).
        boolean currentHasState = true;
        boolean comparisonHasState = true;

        ComparePeriodInfo currentInfo = new ComparePeriodInfo(
                currentYear,
                currentMonth,
                String.format("%02d/%d", currentMonth, currentYear),
                currentHasTx || currentHasState,
                currentHasTx,
                currentHasState
        );
        ComparePeriodInfo comparisonInfo = new ComparePeriodInfo(
                comparisonYear,
                comparisonMonth,
                String.format("%02d/%d", comparisonMonth, comparisonYear),
                comparisonHasTx || comparisonHasState,
                comparisonHasTx,
                comparisonHasState
        );

        String message = "Đang so sánh " + currentInfo.label() + " với " + comparisonInfo.label() + ".";
        String suggestion = null;
        Integer nearestYear = null;
        Integer nearestMonth = null;

        if (!comparisonHasTx) {
            message = message + " Tháng " + comparisonInfo.label() + " không có dữ liệu tài chính.";
            YearMonth nearest = financePeriodRangeService.findNearestEarlierMonthWithData(currentYm);
            if (nearest != null && !nearest.equals(comparisonYm)) {
                suggestion = "So sánh với kỳ có dữ liệu gần nhất";
                nearestYear = nearest.getYear();
                nearestMonth = nearest.getMonthValue();
            }
        }

        return new ComparePeriodsResponse(
                currentInfo,
                comparisonInfo,
                new ComparePeriodMetrics(
                        current.income(),
                        current.expense(),
                        current.net(),
                        current.closingBalance(),
                        current.debt()
                ),
                new ComparePeriodMetrics(
                        comparison.income(),
                        comparison.expense(),
                        comparison.net(),
                        comparison.closingBalance(),
                        comparison.debt()
                ),
                buildDifference(current.income(), comparison.income()),
                buildDifference(current.expense(), comparison.expense()),
                buildDifference(current.net(), comparison.net()),
                buildDifference(current.closingBalance(), comparison.closingBalance()),
                buildDifference(current.debt(), comparison.debt()),
                message,
                suggestion,
                nearestYear,
                nearestMonth
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal calculateStudentDebt() {
        return scale(nullToZero(invoiceRepository.sumDebtAmount()));
    }

    /**
     * Outstanding debt as of periodEnd:
     * invoices created by periodEnd, not canceled by periodEnd,
     * minus VALID payments with paymentDate &lt;= periodEnd.
     * For today/current, uses the live debt total.
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateStudentDebtAsOf(LocalDate asOfDate) {
        LocalDate today = LocalDate.now();
        if (asOfDate == null || !asOfDate.isBefore(today)) {
            return calculateStudentDebt();
        }

        BigDecimal total = ZERO;
        for (Invoice invoice : invoiceRepository.findInvoicesEffectiveAsOf(asOfDate)) {
            if (invoice.getStatus() == InvoiceStatus.REPLACED
                    && invoice.getUpdatedAt() != null
                    && !invoice.getUpdatedAt().toLocalDate().isAfter(asOfDate)) {
                continue;
            }
            BigDecimal paid = nullToZero(paymentRepository.sumValidAmountByInvoiceIdUpTo(invoice.getId(), asOfDate));
            BigDecimal remaining = scale(invoice.getFinalAmount().subtract(paid));
            if (remaining.compareTo(ZERO) > 0) {
                total = total.add(remaining);
            }
        }
        return scale(total);
    }

    @Transactional(readOnly = true)
    public List<StudentDebtItemResponse> calculateStudentDebtDetails() {
        return calculateStudentDebtDetailsAsOf(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<StudentDebtItemResponse> calculateStudentDebtDetailsAsOf(LocalDate asOfDate) {
        LocalDate today = LocalDate.now();
        if (asOfDate == null || !asOfDate.isBefore(today)) {
            return StudentFinancialSummaryAggregator.aggregateDebtSummaries(
                            invoiceRepository.findAllForDebtSummary(null)
                    ).stream()
                    .map(summary -> new StudentDebtItemResponse(
                            summary.studentId(),
                            summary.studentCode(),
                            summary.studentName(),
                            summary.currentClassroomId(),
                            summary.currentClassroomName(),
                            summary.totalRemainingDebt(),
                            (long) summary.debtInvoiceCount()
                    ))
                    .sorted(Comparator.comparing(StudentDebtItemResponse::remainingDebt).reversed())
                    .toList();
        }

        Map<String, StudentDebtItemResponse> grouped = new LinkedHashMap<>();
        for (Invoice invoice : invoiceRepository.findInvoicesEffectiveAsOf(asOfDate)) {
            if (invoice.getStatus() == InvoiceStatus.REPLACED
                    && invoice.getUpdatedAt() != null
                    && !invoice.getUpdatedAt().toLocalDate().isAfter(asOfDate)) {
                continue;
            }
            BigDecimal paid = nullToZero(paymentRepository.sumValidAmountByInvoiceIdUpTo(invoice.getId(), asOfDate));
            BigDecimal remaining = scale(invoice.getFinalAmount().subtract(paid));
            if (remaining.compareTo(ZERO) <= 0) {
                continue;
            }
            String key = invoice.getStudent().getId() + ":" + invoice.getClassroom().getId();
            StudentDebtItemResponse existing = grouped.get(key);
            if (existing == null) {
                grouped.put(key, new StudentDebtItemResponse(
                        invoice.getStudent().getId(),
                        invoice.getStudent().getStudentCode(),
                        invoice.getStudent().getFullName(),
                        invoice.getClassroom().getId(),
                        invoice.getClassroom().getClassName(),
                        remaining,
                        1L
                ));
            } else {
                grouped.put(key, new StudentDebtItemResponse(
                        existing.studentId(),
                        existing.studentCode(),
                        existing.studentName(),
                        existing.classroomId(),
                        existing.classroomName(),
                        scale(existing.remainingDebt().add(remaining)),
                        existing.debtInvoiceCount() + 1
                ));
            }
        }
        return grouped.values().stream()
                .sorted(Comparator.comparing(StudentDebtItemResponse::remainingDebt).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentReconciliationResponse reconcilePayments(LocalDate fromDate, LocalDate toDate) {
        List<Payment> payments = paymentRepository.findAll();
        List<CashTransaction> paymentLedger = cashTransactionRepository.findAllPaymentLedgerRows();

        Map<Long, List<CashTransaction>> ledgerByPaymentId = paymentLedger.stream()
                .filter(tx -> tx.getSourceId() != null)
                .collect(Collectors.groupingBy(CashTransaction::getSourceId));

        List<PaymentReconciliationMismatchItem> mismatches = new ArrayList<>();
        long matched = 0;
        long missingLedger = 0;
        long orphanLedger = 0;
        long duplicate = 0;
        long amountMismatch = 0;
        long cancellationMismatch = 0;
        long accountMismatch = 0;
        long dateMismatch = 0;

        BigDecimal totalValidCount = BigDecimal.ZERO;
        BigDecimal totalPaymentAmount = ZERO;
        BigDecimal totalTuitionLedger = ZERO;

        for (Payment payment : payments) {
            if (fromDate != null && payment.getPaymentDate().isBefore(fromDate)) {
                continue;
            }
            if (toDate != null && payment.getPaymentDate().isAfter(toDate)) {
                continue;
            }

            List<CashTransaction> txs = ledgerByPaymentId.getOrDefault(payment.getId(), List.of());
            if (payment.getStatus() == PaymentStatus.VALID) {
                totalValidCount = totalValidCount.add(BigDecimal.ONE);
                totalPaymentAmount = totalPaymentAmount.add(payment.getAmount());

                if (txs.isEmpty()) {
                    missingLedger++;
                    mismatches.add(new PaymentReconciliationMismatchItem(
                            "MISSING_LEDGER",
                            payment.getId(),
                            payment.getPaymentCode(),
                            null,
                            null,
                            "VALID payment has no PAYMENT cash transaction"
                    ));
                    continue;
                }
                if (txs.size() > 1) {
                    duplicate++;
                    mismatches.add(new PaymentReconciliationMismatchItem(
                            "DUPLICATE_LEDGER",
                            payment.getId(),
                            payment.getPaymentCode(),
                            txs.get(0).getId(),
                            txs.get(0).getTransactionCode(),
                            "Multiple PAYMENT cash transactions for one payment"
                    ));
                }

                CashTransaction primary = txs.get(0);
                boolean rowMatched = true;
                if (primary.getAmount().compareTo(payment.getAmount()) != 0) {
                    amountMismatch++;
                    rowMatched = false;
                    mismatches.add(new PaymentReconciliationMismatchItem(
                            "AMOUNT_MISMATCH",
                            payment.getId(),
                            payment.getPaymentCode(),
                            primary.getId(),
                            primary.getTransactionCode(),
                            "Payment amount " + payment.getAmount() + " != ledger " + primary.getAmount()
                    ));
                }
                if (!Objects.equals(primary.getTransactionDate(), payment.getPaymentDate())) {
                    dateMismatch++;
                    rowMatched = false;
                    mismatches.add(new PaymentReconciliationMismatchItem(
                            "DATE_MISMATCH",
                            payment.getId(),
                            payment.getPaymentCode(),
                            primary.getId(),
                            primary.getTransactionCode(),
                            "Payment date " + payment.getPaymentDate() + " != ledger " + primary.getTransactionDate()
                    ));
                }
                if (payment.getFinancialAccount() != null
                        && !Objects.equals(payment.getFinancialAccount().getId(), primary.getAccount().getId())) {
                    accountMismatch++;
                    rowMatched = false;
                    mismatches.add(new PaymentReconciliationMismatchItem(
                            "ACCOUNT_MISMATCH",
                            payment.getId(),
                            payment.getPaymentCode(),
                            primary.getId(),
                            primary.getTransactionCode(),
                            "Payment account differs from ledger account"
                    ));
                }
                if (rowMatched && txs.size() == 1) {
                    matched++;
                }
            } else if (payment.getStatus() == PaymentStatus.CANCELED) {
                if (!txs.isEmpty()) {
                    CashTransaction primary = txs.get(0);
                    if (primary.getStatus() != TransactionStatus.CANCELED) {
                        cancellationMismatch++;
                        mismatches.add(new PaymentReconciliationMismatchItem(
                                "CANCELLATION_MISMATCH",
                                payment.getId(),
                                payment.getPaymentCode(),
                                primary.getId(),
                                primary.getTransactionCode(),
                                "Canceled payment ledger row has not been canceled"
                        ));
                    }
                }
            }
        }

        for (CashTransaction tx : paymentLedger) {
            if (tx.getSourceId() == null) {
                orphanLedger++;
                mismatches.add(new PaymentReconciliationMismatchItem(
                        "ORPHAN_LEDGER",
                        null,
                        null,
                        tx.getId(),
                        tx.getTransactionCode(),
                        "PAYMENT cash transaction has no source payment id"
                ));
                continue;
            }
            boolean paymentExists = payments.stream().anyMatch(p -> Objects.equals(p.getId(), tx.getSourceId()));
            if (!paymentExists) {
                orphanLedger++;
                mismatches.add(new PaymentReconciliationMismatchItem(
                        "ORPHAN_LEDGER",
                        tx.getSourceId(),
                        null,
                        tx.getId(),
                        tx.getTransactionCode(),
                        "PAYMENT cash transaction references missing payment"
                ));
            }
        }

        // Recalculate tuition ledger using the same period formula for consistency
        LocalDate ledgerFrom = fromDate != null ? fromDate : LocalDate.of(1970, 1, 1);
        LocalDate ledgerTo = toDate != null ? toDate : LocalDate.of(9999, 12, 31);
        totalTuitionLedger = scale(cashTransactionRepository.sumTuitionLedgerAmount(ledgerFrom, ledgerTo));

        BigDecimal difference = scale(totalPaymentAmount.subtract(totalTuitionLedger));
        ReconciliationStatus status = mismatches.isEmpty() && difference.compareTo(ZERO) == 0
                ? ReconciliationStatus.MATCHED
                : ReconciliationStatus.MISMATCHED;

        return new PaymentReconciliationResponse(
                totalValidCount,
                scale(totalPaymentAmount),
                totalTuitionLedger,
                difference,
                matched,
                missingLedger,
                orphanLedger,
                duplicate,
                amountMismatch,
                cancellationMismatch,
                accountMismatch,
                dateMismatch,
                status,
                mismatches
        );
    }

    @Transactional(readOnly = true)
    public FinanceOverviewResponse getOverview(FinanceScope scope, Integer year, Integer month) {
        if (scope == null) {
            throw new BusinessException("scope is required (MONTH or ALL)");
        }

        FinancePeriodRange range = financePeriodRangeService.getFinancePeriodRange();
        LocalDate financeStart = range.financeStartDate();
        LocalDate periodStart;
        LocalDate periodEnd;
        Integer responseYear = null;
        Integer responseMonth = null;
        ChartGrouping chartGrouping;
        List<DailyCashFlowItemResponse> chart;
        BigDecimal totalIncome;
        BigDecimal totalExpense;
        List<AccountBalanceResponse> balances;
        BigDecimal outstandingDebt;
        List<StudentDebtItemResponse> topDebt;

        if (scope == FinanceScope.ALL) {
            periodStart = financeStart;
            periodEnd = financePeriodRangeService.resolveAllScopeEndDate();
            chartGrouping = ChartGrouping.MONTH;
            totalIncome = calculatePeriodIncome(periodStart, periodEnd, null, null);
            totalExpense = calculatePeriodExpense(periodStart, periodEnd, null, null);
            balances = calculateAccountBalances(periodEnd);
            outstandingDebt = calculateStudentDebt();
            topDebt = calculateStudentDebtDetails().stream().limit(10).toList();
            chart = buildMonthlyChart(periodStart, periodEnd);
        } else {
            if (year == null || month == null) {
                throw new BusinessException("year and month are required for MONTH scope");
            }
            financePeriodRangeService.validateMonthSelectable(year, month);
            responseYear = year;
            responseMonth = month;
            periodStart = financePeriodRangeService.resolvePeriodStart(year, month);
            periodEnd = financePeriodRangeService.resolvePeriodEnd(year, month);
            chartGrouping = ChartGrouping.DAY;

            FinancialPeriod period = financialPeriodRepository.findByYearAndMonth(year, month).orElse(null);
            boolean closedWithSnapshot = period != null
                    && period.getStatus() == FinancialPeriodStatus.CLOSED
                    && period.getTotalIncomeSnapshot() != null
                    && period.getTotalExpenseSnapshot() != null
                    && period.getClosingBalanceSnapshot() != null;

            if (closedWithSnapshot) {
                totalIncome = scale(period.getTotalIncomeSnapshot());
                totalExpense = scale(period.getTotalExpenseSnapshot());
                outstandingDebt = scale(nullToZero(period.getOutstandingDebtSnapshot()));
            } else {
                totalIncome = calculatePeriodIncome(periodStart, periodEnd, null, null);
                totalExpense = calculatePeriodExpense(periodStart, periodEnd, null, null);
                outstandingDebt = calculateStudentDebtAsOf(periodEnd);
            }
            balances = calculateAccountBalances(periodEnd);
            topDebt = calculateStudentDebtDetailsAsOf(periodEnd).stream().limit(10).toList();
            chart = buildDailyCashFlow(periodStart, periodEnd);

            BigDecimal total = closedWithSnapshot
                    ? scale(period.getClosingBalanceSnapshot())
                    : scale(balances.stream().map(AccountBalanceResponse::balance).reduce(ZERO, BigDecimal::add));
            return buildOverviewResponse(
                    scope,
                    periodStart,
                    periodEnd,
                    range,
                    responseYear,
                    responseMonth,
                    total,
                    totalIncome,
                    totalExpense,
                    outstandingDebt,
                    chartGrouping,
                    balances,
                    chart,
                    topDebt
            );
        }

        BigDecimal total = scale(balances.stream().map(AccountBalanceResponse::balance).reduce(ZERO, BigDecimal::add));
        return buildOverviewResponse(
                scope,
                periodStart,
                periodEnd,
                range,
                responseYear,
                responseMonth,
                total,
                totalIncome,
                totalExpense,
                outstandingDebt,
                chartGrouping,
                balances,
                chart,
                topDebt
        );
    }

    private FinanceOverviewResponse buildOverviewResponse(
            FinanceScope scope,
            LocalDate periodStart,
            LocalDate periodEnd,
            FinancePeriodRange range,
            Integer year,
            Integer month,
            BigDecimal totalBalance,
            BigDecimal totalIncome,
            BigDecimal totalExpense,
            BigDecimal outstandingDebt,
            ChartGrouping chartGrouping,
            List<AccountBalanceResponse> balances,
            List<DailyCashFlowItemResponse> chart,
            List<StudentDebtItemResponse> topDebt
    ) {
        BigDecimal cash = scale(balances.stream()
                .filter(b -> b.type() == FinancialAccountType.CASH)
                .map(AccountBalanceResponse::balance)
                .reduce(ZERO, BigDecimal::add));
        BigDecimal bank = scale(balances.stream()
                .filter(b -> b.type() == FinancialAccountType.BANK)
                .map(AccountBalanceResponse::balance)
                .reduce(ZERO, BigDecimal::add));

        PaymentReconciliationResponse reconciliation = reconcilePayments(periodStart, periodEnd);
        List<String> warnings = new ArrayList<>();
        boolean hasNegative = balances.stream().anyMatch(AccountBalanceResponse::negativeBalance);
        if (hasNegative) {
            warnings.add("One or more accounts have a negative balance");
        }
        if (reconciliation.status() == ReconciliationStatus.MISMATCHED) {
            warnings.add("Payment reconciliation is mismatched");
        }
        if (range.hasDataBeyondBusinessMonth()) {
            warnings.add("Có giao dịch có ngày lớn hơn ngày hiện tại. Vui lòng kiểm tra lại.");
        }

        List<CashTransactionResponse> recent = cashTransactionRepository.findAll(
                        CashTransactionSpecs.search(
                                periodStart, periodEnd, null, null, null, null, TransactionStatus.POSTED, null
                        ),
                        PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "transactionDate")
                                .and(Sort.by(Sort.Direction.DESC, "id")))
                ).getContent().stream()
                .map(tx -> financeMapper.toTransactionResponse(tx, null, List.of()))
                .toList();

        return new FinanceOverviewResponse(
                scope,
                periodStart,
                periodEnd,
                range.financeStartDate(),
                range.businessDate(),
                range.minSelectableMonthLabel(),
                range.maxSelectableMonthLabel(),
                range.latestTransactionDate(),
                range.latestPaymentDate(),
                range.hasDataBeyondBusinessMonth(),
                year,
                month,
                totalBalance,
                cash,
                bank,
                totalIncome,
                totalExpense,
                scale(totalIncome.subtract(totalExpense)),
                outstandingDebt,
                hasNegative,
                reconciliation.status() == ReconciliationStatus.MATCHED,
                chartGrouping,
                warnings,
                balances,
                calculateCategoryBreakdown(periodStart, periodEnd, CategoryDirection.EXPENSE),
                calculateCategoryBreakdown(periodStart, periodEnd, CategoryDirection.INCOME),
                chart,
                recent,
                topDebt
        );
    }

    private List<DailyCashFlowItemResponse> buildMonthlyChart(LocalDate fromDate, LocalDate toDate) {
        List<DailyCashFlowItemResponse> rows = new ArrayList<>();
        YearMonth cursor = YearMonth.from(fromDate);
        YearMonth end = YearMonth.from(toDate);
        while (!cursor.isAfter(end)) {
            LocalDate monthStart = cursor.atDay(1);
            if (monthStart.isBefore(fromDate)) {
                monthStart = fromDate;
            }
            LocalDate monthEnd = cursor.equals(YearMonth.from(toDate)) ? toDate : cursor.atEndOfMonth();
            BigDecimal income = calculatePeriodIncome(monthStart, monthEnd, null, null);
            BigDecimal expense = calculatePeriodExpense(monthStart, monthEnd, null, null);
            rows.add(new DailyCashFlowItemResponse(
                    cursor.atDay(1),
                    income,
                    expense,
                    scale(income.subtract(expense))
            ));
            cursor = cursor.plusMonths(1);
        }
        return rows;
    }

    @Transactional(readOnly = true)
    public Page<CashTransactionResponse> searchTransactions(
            LocalDate fromDate,
            LocalDate toDate,
            Long accountId,
            Long categoryId,
            TransactionDirection direction,
            TransactionSourceType sourceType,
            TransactionStatus status,
            String keyword,
            int page,
            int size
    ) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "transactionDate").and(Sort.by(Sort.Direction.DESC, "id"))
        );
        Page<CashTransaction> result = cashTransactionRepository.findAll(
                CashTransactionSpecs.search(
                        fromDate, toDate, accountId, categoryId, direction, sourceType, status, keyword
                ),
                pageable
        );

        return result.map(tx -> {
            BigDecimal balanceAfter = calculateAccountBalance(tx.getAccount().getId(), tx.getTransactionDate());
            return financeMapper.toTransactionResponse(tx, balanceAfter, List.of());
        });
    }

    @Transactional(readOnly = true)
    public CashTransactionResponse getTransaction(Long id) {
        CashTransaction tx = cashTransactionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));
        BigDecimal balanceAfter = calculateAccountBalance(tx.getAccount().getId(), tx.getTransactionDate());
        return financeMapper.toTransactionResponse(tx, balanceAfter, List.of());
    }

    private List<DailyCashFlowItemResponse> buildDailyCashFlow(LocalDate fromDate, LocalDate toDate) {
        Map<LocalDate, BigDecimal> income = new HashMap<>();
        Map<LocalDate, BigDecimal> expense = new HashMap<>();
        for (CashTransaction tx : cashTransactionRepository.findFinalizedNonTransferInRange(fromDate, toDate)) {
            if (tx.getDirection() == TransactionDirection.IN) {
                income.merge(tx.getTransactionDate(), tx.getAmount(), BigDecimal::add);
            } else {
                expense.merge(tx.getTransactionDate(), tx.getAmount(), BigDecimal::add);
            }
        }
        List<DailyCashFlowItemResponse> rows = new ArrayList<>();
        for (LocalDate d = fromDate; !d.isAfter(toDate); d = d.plusDays(1)) {
            BigDecimal in = scale(income.getOrDefault(d, ZERO));
            BigDecimal out = scale(expense.getOrDefault(d, ZERO));
            rows.add(new DailyCashFlowItemResponse(d, in, out, scale(in.subtract(out))));
        }
        return rows;
    }

    private List<CashTransactionResponse> mapTransactionsWithRunningBalance(List<CashTransaction> txs) {
        // Approximate per-account balance after each tx using as-of date (end of day aggregation).
        return txs.stream()
                .map(tx -> financeMapper.toTransactionResponse(
                        tx,
                        calculateAccountBalance(tx.getAccount().getId(), tx.getTransactionDate()),
                        List.of()
                ))
                .toList();
    }

    private PeriodMetrics metricsForMonth(int year, int month) {
        PeriodSummaryResponse summary = calculatePeriodSummary(year, month, true);
        return new PeriodMetrics(
                summary.totalIncome(),
                summary.totalExpense(),
                summary.netCashFlow(),
                summary.closingBalance(),
                summary.closingDebt()
        );
    }

    private CompareMetricDifference buildDifference(BigDecimal current, BigDecimal comparison) {
        BigDecimal amount = scale(current.subtract(comparison));
        ComparisonTrend trend = ComparisonTrend.UNCHANGED;
        if (amount.compareTo(ZERO) > 0) {
            trend = ComparisonTrend.INCREASE;
        } else if (amount.compareTo(ZERO) < 0) {
            trend = ComparisonTrend.DECREASE;
        }

        boolean percentageUnavailable = comparison == null || comparison.compareTo(ZERO) == 0;
        BigDecimal percentage = null;
        if (!percentageUnavailable) {
            percentage = amount
                    .multiply(BigDecimal.valueOf(100))
                    .divide(comparison.abs(), 2, RoundingMode.HALF_UP);
        }
        return new CompareMetricDifference(amount, percentage, trend, percentageUnavailable);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private BigDecimal scale(BigDecimal value) {
        return nullToZero(value).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal nullToZero(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private record PeriodMetrics(
            BigDecimal income,
            BigDecimal expense,
            BigDecimal net,
            BigDecimal closingBalance,
            BigDecimal debt
    ) {
    }
}
