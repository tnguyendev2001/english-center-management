package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.finance.dto.TransactionCategoryRequest;
import com.englishcenter.finance.dto.TransactionCategoryResponse;
import com.englishcenter.finance.mapper.FinanceMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionCategoryService {
    private final TransactionCategoryRepository transactionCategoryRepository;
    private final FinanceMapper financeMapper;

    public TransactionCategoryService(
            TransactionCategoryRepository transactionCategoryRepository,
            FinanceMapper financeMapper
    ) {
        this.transactionCategoryRepository = transactionCategoryRepository;
        this.financeMapper = financeMapper;
    }

    @Transactional(readOnly = true)
    public List<TransactionCategoryResponse> listCategories() {
        return transactionCategoryRepository.findAllByOrderByDirectionAscDisplayOrderAscNameAsc().stream()
                .map(financeMapper::toCategoryResponse)
                .toList();
    }

    @Transactional
    public TransactionCategoryResponse create(TransactionCategoryRequest request) {
        String code = request.code().trim().toUpperCase();
        if (transactionCategoryRepository.existsByCodeIgnoreCase(code)) {
            throw new BusinessException("Category code already exists: " + code);
        }

        TransactionCategory category = new TransactionCategory();
        category.setCode(code);
        category.setName(request.name().trim());
        category.setDirection(request.direction());
        category.setSystemCategory(false);
        category.setActive(true);
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : 0);
        if (request.parentId() != null) {
            category.setParent(findCategory(request.parentId()));
        }
        return financeMapper.toCategoryResponse(transactionCategoryRepository.save(category));
    }

    @Transactional
    public TransactionCategoryResponse update(Long id, TransactionCategoryRequest request) {
        TransactionCategory category = findCategory(id);
        String code = request.code().trim().toUpperCase();
        if (transactionCategoryRepository.existsByCodeIgnoreCaseAndIdNot(code, id)) {
            throw new BusinessException("Category code already exists: " + code);
        }

        boolean hasTx = transactionCategoryRepository.hasTransactions(id);
        if (hasTx && category.getDirection() != request.direction()) {
            throw new BusinessException("Cannot change category direction after it has been used");
        }
        if (category.isSystemCategory() && category.getDirection() != request.direction()) {
            throw new BusinessException("Cannot change direction of system category");
        }
        if (category.isSystemCategory() && !category.getCode().equalsIgnoreCase(code)) {
            throw new BusinessException("Cannot change code of system category");
        }

        category.setCode(code);
        category.setName(request.name().trim());
        if (!hasTx && !category.isSystemCategory()) {
            category.setDirection(request.direction());
        }
        category.setDisplayOrder(request.displayOrder() != null ? request.displayOrder() : category.getDisplayOrder());
        if (request.parentId() != null) {
            category.setParent(findCategory(request.parentId()));
        } else {
            category.setParent(null);
        }
        return financeMapper.toCategoryResponse(transactionCategoryRepository.save(category));
    }

    @Transactional
    public TransactionCategoryResponse activate(Long id) {
        TransactionCategory category = findCategory(id);
        category.setActive(true);
        return financeMapper.toCategoryResponse(transactionCategoryRepository.save(category));
    }

    @Transactional
    public TransactionCategoryResponse deactivate(Long id) {
        TransactionCategory category = findCategory(id);
        if (category.isSystemCategory() && FinancePostingService.TUITION_CATEGORY_CODE.equalsIgnoreCase(category.getCode())) {
            throw new BusinessException("Cannot deactivate TUITION system category required for Payment integration");
        }
        category.setActive(false);
        return financeMapper.toCategoryResponse(transactionCategoryRepository.save(category));
    }

    private TransactionCategory findCategory(Long id) {
        return transactionCategoryRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction category not found"));
    }
}
