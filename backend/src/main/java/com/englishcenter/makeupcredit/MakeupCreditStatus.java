package com.englishcenter.makeupcredit;

/**
 * Status of an approved-leave tracking record (internal MakeupCredit).
 *
 * <p>{@code AVAILABLE} is a recorded leave ("Đã ghi nhận"). {@code CANCELED} is a
 * canceled leave record. {@code USED} is legacy only and must not appear in V1 UI
 * or enrollment progress calculations.
 */
public enum MakeupCreditStatus {
    AVAILABLE,
    USED,
    CANCELED
}
