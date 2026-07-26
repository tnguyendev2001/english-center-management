package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.finance.dto.FinancialAccountRequest;
import com.englishcenter.finance.dto.FinancialAccountResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinancialAccountService {
    private final FinancialAccountRepository financialAccountRepository;
    private final CashTransactionRepository cashTransactionRepository;
    private final FinanceCalculationService financeCalculationService;
    private final FinanceMapper financeMapper;

    public FinancialAccountService(
            FinancialAccountRepository financialAccountRepository,
            CashTransactionRepository cashTransactionRepository,
            FinanceCalculationService financeCalculationService,
            FinanceMapper financeMapper
    ) {
        this.financialAccountRepository = financialAccountRepository;
        this.cashTransactionRepository = cashTransactionRepository;
        this.financeCalculationService = financeCalculationService;
        this.financeMapper = financeMapper;
    }

    @Transactional(readOnly = true)
    public List<FinancialAccountResponse> listAccounts() {
        return financialAccountRepository.findAllByOrderByDisplayOrderAscNameAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public FinancialAccountResponse create(FinancialAccountRequest request) {
        String code = request.code().trim().toUpperCase();
        if (financialAccountRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Account code already exists: " + code);
        }
        validateOpeningBalance(request.openingBalance());

        FinancialAccount account = new FinancialAccount();
        applyRequest(account, request, code, true);
        return toResponse(financialAccountRepository.save(account));
    }

    @Transactional
    public FinancialAccountResponse update(Long id, FinancialAccountRequest request) {
        FinancialAccount account = findAccount(id);
        String code = request.code().trim().toUpperCase();
        if (financialAccountRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("Account code already exists: " + code);
        }

        boolean hasTx = financialAccountRepository.hasTransactions(id);
        if (hasTx) {
            if (request.openingBalance().compareTo(account.getOpeningBalance()) != 0
                    || !request.openingBalanceDate().equals(account.getOpeningBalanceDate())) {
                throw new BusinessException(
                        "Opening balance cannot be changed after transactions exist; use an audited adjustment"
                );
            }
        } else {
            validateOpeningBalance(request.openingBalance());
        }

        applyRequest(account, request, code, !hasTx);
        return toResponse(financialAccountRepository.save(account));
    }

    @Transactional
    public FinancialAccountResponse activate(Long id) {
        FinancialAccount account = findAccount(id);
        account.setActive(true);
        return toResponse(financialAccountRepository.save(account));
    }

    @Transactional
    public FinancialAccountResponse deactivate(Long id) {
        FinancialAccount account = findAccount(id);
        account.setActive(false);
        return toResponse(financialAccountRepository.save(account));
    }

    private void applyRequest(FinancialAccount account, FinancialAccountRequest request, String code, boolean allowOpeningUpdate) {
        account.setCode(code);
        account.setName(request.name().trim());
        account.setType(request.type());
        if (allowOpeningUpdate) {
            account.setOpeningBalance(request.openingBalance());
            account.setOpeningBalanceDate(request.openingBalanceDate());
        }
        account.setNote(trimToNull(request.note()));
        account.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
    }

    private FinancialAccountResponse toResponse(FinancialAccount account) {
        BigDecimal balance = financeCalculationService.calculateAccountBalance(account.getId(), LocalDate.now());
        LocalDate lastTxDate = cashTransactionRepository
                .findTopByAccountIdAndStatusInOrderByTransactionDateDescIdDesc(
                        account.getId(),
                        EnumSet.of(TransactionStatus.POSTED)
                )
                .map(CashTransaction::getTransactionDate)
                .orElse(null);
        return financeMapper.toAccountResponse(account, balance, lastTxDate);
    }

    private FinancialAccount findAccount(Long id) {
        return financialAccountRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Financial account not found"));
    }

    private void validateOpeningBalance(BigDecimal openingBalance) {
        if (openingBalance == null) {
            throw new BusinessException("Opening balance is required");
        }
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
