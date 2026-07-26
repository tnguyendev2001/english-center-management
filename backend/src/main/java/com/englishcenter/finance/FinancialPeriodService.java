package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.finance.dto.ClosePeriodRequest;
import com.englishcenter.finance.dto.FinancialPeriodResponse;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.dto.ReopenPeriodRequest;
import com.englishcenter.finance.mapper.FinanceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialPeriodService {
    private final FinancialPeriodRepository financialPeriodRepository;
    private final FinanceCalculationService financeCalculationService;
    private final FinanceConfigService financeConfigService;
    private final FinancePeriodRangeService financePeriodRangeService;
    private final FinanceMapper financeMapper;

    public FinancialPeriodService(
            FinancialPeriodRepository financialPeriodRepository,
            FinanceCalculationService financeCalculationService,
            FinanceConfigService financeConfigService,
            FinancePeriodRangeService financePeriodRangeService,
            FinanceMapper financeMapper
    ) {
        this.financialPeriodRepository = financialPeriodRepository;
        this.financeCalculationService = financeCalculationService;
        this.financeConfigService = financeConfigService;
        this.financePeriodRangeService = financePeriodRangeService;
        this.financeMapper = financeMapper;
    }

    /**
     * Default for a missing period row: OPEN.
     * Only an explicit CLOSED row blocks postings for that year/month.
     * Validation is based solely on the provided date's year/month
     * (e.g. paymentDate / transactionDate) — never invoice or enrollment dates.
     */
    @Transactional(readOnly = true)
    public void validatePeriodOpen(LocalDate transactionDate) {
        if (transactionDate == null) {
            throw new BusinessException("Transaction date is required");
        }
        FinancialPeriod closed = findClosedPeriod(transactionDate);
        if (closed != null) {
            throw new BusinessException(closedPeriodMessage(closed.getYear(), closed.getMonth()));
        }
    }

    /**
     * Payment posting must validate by paymentDate only.
     * An unpaid Invoice from a closed month remains payable in an open month.
     */
    @Transactional(readOnly = true)
    public void validatePaymentPeriodOpen(LocalDate paymentDate) {
        if (paymentDate == null) {
            throw new BusinessException("Payment date is required");
        }
        FinancialPeriod closed = findClosedPeriod(paymentDate);
        if (closed != null) {
            throw new BusinessException(
                    "Tháng " + String.format("%02d/%d", closed.getMonth(), closed.getYear())
                            + " đã khóa sổ. Vui lòng chọn ngày thanh toán thuộc kỳ đang mở hoặc mở lại tháng."
            );
        }
    }

    @Transactional(readOnly = true)
    public boolean isPeriodClosed(LocalDate date) {
        return findClosedPeriod(date) != null;
    }

    private FinancialPeriod findClosedPeriod(LocalDate date) {
        if (date == null) {
            return null;
        }
        return financialPeriodRepository.findByYearAndMonth(date.getYear(), date.getMonthValue())
                .filter(period -> period.getStatus() == FinancialPeriodStatus.CLOSED)
                .orElse(null);
    }

    private String closedPeriodMessage(int year, int month) {
        return "Tháng " + String.format("%02d/%d", month, year)
                + " đã khóa sổ. Vui lòng chọn ngày thuộc kỳ đang mở hoặc mở lại tháng.";
    }

    @Transactional(readOnly = true)
    public List<FinancialPeriodResponse> listPeriods() {
        return financialPeriodRepository.findAllByOrderByYearDescMonthDesc().stream()
                .map(financeMapper::toPeriodResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FinancialPeriodResponse getPeriod(int year, int month) {
        validateMonth(month);
        return financialPeriodRepository.findByYearAndMonth(year, month)
                .map(financeMapper::toPeriodResponse)
                .orElseGet(() -> new FinancialPeriodResponse(
                        null,
                        year,
                        month,
                        FinancialPeriodStatus.OPEN,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null,
                        null
                ));
    }

    @Transactional
    public FinancialPeriodResponse closePeriod(int year, int month, ClosePeriodRequest request) {
        validateMonth(month);
        financeConfigService.validateMonthInFinanceRange(year, month);
        YearMonth target = YearMonth.of(year, month);
        YearMonth businessMonth = YearMonth.from(financePeriodRangeService.getBusinessDate());
        if (!target.isBefore(businessMonth)) {
            throw new BusinessException("Không thể khóa tháng hiện tại trước khi tháng kết thúc.");
        }

        FinancialPeriod period = financialPeriodRepository.findByYearAndMonth(year, month)
                .orElseGet(() -> createOpenPeriod(year, month));

        if (period.getStatus() == FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Period is already closed");
        }

        PaymentReconciliationResponse reconciliation = financeCalculationService.reconcilePayments(null, null);
        if (reconciliation.status() == ReconciliationStatus.MISMATCHED
                && (request == null || !request.overrideReconciliationMismatch())) {
            throw new BusinessException(
                    "Cannot close period while payment reconciliation is mismatched. "
                            + "Provide overrideReconciliationMismatch=true to force close."
            );
        }

        PeriodSummaryResponse summary = financeCalculationService.calculatePeriodSummary(year, month, false);
        if (!summary.formulaReconciles()) {
            throw new BusinessException(
                    "Cannot close period: closing balance does not reconcile with opening + income - expense"
            );
        }

        period.setStatus(FinancialPeriodStatus.CLOSED);
        period.setClosedAt(LocalDateTime.now());
        period.setClosedBy(request != null && request.closedBy() != null ? request.closedBy().trim() : null);
        period.setNote(request != null ? trimToNull(request.note()) : null);
        period.setOpeningBalanceSnapshot(summary.openingBalance());
        period.setTotalIncomeSnapshot(summary.totalIncome());
        period.setTotalExpenseSnapshot(summary.totalExpense());
        period.setClosingBalanceSnapshot(summary.closingBalance());
        period.setOutstandingDebtSnapshot(summary.closingDebt());
        period.setReopenedAt(null);
        period.setReopenedBy(null);
        period.setReopenReason(null);

        return financeMapper.toPeriodResponse(financialPeriodRepository.save(period));
    }

    @Transactional
    public FinancialPeriodResponse reopenPeriod(int year, int month, ReopenPeriodRequest request) {
        validateMonth(month);
        FinancialPeriod period = financialPeriodRepository.findByYearAndMonth(year, month)
                .orElseThrow(() -> new NotFoundException("Financial period not found"));

        if (period.getStatus() != FinancialPeriodStatus.CLOSED) {
            throw new BusinessException("Only closed periods can be reopened");
        }

        period.setStatus(FinancialPeriodStatus.OPEN);
        period.setReopenedAt(LocalDateTime.now());
        period.setReopenedBy(trimToNull(request.reopenedBy()));
        period.setReopenReason(request.reason().trim());
        // Snapshots are preserved for audit; closed-period reports use them only while CLOSED.

        return financeMapper.toPeriodResponse(financialPeriodRepository.save(period));
    }

    private FinancialPeriod createOpenPeriod(int year, int month) {
        FinancialPeriod period = new FinancialPeriod();
        period.setYear(year);
        period.setMonth(month);
        period.setStatus(FinancialPeriodStatus.OPEN);
        return financialPeriodRepository.save(period);
    }

    private void validateMonth(int month) {
        if (month < 1 || month > 12) {
            throw new BusinessException("Month must be between 1 and 12");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
