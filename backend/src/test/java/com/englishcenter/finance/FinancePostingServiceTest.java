package com.englishcenter.finance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.englishcenter.classroom.Classroom;
import com.englishcenter.finance.dto.CancelTransactionRequest;
import com.englishcenter.finance.dto.ManualExpenseRequest;
import com.englishcenter.finance.dto.ManualIncomeRequest;
import com.englishcenter.finance.dto.TransferRequest;
import com.englishcenter.finance.dto.TransferResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import com.englishcenter.invoice.Invoice;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import com.englishcenter.student.Student;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FinancePostingServiceTest {
    @Mock
    private CashTransactionRepository cashTransactionRepository;
    @Mock
    private FinancialAccountRepository financialAccountRepository;
    @Mock
    private TransactionCategoryRepository transactionCategoryRepository;
    @Mock
    private FinancialPeriodService financialPeriodService;
    @Mock
    private FinanceCalculationService financeCalculationService;
    @Mock
    private PaymentRepository paymentRepository;

    private FinancePostingService service;
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        service = new FinancePostingService(
                cashTransactionRepository,
                financialAccountRepository,
                transactionCategoryRepository,
                financialPeriodService,
                financeCalculationService,
                paymentRepository,
                new FinanceMapper()
        );
        lenient().when(financeCalculationService.calculateAccountBalance(any(), any()))
                .thenReturn(new BigDecimal("100000"));
    }

    @Test
    void postPaymentIncomeIsIdempotent() {
        Payment payment = payment();
        CashTransaction existing = new CashTransaction();
        existing.setId(99L);
        existing.setTransactionCode("CTX-PAY-EXIST");
        existing.setSourceType(TransactionSourceType.PAYMENT);
        existing.setSourceId(payment.getId());
        existing.setStatus(TransactionStatus.POSTED);

        when(cashTransactionRepository.findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                TransactionSourceType.PAYMENT, payment.getId()
        )).thenReturn(Optional.of(existing));

        CashTransaction result = service.postPaymentIncome(payment);

        assertThat(result.getId()).isEqualTo(99L);
        verify(cashTransactionRepository, never()).save(any());
        verify(cashTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelManualIncomeKeepsDirectionAndDoesNotCreateOpposite() {
        FinancialAccount account = cashAccount();
        TransactionCategory category = incomeCategory();
        CashTransaction original = postedIncome(account, category, "323246");

        when(cashTransactionRepository.findById(5L)).thenReturn(Optional.of(original));
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(financialPeriodService.isPeriodClosed(any())).thenReturn(false);

        var response = service.cancelTransaction(5L, new CancelTransactionRequest("Nhập sai", "admin"));

        assertThat(response.status()).isEqualTo(TransactionStatus.CANCELED);
        assertThat(response.direction()).isEqualTo(TransactionDirection.IN);
        assertThat(response.amount()).isEqualByComparingTo("323246");
        assertThat(response.cancelReason()).isEqualTo("Nhập sai");
        assertThat(original.getStatus()).isEqualTo(TransactionStatus.CANCELED);
        verify(cashTransactionRepository).save(original);
        verify(cashTransactionRepository, never()).saveAndFlush(any());
    }

    @Test
    void cancelPaymentIncomeIsIdempotentWhenAlreadyCanceled() {
        Payment payment = payment();
        CashTransaction original = new CashTransaction();
        original.setId(5L);
        original.setStatus(TransactionStatus.CANCELED);
        original.setSourceType(TransactionSourceType.PAYMENT);
        original.setDirection(TransactionDirection.IN);
        original.setAmount(new BigDecimal("1000000"));

        when(cashTransactionRepository.findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                TransactionSourceType.PAYMENT, payment.getId()
        )).thenReturn(Optional.of(original));

        CashTransaction result = service.cancelPaymentIncome(payment, "cancel again");

        assertThat(result.getStatus()).isEqualTo(TransactionStatus.CANCELED);
        verify(cashTransactionRepository, never()).save(any());
    }

    @Test
    void financeEndpointRejectsPaymentCancellation() {
        CashTransaction original = new CashTransaction();
        original.setId(5L);
        original.setStatus(TransactionStatus.POSTED);
        original.setSourceType(TransactionSourceType.PAYMENT);
        original.setTransactionDate(LocalDate.of(2026, 7, 1));
        when(cashTransactionRepository.findById(5L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.cancelTransaction(5L, new CancelTransactionRequest("x", null)))
                .hasMessageContaining("Thanh toán");
    }

    @Test
    void closedPeriodRejectsCancel() {
        FinancialAccount account = cashAccount();
        TransactionCategory category = incomeCategory();
        CashTransaction original = postedIncome(account, category, "100000");
        when(cashTransactionRepository.findById(5L)).thenReturn(Optional.of(original));
        when(financialPeriodService.isPeriodClosed(any())).thenReturn(true);

        assertThatThrownBy(() -> service.cancelTransaction(5L, new CancelTransactionRequest("x", null)))
                .hasMessageContaining("đã được chốt");
    }

    @Test
    void transferCreatesLinkedPair() {
        FinancialAccount cash = cashAccount();
        FinancialAccount bank = bankAccount();
        when(financialAccountRepository.findById(1L)).thenReturn(Optional.of(cash));
        when(financialAccountRepository.findById(2L)).thenReturn(Optional.of(bank));
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenAnswer(invocation -> {
            CashTransaction tx = invocation.getArgument(0);
            tx.setId(idSeq.getAndIncrement());
            return tx;
        });

        TransferResponse response = service.transfer(new TransferRequest(
                LocalDate.of(2026, 7, 1),
                1L,
                2L,
                new BigDecimal("3000000"),
                "Nạp ngân hàng",
                null
        ));

        assertThat(response.outflow().sourceType()).isEqualTo(TransactionSourceType.TRANSFER);
        assertThat(response.inflow().sourceType()).isEqualTo(TransactionSourceType.TRANSFER);
    }

    @Test
    void negativeBalanceStillSucceedsWithWarning() {
        FinancialAccount account = cashAccount();
        TransactionCategory category = expenseCategory();
        when(financialAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionCategoryRepository.findById(10L)).thenReturn(Optional.of(category));
        when(cashTransactionRepository.save(any(CashTransaction.class))).thenAnswer(invocation -> {
            CashTransaction tx = invocation.getArgument(0);
            tx.setId(idSeq.getAndIncrement());
            return tx;
        });
        when(financeCalculationService.calculateAccountBalance(any(), any()))
                .thenReturn(new BigDecimal("-100000"));

        var response = service.postManualExpense(new ManualExpenseRequest(
                LocalDate.of(2026, 7, 1),
                1L,
                10L,
                new BigDecimal("200000"),
                null,
                null,
                "Mua giấy",
                null
        ));

        assertThat(response.warnings()).isNotEmpty();
    }

    @Test
    void cannotCancelTwice() {
        CashTransaction original = new CashTransaction();
        original.setId(5L);
        original.setStatus(TransactionStatus.CANCELED);
        original.setSourceType(TransactionSourceType.MANUAL_INCOME);
        original.setTransactionDate(LocalDate.of(2026, 7, 1));
        when(cashTransactionRepository.findById(5L)).thenReturn(Optional.of(original));

        assertThatThrownBy(() -> service.cancelTransaction(5L, new CancelTransactionRequest("again", null)))
                .hasMessageContaining("already been canceled");
    }

    @Test
    void postManualIncomeRequiresIncomeCategory() {
        FinancialAccount account = cashAccount();
        TransactionCategory expense = expenseCategory();
        when(financialAccountRepository.findById(1L)).thenReturn(Optional.of(account));
        when(transactionCategoryRepository.findById(10L)).thenReturn(Optional.of(expense));

        assertThatThrownBy(() -> service.postManualIncome(new ManualIncomeRequest(
                LocalDate.of(2026, 7, 1),
                1L,
                10L,
                new BigDecimal("1000"),
                null,
                null,
                "Test",
                null
        ))).hasMessageContaining("direction must be INCOME");
    }

    private CashTransaction postedIncome(FinancialAccount account, TransactionCategory category, String amount) {
        CashTransaction original = new CashTransaction();
        original.setId(5L);
        original.setTransactionCode("CTX-IN-1");
        original.setTransactionDate(LocalDate.of(2026, 7, 1));
        original.setAccount(account);
        original.setDirection(TransactionDirection.IN);
        original.setCategory(category);
        original.setAmount(new BigDecimal(amount));
        original.setStatus(TransactionStatus.POSTED);
        original.setSourceType(TransactionSourceType.MANUAL_INCOME);
        return original;
    }

    private Payment payment() {
        Student student = new Student();
        student.setId(1L);
        student.setFullName("Nguyen Van A");
        Classroom classroom = new Classroom();
        classroom.setId(2L);
        classroom.setClassName("A1");
        Invoice invoice = new Invoice();
        invoice.setId(3L);
        invoice.setInvoiceCode("INV-1");

        Payment payment = new Payment();
        payment.setId(10L);
        payment.setPaymentCode("PAY-10");
        payment.setAmount(new BigDecimal("1000000"));
        payment.setPaymentDate(LocalDate.of(2026, 7, 1));
        payment.setMethod(PaymentMethod.CASH);
        payment.setStatus(PaymentStatus.VALID);
        payment.setStudent(student);
        payment.setClassroom(classroom);
        payment.setInvoice(invoice);
        return payment;
    }

    private FinancialAccount cashAccount() {
        FinancialAccount account = new FinancialAccount();
        account.setId(1L);
        account.setCode("CASH_CENTER");
        account.setName("Tiền mặt");
        account.setType(FinancialAccountType.CASH);
        account.setActive(true);
        account.setOpeningBalance(new BigDecimal("10000000"));
        account.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        return account;
    }

    private FinancialAccount bankAccount() {
        FinancialAccount account = new FinancialAccount();
        account.setId(2L);
        account.setCode("VCB");
        account.setName("Vietcombank");
        account.setType(FinancialAccountType.BANK);
        account.setActive(true);
        account.setOpeningBalance(BigDecimal.ZERO);
        account.setOpeningBalanceDate(LocalDate.of(2026, 1, 1));
        return account;
    }

    private TransactionCategory incomeCategory() {
        TransactionCategory category = new TransactionCategory();
        category.setId(2L);
        category.setCode("MATERIAL_INCOME");
        category.setName("Tiền tài liệu");
        category.setDirection(CategoryDirection.INCOME);
        category.setActive(true);
        return category;
    }

    private TransactionCategory expenseCategory() {
        TransactionCategory category = new TransactionCategory();
        category.setId(10L);
        category.setCode("PAPER");
        category.setName("Giấy");
        category.setDirection(CategoryDirection.EXPENSE);
        category.setActive(true);
        return category;
    }
}
