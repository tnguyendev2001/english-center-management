package com.englishcenter.center.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateCenterProfileRequest(
        @NotBlank @Size(max = 255) String centerName,
        @Size(max = 255) String centerSubtitle,
        @Size(max = 1000) String logoUrl,
        @Size(max = 500) String address,
        @Size(max = 50) String phone,
        @Size(max = 255) String email,
        @Size(max = 255) String website,
        @Size(max = 50) String taxCode,
        @Size(max = 255) String bankName,
        @Size(max = 100) String bankAccountNumber,
        @Size(max = 255) String bankAccountName,
        @Size(max = 1000) String paymentInstruction,
        @Size(max = 1000) String invoiceFooterNote,
        @Size(max = 1000) String receiptFooterNote
) {
}
