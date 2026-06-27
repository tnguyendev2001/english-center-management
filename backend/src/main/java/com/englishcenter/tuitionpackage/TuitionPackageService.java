package com.englishcenter.tuitionpackage;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.common.exception.NotFoundException;
import com.englishcenter.tuitionpackage.dto.TuitionPackageCreateRequest;
import com.englishcenter.tuitionpackage.dto.TuitionPackageResponse;
import com.englishcenter.tuitionpackage.dto.TuitionPackageUpdateRequest;
import com.englishcenter.tuitionpackage.mapper.TuitionPackageMapper;
import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TuitionPackageService {
    private static final int MAX_PAGE_SIZE = 100;

    private final TuitionPackageRepository tuitionPackageRepository;
    private final TuitionPackageMapper tuitionPackageMapper;

    public TuitionPackageService(
            TuitionPackageRepository tuitionPackageRepository,
            TuitionPackageMapper tuitionPackageMapper
    ) {
        this.tuitionPackageRepository = tuitionPackageRepository;
        this.tuitionPackageMapper = tuitionPackageMapper;
    }

    @Transactional(readOnly = true)
    public Page<TuitionPackageResponse> search(String keyword, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                normalizePageSize(size),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<TuitionPackage> tuitionPackages = isBlank(keyword)
                ? tuitionPackageRepository.findAll(pageable)
                : tuitionPackageRepository.search(keyword.trim(), pageable);

        return tuitionPackages.map(tuitionPackageMapper::toResponse);
    }

    @Transactional(readOnly = true)
    public TuitionPackageResponse getById(Long id) {
        return tuitionPackageMapper.toResponse(findTuitionPackage(id));
    }

    @Transactional
    public TuitionPackageResponse create(TuitionPackageCreateRequest request) {
        validate(request.name(), request.totalSessions(), request.price());

        TuitionPackage tuitionPackage = tuitionPackageMapper.toEntity(request);
        return tuitionPackageMapper.toResponse(tuitionPackageRepository.save(tuitionPackage));
    }

    @Transactional
    public TuitionPackageResponse update(Long id, TuitionPackageUpdateRequest request) {
        validate(request.name(), request.totalSessions(), request.price());

        TuitionPackage tuitionPackage = findTuitionPackage(id);
        tuitionPackageMapper.updateEntity(tuitionPackage, request);
        return tuitionPackageMapper.toResponse(tuitionPackageRepository.save(tuitionPackage));
    }

    @Transactional
    public TuitionPackageResponse deactivate(Long id) {
        TuitionPackage tuitionPackage = findTuitionPackage(id);
        tuitionPackage.setStatus(TuitionPackageStatus.INACTIVE);
        return tuitionPackageMapper.toResponse(tuitionPackageRepository.save(tuitionPackage));
    }

    private TuitionPackage findTuitionPackage(Long id) {
        return tuitionPackageRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Tuition package not found"));
    }

    private void validate(String name, Integer totalSessions, BigDecimal price) {
        if (isBlank(name)) {
            throw new BusinessException("Package name is required");
        }

        if (totalSessions == null || totalSessions <= 0) {
            throw new BusinessException("Total sessions must be greater than 0");
        }

        if (price == null || price.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException("Price must be greater than 0");
        }
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return 20;
        }

        return Math.min(size, MAX_PAGE_SIZE);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
