package com.englishcenter.center.dto;

public record CenterProfileResponse(
        Long id,
        String centerName,
        String centerSubtitle,
        String logoUrl,
        String address,
        String phone,
        String email,
        String website,
        String taxCode,
        String bankName,
        String bankAccountNumber,
        String bankAccountName,
        String paymentInstruction,
        String invoiceFooterNote,
        String receiptFooterNote
) {
}
