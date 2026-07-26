package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.finance.dto.CancelTransactionRequest;
import com.englishcenter.finance.dto.CashTransactionResponse;
import com.englishcenter.finance.dto.ManualExpenseRequest;
import com.englishcenter.finance.dto.ManualIncomeRequest;
import com.englishcenter.finance.dto.RepairPaymentLedgerRequest;
import com.englishcenter.finance.dto.RepairPaymentLedgerResponse;
import com.englishcenter.finance.dto.TransferRequest;
import com.englishcenter.finance.dto.TransferResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import com.englishcenter.payment.Payment;
import com.englishcenter.payment.PaymentMethod;
import com.englishcenter.payment.PaymentRepository;
import com.englishcenter.payment.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancePostingService {
    public static final String TUITION_CATEGORY_CODE = "TUITION";
    private static final String CLOSED_PERIOD_CANCEL_MESSAGE =
            "Kỳ tài chính đã được chốt. Vui lòng mở lại kỳ trước khi hủy giao dịch.";
    private static final String PAYMENT_CANCEL_VIA_PAYMENT_MODULE_MESSAGE =
            "Vui lòng hủy thanh toán tại chức năng Thanh toán.";

    private final CashTransactionRepository cashTransactionRepository;
    private final FinancialAccountRepository financialAccountRepository;
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final FinancialPeriodService financialPeriodService;
    private final FinanceCalculationService financeCalculationService;
    private final PaymentRepository paymentRepository;
    private final FinanceMapper financeMapper;

    public FinancePostingService(
            CashTransactionRepository cashTransactionRepository,
            FinancialAccountRepository financialAccountRepository,
            TransactionCategoryRepository transactionCategoryRepository,
            FinancialPeriodService financialPeriodService,
            FinanceCalculationService financeCalculationService,
            PaymentRepository paymentRepository,
            FinanceMapper financeMapper
    ) {
        this.cashTransactionRepository = cashTransactionRepository;
        this.financialAccountRepository = financialAccountRepository;
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.financialPeriodService = financialPeriodService;
        this.financeCalculationService = financeCalculationService;
        this.paymentRepository = paymentRepository;
        this.financeMapper = financeMapper;
    }

    @Transactional
    public CashTransactionResponse postManualIncome(ManualIncomeRequest request) {
        validateAmount(request.amount());
        financialPeriodService.validatePeriodOpen(request.transactionDate());

        FinancialAccount account = requireActiveAccount(request.accountId());
        TransactionCategory category = requireCategory(request.categoryId(), CategoryDirection.INCOME);

        CashTransaction tx = buildPostedTransaction(
                request.transactionDate(),
                account,
                TransactionDirection.IN,
                category,
                request.amount(),
                TransactionSourceType.MANUAL_INCOME,
                null,
                trimToNull(request.referenceNo()),
                trimToNull(request.payerOrPayee()),
                request.description().trim(),
                trimToNull(request.attachmentReference()),
                null
        );
        tx = cashTransactionRepository.save(tx);
        return toResponseWithWarnings(tx, account);
    }

    @Transactional
    public CashTransactionResponse postManualExpense(ManualExpenseRequest request) {
        validateAmount(request.amount());
        financialPeriodService.validatePeriodOpen(request.transactionDate());

        FinancialAccount account = requireActiveAccount(request.accountId());
        TransactionCategory category = requireCategory(request.categoryId(), CategoryDirection.EXPENSE);

        CashTransaction tx = buildPostedTransaction(
                request.transactionDate(),
                account,
                TransactionDirection.OUT,
                category,
                request.amount(),
                TransactionSourceType.MANUAL_EXPENSE,
                null,
                trimToNull(request.referenceNo()),
                trimToNull(request.payerOrPayee()),
                request.description().trim(),
                trimToNull(request.attachmentReference()),
                null
        );
        tx = cashTransactionRepository.save(tx);
        return toResponseWithWarnings(tx, account);
    }

    @Transactional
    public TransferResponse transfer(TransferRequest request) {
        validateAmount(request.amount());
        financialPeriodService.validatePeriodOpen(request.transactionDate());

        if (request.sourceAccountId().equals(request.destinationAccountId())) {
            throw new BusinessException("Source and destination accounts must be different");
        }

        FinancialAccount source = requireActiveAccount(request.sourceAccountId());
        FinancialAccount destination = requireActiveAccount(request.destinationAccountId());
        String groupId = "TRF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        CashTransaction outflow = buildPostedTransaction(
                request.transactionDate(),
                source,
                TransactionDirection.OUT,
                null,
                request.amount(),
                TransactionSourceType.TRANSFER,
                null,
                trimToNull(request.referenceNo()),
                null,
                request.description().trim(),
                null,
                groupId
        );
        CashTransaction inflow = buildPostedTransaction(
                request.transactionDate(),
                destination,
                TransactionDirection.IN,
                null,
                request.amount(),
                TransactionSourceType.TRANSFER,
                null,
                trimToNull(request.referenceNo()),
                null,
                request.description().trim(),
                null,
                groupId
        );

        outflow = cashTransactionRepository.save(outflow);
        inflow = cashTransactionRepository.save(inflow);

        List<String> warnings = new ArrayList<>();
        warnings.addAll(negativeBalanceWarnings(source));
        warnings.addAll(negativeBalanceWarnings(destination));

        return new TransferResponse(
                groupId,
                financeMapper.toTransactionResponse(outflow, null, warnings),
                financeMapper.toTransactionResponse(inflow, null, List.of()),
                warnings
        );
    }

    @Transactional
    public RepairPaymentLedgerResponse repairMissingPaymentLedger(RepairPaymentLedgerRequest request) {
        Payment payment = paymentRepository.findById(request.paymentId())
                .orElseThrow(() -> new NotFoundException("Payment not found"));
        if (payment.getStatus() != PaymentStatus.VALID) {
            throw new BusinessException("Only VALID payments can be repaired into the ledger");
        }

        boolean existed = cashTransactionRepository
                .findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                        TransactionSourceType.PAYMENT,
                        payment.getId()
                )
                .isPresent();

        CashTransaction tx = postPaymentIncome(payment);
        return new RepairPaymentLedgerResponse(
                payment.getId(),
                payment.getPaymentCode(),
                !existed,
                toResponseWithWarnings(tx, tx.getAccount()),
                existed
                        ? "Ledger already existed; no duplicate created"
                        : "Missing PAYMENT ledger row created"
        );
    }

    @Transactional
    public CashTransaction postPaymentIncome(Payment payment) {
        if (payment == null || payment.getId() == null) {
            throw new BusinessException("Payment is required for ledger posting");
        }

        return cashTransactionRepository
                .findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                        TransactionSourceType.PAYMENT,
                        payment.getId()
                )
                .orElseGet(() -> createPaymentIncome(payment));
    }

    /**
     * Called from Payment cancellation. Marks the PAYMENT ledger row CANCELED without creating an OUT row.
     * Idempotent when already CANCELED.
     */
    @Transactional
    public CashTransaction cancelPaymentIncome(Payment payment, String reason) {
        if (payment == null || payment.getId() == null) {
            throw new BusinessException("Payment is required for ledger cancellation");
        }

        CashTransaction original = cashTransactionRepository
                .findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                        TransactionSourceType.PAYMENT,
                        payment.getId()
                )
                .orElseThrow(() -> new BusinessException(
                        "No PAYMENT cash transaction found for payment " + payment.getPaymentCode()
                ));

        if (original.getStatus() == TransactionStatus.CANCELED) {
            return original;
        }

        markCanceled(original, reason, null, true);
        return cashTransactionRepository.save(original);
    }

    /**
     * @deprecated Use {@link #cancelTransaction(Long, CancelTransactionRequest)}.
     */
    @Deprecated
    @Transactional
    public CashTransactionResponse reverseTransaction(Long transactionId, CancelTransactionRequest request) {
        return cancelTransaction(transactionId, request);
    }

    @Transactional
    public CashTransactionResponse cancelTransaction(Long transactionId, CancelTransactionRequest request) {
        CashTransaction original = cashTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new NotFoundException("Transaction not found"));

        if (original.getSourceType() == TransactionSourceType.PAYMENT) {
            throw new BusinessException(PAYMENT_CANCEL_VIA_PAYMENT_MODULE_MESSAGE);
        }
        if (original.getSourceType() == TransactionSourceType.REVERSAL) {
            throw new BusinessException("Cannot cancel a legacy reversal row from this endpoint");
        }

        if (original.getSourceType() == TransactionSourceType.TRANSFER) {
            return cancelTransfer(original, request.reason().trim(), trimToNull(request.canceledBy()));
        }

        markCanceled(original, request.reason().trim(), trimToNull(request.canceledBy()), false);
        original = cashTransactionRepository.save(original);
        return toResponseWithWarnings(original, original.getAccount());
    }

    public FinancialAccount resolvePaymentAccount(PaymentMethod method, Long financialAccountId) {
        if (method == PaymentMethod.OTHER) {
            if (financialAccountId == null) {
                throw new BusinessException("financialAccountId is required for OTHER payment method");
            }
            return requireActiveAccount(financialAccountId);
        }

        if (financialAccountId != null) {
            FinancialAccount account = requireActiveAccount(financialAccountId);
            if (method == PaymentMethod.CASH && account.getType() != FinancialAccountType.CASH) {
                throw new BusinessException("CASH payment must map to a CASH account");
            }
            if (method == PaymentMethod.BANK_TRANSFER && account.getType() != FinancialAccountType.BANK) {
                throw new BusinessException("BANK_TRANSFER payment must map to a BANK account");
            }
            return account;
        }

        if (method == PaymentMethod.CASH) {
            return financialAccountRepository.findFirstByTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(FinancialAccountType.CASH)
                    .orElseThrow(() -> new BusinessException("No default CASH financial account configured"));
        }

        if (method == PaymentMethod.BANK_TRANSFER) {
            return financialAccountRepository.findFirstByTypeAndActiveTrueOrderByDisplayOrderAscIdAsc(FinancialAccountType.BANK)
                    .orElseThrow(() -> new BusinessException(
                            "No default BANK financial account configured; provide financialAccountId"
                    ));
        }

        throw new BusinessException("Unable to resolve financial account for payment method " + method);
    }

    private CashTransaction createPaymentIncome(Payment payment) {
        // Financial period is determined by paymentDate only — not invoice/enrollment dates.
        financialPeriodService.validatePaymentPeriodOpen(payment.getPaymentDate());

        FinancialAccount account = payment.getFinancialAccount();
        if (account == null) {
            account = resolvePaymentAccount(payment.getMethod(), null);
            payment.setFinancialAccount(account);
        }

        TransactionCategory tuition = transactionCategoryRepository.findByCodeIgnoreCase(TUITION_CATEGORY_CODE)
                .orElseThrow(() -> new BusinessException("System category TUITION is missing"));

        String studentName = payment.getStudent() != null ? payment.getStudent().getFullName() : null;
        String description = "Thu học phí"
                + (payment.getPaymentCode() != null ? " - " + payment.getPaymentCode() : "")
                + (payment.getInvoice() != null ? " - " + payment.getInvoice().getInvoiceCode() : "");

        CashTransaction tx = buildPostedTransaction(
                payment.getPaymentDate(),
                account,
                TransactionDirection.IN,
                tuition,
                payment.getAmount(),
                TransactionSourceType.PAYMENT,
                payment.getId(),
                payment.getPaymentCode(),
                studentName,
                description,
                null,
                null
        );

        try {
            return cashTransactionRepository.saveAndFlush(tx);
        } catch (DataIntegrityViolationException ex) {
            return cashTransactionRepository
                    .findFirstBySourceTypeAndSourceIdAndOriginalTransactionIsNull(
                            TransactionSourceType.PAYMENT,
                            payment.getId()
                    )
                    .orElseThrow(() -> ex);
        }
    }

    private CashTransactionResponse cancelTransfer(CashTransaction oneSide, String reason, String canceledBy) {
        if (oneSide.getTransferGroupId() == null) {
            throw new BusinessException("Transfer transaction is missing transferGroupId");
        }

        List<CashTransaction> sides = cashTransactionRepository.findByTransferGroupIdOrderByIdAsc(oneSide.getTransferGroupId());
        if (sides.size() != 2) {
            throw new BusinessException("Transfer group is incomplete and cannot be canceled safely");
        }

        for (CashTransaction side : sides) {
            if (side.getStatus() == TransactionStatus.CANCELED) {
                throw new BusinessException("Transaction has already been canceled");
            }
            if (side.getStatus() != TransactionStatus.POSTED) {
                throw new BusinessException("Only POSTED transactions can be canceled");
            }
            if (financialPeriodService.isPeriodClosed(side.getTransactionDate())) {
                throw new BusinessException(CLOSED_PERIOD_CANCEL_MESSAGE);
            }
        }

        CashTransaction first = null;
        for (CashTransaction side : sides) {
            markCanceled(side, reason, canceledBy, false);
            side = cashTransactionRepository.save(side);
            if (first == null) {
                first = side;
            }
        }

        return toResponseWithWarnings(first, first.getAccount());
    }

    private void markCanceled(CashTransaction tx, String reason, String canceledBy, boolean allowPaymentSource) {
        if (!allowPaymentSource && tx.getSourceType() == TransactionSourceType.PAYMENT) {
            throw new BusinessException(PAYMENT_CANCEL_VIA_PAYMENT_MODULE_MESSAGE);
        }
        if (tx.getStatus() == TransactionStatus.CANCELED) {
            throw new BusinessException("Transaction has already been canceled");
        }
        if (tx.getStatus() != TransactionStatus.POSTED) {
            throw new BusinessException("Only POSTED transactions can be canceled");
        }
        if (financialPeriodService.isPeriodClosed(tx.getTransactionDate())) {
            throw new BusinessException(CLOSED_PERIOD_CANCEL_MESSAGE);
        }

        LocalDateTime now = LocalDateTime.now();
        tx.setStatus(TransactionStatus.CANCELED);
        tx.setCancelReason(reason);
        tx.setCanceledAt(now);
        tx.setCanceledBy(canceledBy);
        // Keep legacy columns populated for audit continuity
        tx.setReversalReason(reason);
        tx.setReversedAt(now);
    }

    private CashTransaction buildPostedTransaction(
            LocalDate date,
            FinancialAccount account,
            TransactionDirection direction,
            TransactionCategory category,
            BigDecimal amount,
            TransactionSourceType sourceType,
            Long sourceId,
            String referenceNo,
            String payerOrPayee,
            String description,
            String attachmentReference,
            String transferGroupId
    ) {
        CashTransaction tx = new CashTransaction();
        tx.setTransactionCode(generateTransactionCode(sourceType));
        tx.setTransactionDate(date);
        tx.setAccount(account);
        tx.setDirection(direction);
        tx.setCategory(category);
        tx.setAmount(amount);
        tx.setStatus(TransactionStatus.POSTED);
        tx.setSourceType(sourceType);
        tx.setSourceId(sourceId);
        tx.setReferenceNo(referenceNo);
        tx.setPayerOrPayee(payerOrPayee);
        tx.setDescription(description);
        tx.setAttachmentReference(attachmentReference);
        tx.setTransferGroupId(transferGroupId);
        tx.setPostedAt(LocalDateTime.now());
        return tx;
    }

    private CashTransactionResponse toResponseWithWarnings(CashTransaction tx, FinancialAccount account) {
        List<String> warnings = negativeBalanceWarnings(account);
        BigDecimal balanceAfter = financeCalculationService.calculateAccountBalance(account.getId(), tx.getTransactionDate());
        return financeMapper.toTransactionResponse(tx, balanceAfter, warnings);
    }

    private List<String> negativeBalanceWarnings(FinancialAccount account) {
        BigDecimal balance = financeCalculationService.calculateAccountBalance(account.getId(), LocalDate.now());
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            return List.of("Account " + account.getCode() + " has negative balance: " + balance);
        }
        return List.of();
    }

    private FinancialAccount requireActiveAccount(Long accountId) {
        FinancialAccount account = financialAccountRepository.findById(accountId)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));
        if (!account.isActive()) {
            throw new BusinessException("Financial account is inactive: " + account.getCode());
        }
        return account;
    }

    private TransactionCategory requireCategory(Long categoryId, CategoryDirection expectedDirection) {
        TransactionCategory category = transactionCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Transaction category not found"));
        if (!category.isActive()) {
            throw new BusinessException("Transaction category is inactive: " + category.getCode());
        }
        if (category.getDirection() != expectedDirection) {
            throw new BusinessException(
                    "Category " + category.getCode() + " direction must be " + expectedDirection
            );
        }
        return category;
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Amount must be greater than 0");
        }
    }

    private String generateTransactionCode(TransactionSourceType sourceType) {
        String prefix = switch (sourceType) {
            case PAYMENT -> "CTX-PAY";
            case MANUAL_INCOME -> "CTX-IN";
            case MANUAL_EXPENSE -> "CTX-OUT";
            case TRANSFER -> "CTX-TRF";
            case REVERSAL -> "CTX-REV";
            case ADJUSTMENT -> "CTX-ADJ";
            case REFUND -> "CTX-RFD";
            case OPENING_BALANCE -> "CTX-OB";
        };
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
