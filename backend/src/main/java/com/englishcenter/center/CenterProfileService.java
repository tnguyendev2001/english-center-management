package com.englishcenter.center;

import com.englishcenter.center.dto.CenterProfileResponse;
import com.englishcenter.center.dto.UpdateCenterProfileRequest;
import com.englishcenter.common.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CenterProfileService {
    private final CenterProfileRepository centerProfileRepository;

    public CenterProfileService(CenterProfileRepository centerProfileRepository) {
        this.centerProfileRepository = centerProfileRepository;
    }

    @Transactional(readOnly = true)
    public CenterProfileResponse getProfile() {
        return toResponse(requireProfile());
    }

    @Transactional(readOnly = true)
    public CenterProfile requireEntity() {
        return requireProfile();
    }

    @Transactional
    public CenterProfileResponse updateProfile(UpdateCenterProfileRequest request) {
        CenterProfile profile = requireProfile();
        profile.setCenterName(request.centerName().trim());
        profile.setCenterSubtitle(trimToNull(request.centerSubtitle()));
        profile.setLogoUrl(trimToNull(request.logoUrl()));
        profile.setAddress(trimToNull(request.address()));
        profile.setPhone(trimToNull(request.phone()));
        profile.setEmail(trimToNull(request.email()));
        profile.setWebsite(trimToNull(request.website()));
        profile.setTaxCode(trimToNull(request.taxCode()));
        profile.setBankName(trimToNull(request.bankName()));
        profile.setBankAccountNumber(trimToNull(request.bankAccountNumber()));
        profile.setBankAccountName(trimToNull(request.bankAccountName()));
        profile.setPaymentInstruction(trimToNull(request.paymentInstruction()));
        profile.setInvoiceFooterNote(trimToNull(request.invoiceFooterNote()));
        profile.setReceiptFooterNote(trimToNull(request.receiptFooterNote()));
        return toResponse(centerProfileRepository.save(profile));
    }

    private CenterProfile requireProfile() {
        return centerProfileRepository.findAll().stream()
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Center profile not found"));
    }

    private CenterProfileResponse toResponse(CenterProfile profile) {
        return new CenterProfileResponse(
                profile.getId(),
                profile.getCenterName(),
                profile.getCenterSubtitle(),
                profile.getLogoUrl(),
                profile.getAddress(),
                profile.getPhone(),
                profile.getEmail(),
                profile.getWebsite(),
                profile.getTaxCode(),
                profile.getBankName(),
                profile.getBankAccountNumber(),
                profile.getBankAccountName(),
                profile.getPaymentInstruction(),
                profile.getInvoiceFooterNote(),
                profile.getReceiptFooterNote()
        );
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
