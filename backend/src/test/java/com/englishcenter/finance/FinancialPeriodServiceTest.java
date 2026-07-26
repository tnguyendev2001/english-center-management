package com.englishcenter.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.ClosePeriodRequest;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.dto.ReopenPeriodRequest;
import com.englishcenter.finance.mapper.FinanceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class FinancialPeriodServiceTest {
    @Mock
    private FinancialPeriodRepository financialPeriodRepository;
    @Mock
    private FinanceCalculationService financeCalculationService;
    @Mock
    private FinanceConfigService financeConfigService;
    @Mock
    private FinancePeriodRangeService financePeriodRangeService;

    private FinancialPeriodService service;
    private int pastYear;
    private int pastMonth;

    @BeforeEach
    void setUp() {
        service = new FinancialPeriodService(
                financialPeriodRepository,
                financeCalculationService,
                financeConfigService,
                financePeriodRangeService,
                new FinanceMapper()
        );
        LocalDate businessDate = LocalDate.of(2026, 7, 27);
        when(financePeriodRangeService.getBusinessDate()).thenReturn(businessDate);
        YearMonth past = YearMonth.from(businessDate).minusMonths(1);
        pastYear = past.getYear();
        pastMonth = past.getMonthValue();
    }

    @Test
    void missingPeriodIsTreatedAsOpen() {
        when(financialPeriodRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.empty());
        service.validatePeriodOpen(LocalDate.of(2026, 7, 15));
    }

    @Test
    void closedPeriodRejectsPostingWithVietnameseMessage() {
        FinancialPeriod period = new FinancialPeriod();
        period.setYear(2026);
        period.setMonth(7);
        period.setStatus(FinancialPeriodStatus.CLOSED);
        when(financialPeriodRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.of(period));

        assertThatThrownBy(() -> service.validatePeriodOpen(LocalDate.of(2026, 7, 15)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Tháng 07/2026 đã khóa sổ. Vui lòng chọn ngày thuộc kỳ đang mở hoặc mở lại tháng.");
    }

    @Test
    void paymentValidationUsesPaymentDateOnly() {
        FinancialPeriod july = new FinancialPeriod();
        july.setYear(2026);
        july.setMonth(7);
        july.setStatus(FinancialPeriodStatus.CLOSED);
        when(financialPeriodRepository.findByYearAndMonth(2026, 7)).thenReturn(Optional.of(july));
        when(financialPeriodRepository.findByYearAndMonth(2026, 8)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validatePaymentPeriodOpen(LocalDate.of(2026, 7, 27)))
                .isInstanceOf(BusinessException.class)
                .hasMessage(
                        "Tháng 07/2026 đã khóa sổ. Vui lòng chọn ngày thanh toán thuộc kỳ đang mở hoặc mở lại tháng."
                );

        // Invoice may belong to July; payment dated in August is allowed.
        service.validatePaymentPeriodOpen(LocalDate.of(2026, 8, 5));
    }

    @Test
    void cannotCloseCurrentUnfinishedMonth() {
        assertThatThrownBy(() -> service.closePeriod(
                2026,
                7,
                new ClosePeriodRequest(null, false, null)
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Không thể khóa tháng hiện tại trước khi tháng kết thúc.");
    }

    @Test
    void closePeriodBlockedWhenReconciliationMismatched() {
        when(financialPeriodRepository.findByYearAndMonth(pastYear, pastMonth)).thenReturn(Optional.empty());
        when(financialPeriodRepository.save(any(FinancialPeriod.class))).thenAnswer(invocation -> {
            FinancialPeriod period = invocation.getArgument(0);
            period.setId(1L);
            return period;
        });
        when(financeCalculationService.reconcilePayments(isNull(), isNull())).thenReturn(
                new PaymentReconciliationResponse(
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ZERO,
                        BigDecimal.ONE,
                        0, 1, 0, 0, 0, 0, 0, 0,
                        ReconciliationStatus.MISMATCHED,
                        List.of()
                )
        );

        assertThatThrownBy(() -> service.closePeriod(pastYear, pastMonth, new ClosePeriodRequest(null, false, null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("reconciliation");
    }

    @Test
    void reopenRequiresReasonAndSetsOpen() {
        FinancialPeriod period = new FinancialPeriod();
        period.setId(1L);
        period.setYear(pastYear);
        period.setMonth(pastMonth);
        period.setStatus(FinancialPeriodStatus.CLOSED);
        period.setClosingBalanceSnapshot(new BigDecimal("1000"));
        period.setOutstandingDebtSnapshot(new BigDecimal("5000000"));
        when(financialPeriodRepository.findByYearAndMonth(pastYear, pastMonth)).thenReturn(Optional.of(period));
        when(financialPeriodRepository.save(period)).thenReturn(period);

        var response = service.reopenPeriod(pastYear, pastMonth, new ReopenPeriodRequest("Fix data", "admin"));

        assertThat(response.status()).isEqualTo(FinancialPeriodStatus.OPEN);
        assertThat(response.reopenReason()).isEqualTo("Fix data");
        assertThat(period.getClosingBalanceSnapshot()).isEqualByComparingTo("1000");
        // Historical debt snapshot is preserved on reopen.
        assertThat(period.getOutstandingDebtSnapshot()).isEqualByComparingTo("5000000");
    }

    @Test
    void closePeriodStoresSnapshotsWhenValid() {
        when(financialPeriodRepository.findByYearAndMonth(pastYear, pastMonth)).thenReturn(Optional.empty());
        when(financialPeriodRepository.save(any(FinancialPeriod.class))).thenAnswer(invocation -> {
            FinancialPeriod period = invocation.getArgument(0);
            period.setId(1L);
            return period;
        });
        when(financeCalculationService.reconcilePayments(isNull(), isNull())).thenReturn(
                new PaymentReconciliationResponse(
                        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                        0, 0, 0, 0, 0, 0, 0, 0,
                        ReconciliationStatus.MATCHED,
                        List.of()
                )
        );
        when(financeCalculationService.calculatePeriodSummary(anyInt(), anyInt(), anyBoolean())).thenReturn(
                new PeriodSummaryResponse(
                        pastYear, pastMonth,
                        LocalDate.of(pastYear, pastMonth, 1),
                        YearMonth.of(pastYear, pastMonth).atEndOfMonth(),
                        "OPEN",
                        new BigDecimal("10000000"),
                        new BigDecimal("8000000"),
                        new BigDecimal("5000000"),
                        new BigDecimal("3000000"),
                        new BigDecimal("3000000"),
                        new BigDecimal("5000000"),
                        new BigDecimal("15000000"),
                        true,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        new BigDecimal("5000000"),
                        false,
                        List.of()
                )
        );

        var response = service.closePeriod(pastYear, pastMonth, new ClosePeriodRequest("OK", false, "admin"));

        assertThat(response.status()).isEqualTo(FinancialPeriodStatus.CLOSED);
        assertThat(response.closingBalanceSnapshot()).isEqualByComparingTo("15000000");
        assertThat(response.outstandingDebtSnapshot()).isEqualByComparingTo("5000000");
        assertThat(response.closedBy()).isEqualTo("admin");
    }
}
