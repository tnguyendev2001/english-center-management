package com.englishcenter.makeupcredit;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.makeupcredit.dto.MakeupCreditResponse;
import com.englishcenter.makeupcredit.mapper.MakeupCreditMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Leave-history operations on MakeupCredit. Does not create ClassSession or change enrollment progress. */
@Service
public class MakeupCreditService {
    private static final int MAX_PAGE_SIZE = 100;

    private final MakeupCreditRepository makeupCreditRepository;
    private final MakeupCreditMapper makeupCreditMapper;

    public MakeupCreditService(
            MakeupCreditRepository makeupCreditRepository,
            MakeupCreditMapper makeupCreditMapper
    ) {
        this.makeupCreditRepository = makeupCreditRepository;
        this.makeupCreditMapper = makeupCreditMapper;
    }

    @Transactional(readOnly = true)
    public Page<MakeupCreditResponse> getMakeupCredits(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), normalizePageSize(size));
        return makeupCreditRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(makeupCreditMapper::toResponse);
    }

    @Transactional
    public MakeupCreditResponse cancel(Long id) {
        MakeupCredit credit = makeupCreditRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Leave record not found"));
        if (credit.getStatus() == MakeupCreditStatus.CANCELED) {
            throw new BusinessException("Leave record is already canceled");
        }

        credit.setStatus(MakeupCreditStatus.CANCELED);
        credit.setNote("Canceled from leave history");
        return makeupCreditMapper.toResponse(makeupCreditRepository.save(credit));
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }
}
