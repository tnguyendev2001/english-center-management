package com.englishcenter.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.englishcenter.common.config.AppTimeProperties;
import com.englishcenter.common.config.FinanceProperties;
import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.ComparePeriodsResponse;
import com.englishcenter.finance.dto.FinanceOverviewResponse;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import com.englishcenter.invoice.InvoiceRepository;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.settings.SystemSettingRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinanceCalculationServiceTest {
    private static final ZoneId HCM = ZoneId.of("Asia/Ho_Chi_Minh");

    @Mock
    private CashTransactionRepository cashTransactionRepository;
    @Mock
    private FinancialAccountRepository financialAccountRepository;
    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;
    @Mock
    private FinancialPeriodRepository financialPeriodRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private PaymentRepository paymentRepository;
    @Mock
    private SystemSettingRepository systemSettingRepository;

    private FinanceCalculationService service;

    @BeforeEach
    void setUp() {
        FinanceProperties properties = new FinanceProperties();
        properties.setFinanceStartDate(LocalDate.of(2026, 5, 1));
        AppTimeProperties timeProperties = new AppTimeProperties();
        timeProperties.setZoneId("Asia/Ho_Chi_Minh");
        Clock clock = Clock.fixed(LocalDate.of(2026, 7, 27).atStartOfDay(HCM).toInstant(), HCM);

        when(systemSettingRepository.findBySettingKey(any())).thenReturn(Optional.empty());
        when(cashTransactionRepository.findLatestPostedTransactionDate()).thenReturn(null);
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(null);
        when(financialPeriodRepository.findFirstByOrderByYearDescMonthDesc()).thenReturn(Optional.empty());

        FinancePeriodRangeService rangeService = new FinancePeriodRangeService(
                systemSettingRepository,
                properties,
                timeProperties,
                cashTransactionRepository,
                paymentRepository,
                financialPeriodRepository,
                clock
        );
        FinanceConfigService financeConfigService = new FinanceConfigService(
                systemSettingRepository,
                properties,
                rangeService
        );
        service = new FinanceCalculationService(
                cashTransactionRepository,
                financialAccountRepository,
                transactionCategoryRepository,
                financialPeriodRepository,
                invoiceRepository,
                paymentRepository,
                financeConfigService,
                rangeService,
                new FinanceMapper()
        );
    }

    @Test
    void calculateAccountBalanceUsesOpeningPlusInMinusOut() {
        FinancialAccount account = account(1L, "CASH", new BigDecimal("10000000"));
        when(financialAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                eq(1L), eq(TransactionDirection.IN), any()
        )).thenReturn(new BigDecimal("5000000"));
        when(cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                eq(1L), eq(TransactionDirection.OUT), any()
        )).thenReturn(new BigDecimal("2000000"));

        BigDecimal balance = service.calculateAccountBalance(1L, LocalDate.of(2026, 7, 26));

        assertThat(balance).isEqualByComparingTo("13000000");
    }

    @Test
    void periodIncomeAndExpenseExcludeTransfers() {
        when(cashTransactionRepository.sumPeriodAmountExcludingTransfers(
                eq(TransactionDirection.IN), any(), any(), isNull(), isNull()
        )).thenReturn(new BigDecimal("8000000"));
        when(cashTransactionRepository.sumPeriodAmountExcludingTransfers(
                eq(TransactionDirection.OUT), any(), any(), isNull(), isNull()
        )).thenReturn(new BigDecimal("3000000"));
        when(cashTransactionRepository.sumTuitionLedgerAmount(any(), any())).thenReturn(new BigDecimal("5000000"));
        when(financialAccountRepository.findAll()).thenReturn(List.of());
        when(financialPeriodRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.empty());
        when(invoiceRepository.sumDebtAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findInvoicesEffectiveAsOf(any())).thenReturn(List.of());
        when(paymentRepository.sumValidAmountBetween(any(), any())).thenReturn(BigDecimal.ZERO);

        PeriodSummaryResponse summary = service.calculatePeriodSummary(2026, 7, false);

        assertThat(summary.totalIncome()).isEqualByComparingTo("8000000");
        assertThat(summary.totalExpense()).isEqualByComparingTo("3000000");
        assertThat(summary.netCashFlow()).isEqualByComparingTo("5000000");
        assertThat(summary.closingBalance()).isEqualByComparingTo(
                summary.openingBalance().add(summary.totalIncome()).subtract(summary.totalExpense())
        );
    }

    @Test
    void studentDebtUsesInvoiceRemainingNotCashLedger() {
        when(invoiceRepository.sumDebtAmount()).thenReturn(new BigDecimal("2000000"));

        assertThat(service.calculateStudentDebt()).isEqualByComparingTo("2000000");
    }

    @Test
    void comparePeriodsReturnsNullPercentageWhenPreviousIsZeroButStillShowsValues() {
        when(cashTransactionRepository.sumPeriodAmountExcludingTransfers(any(), any(), any(), isNull(), isNull()))
                .thenReturn(BigDecimal.ZERO);
        when(cashTransactionRepository.sumTuitionLedgerAmount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(cashTransactionRepository.countPostedInRange(any(), any())).thenReturn(0L);
        when(cashTransactionRepository.findPostedTransactionYearMonths()).thenReturn(List.of("2026-07"));
        when(paymentRepository.findValidPaymentYearMonths()).thenReturn(List.of());
        when(paymentRepository.countValidInRange(any(), any())).thenReturn(0L);
        when(financialAccountRepository.findAll()).thenReturn(List.of());
        when(financialPeriodRepository.findByYearAndMonth(any(Integer.class), any(Integer.class)))
                .thenReturn(Optional.empty());
        when(financialPeriodRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.sumDebtAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findInvoicesEffectiveAsOf(any())).thenReturn(List.of());
        when(paymentRepository.sumValidAmountBetween(any(), any())).thenReturn(BigDecimal.ZERO);

        ComparePeriodsResponse compare = service.comparePeriods(2026, 7, 2026, 6);

        assertThat(compare.income().percentage()).isNull();
        assertThat(compare.income().percentageUnavailable()).isTrue();
        assertThat(compare.currentPeriod().label()).isEqualTo("07/2026");
        assertThat(compare.comparisonPeriod().label()).isEqualTo("06/2026");
        assertThat(compare.message()).contains("Đang so sánh 07/2026 với 06/2026");
        assertThat(compare.message()).contains("không có dữ liệu tài chính");
        assertThat(compare.current().income()).isEqualByComparingTo("0");
        assertThat(compare.comparison().income()).isEqualByComparingTo("0");
    }

    @Test
    void compareDefaultsPreferLatestMonthsWithData() {
        when(cashTransactionRepository.findPostedTransactionYearMonths())
                .thenReturn(List.of("2026-06", "2026-07", "2026-08"));
        when(paymentRepository.findValidPaymentYearMonths()).thenReturn(List.of("2026-08"));
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 10));
        when(financialPeriodRepository.findAll()).thenReturn(List.of());

        var defaults = service.getCompareDefaults();

        assertThat(defaults.currentYear()).isEqualTo(2026);
        assertThat(defaults.currentMonth()).isEqualTo(8);
        assertThat(defaults.comparisonYear()).isEqualTo(2026);
        assertThat(defaults.comparisonMonth()).isEqualTo(7);
    }

    @Test
    void overviewRejectsMonthBeforeFinanceStart() {
        assertThatThrownBy(() -> service.getOverview(FinanceScope.MONTH, 2020, 2))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Dữ liệu tài chính chỉ được theo dõi từ tháng 05/2026");
    }

    @Test
    void overviewAllowsAugustWhenExistingPaymentExists() {
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 10));
        stubOverviewQueries();

        FinanceOverviewResponse overview = service.getOverview(FinanceScope.MONTH, 2026, 8);

        assertThat(overview.year()).isEqualTo(2026);
        assertThat(overview.month()).isEqualTo(8);
        assertThat(overview.maxSelectableMonth()).isEqualTo("2026-08");
        assertThat(overview.periodEnd()).isEqualTo(LocalDate.of(2026, 8, 10));
    }

    @Test
    void overviewRejectsFutureMonth() {
        LocalDate future = LocalDate.of(2026, 9, 1);
        assertThatThrownBy(() -> service.getOverview(
                FinanceScope.MONTH, future.getYear(), future.getMonthValue()
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể xem kỳ tài chính trong tương lai.");
    }

    @Test
    void overviewAllUsesFinanceStartThroughLatestData() {
        when(paymentRepository.findLatestValidPaymentDate()).thenReturn(LocalDate.of(2026, 8, 10));
        stubOverviewQueries();

        FinanceOverviewResponse overview = service.getOverview(FinanceScope.ALL, null, null);

        assertThat(overview.scope()).isEqualTo(FinanceScope.ALL);
        assertThat(overview.periodStart()).isEqualTo(LocalDate.of(2026, 5, 1));
        assertThat(overview.periodEnd()).isEqualTo(LocalDate.of(2026, 8, 10));
        assertThat(overview.chartGrouping()).isEqualTo(ChartGrouping.MONTH);
        assertThat(overview.maxSelectableMonth()).isEqualTo("2026-08");
    }

    @Test
    void openingBalanceIgnoredWhenAsOfBeforeOpeningDate() {
        FinancialAccount account = account(1L, "CASH", new BigDecimal("10000000"));
        account.setOpeningBalanceDate(LocalDate.of(2026, 6, 1));
        when(financialAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                eq(1L), eq(TransactionDirection.IN), eq(LocalDate.of(2026, 5, 31))
        )).thenReturn(BigDecimal.ZERO);
        when(cashTransactionRepository.sumFinalizedAmountByAccountAndDirectionUpTo(
                eq(1L), eq(TransactionDirection.OUT), eq(LocalDate.of(2026, 5, 31))
        )).thenReturn(BigDecimal.ZERO);

        BigDecimal balance = service.calculateAccountBalance(1L, LocalDate.of(2026, 5, 31));

        assertThat(balance).isEqualByComparingTo("0");
    }

    @Test
    void reconcilePaymentsDetectsMissingLedger() {
        Payment payment = new Payment();
        payment.setId(10L);
        payment.setPaymentCode("PAY-1");
        payment.setAmount(new BigDecimal("1000000"));
        payment.setPaymentDate(LocalDate.of(2026, 7, 1));
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.VALID);

        when(paymentRepository.findAll()).thenReturn(List.of(payment));
        when(cashTransactionRepository.findAllPaymentLedgerRows()).thenReturn(List.of());
        when(cashTransactionRepository.sumTuitionLedgerAmount(any(), any())).thenReturn(BigDecimal.ZERO);

        PaymentReconciliationResponse result = service.reconcilePayments(null, null);

        assertThat(result.status()).isEqualTo(ReconciliationStatus.MISMATCHED);
        assertThat(result.missingLedgerCount()).isEqualTo(1);
        assertThat(result.mismatches()).isNotEmpty();
    }

    private void stubOverviewQueries() {
        when(cashTransactionRepository.sumPeriodAmountExcludingTransfers(
                any(), any(), any(), isNull(), isNull()
        )).thenAnswer(invocation -> {
            TransactionDirection direction = invocation.getArgument(0);
            return direction == TransactionDirection.IN
                    ? new BigDecimal("1000000")
                    : new BigDecimal("400000");
        });
        when(financialAccountRepository.findAllByOrderByDisplayOrderAscNameAsc()).thenReturn(List.of());
        when(financialAccountRepository.findAll()).thenReturn(List.of());
        when(invoiceRepository.sumDebtAmount()).thenReturn(BigDecimal.ZERO);
        when(invoiceRepository.findInvoicesEffectiveAsOf(any())).thenReturn(List.of());
        when(transactionCategoryRepository.findAllByOrderByDirectionAscDisplayOrderAscNameAsc())
                .thenReturn(List.of());
        when(cashTransactionRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(paymentRepository.findAll()).thenReturn(List.of());
        when(cashTransactionRepository.findAllPaymentLedgerRows()).thenReturn(List.of());
        when(cashTransactionRepository.sumTuitionLedgerAmount(any(), any())).thenReturn(BigDecimal.ZERO);
        when(financialPeriodRepository.findByYearAndMonth(any(Integer.class), any(Integer.class)))
                .thenReturn(Optional.empty());
    }

    private FinancialAccount account(Long id, String code, BigDecimal opening) {
        FinancialAccount account = new FinancialAccount();
        account.setId(id);
        account.setCode(code);
        account.setName(code);
        account.setType(FinancialAccountType.CASH);
        account.setOpeningBalance(opening);
        account.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        account.setActive(true);
        return account;
    }
}
