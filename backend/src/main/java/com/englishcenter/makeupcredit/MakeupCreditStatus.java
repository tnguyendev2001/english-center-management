package com.englishcenter.makeupcredit;

/**
 * Status of an approved-leave tracking record (MakeupCredit).
 *
 * <ul>
 *   <li>{@link #AVAILABLE} — leave recorded (UI: "Đã ghi nhận")</li>
 *   <li>{@link #CANCELED} — leave record canceled (e.g. attendance corrected)</li>
 *   <li>{@link #USED} — legacy / unused in V1 leave tracking; do not expose as "Đã dùng" workflow</li>
 * </ul>
 *
 * V1 does not consume these records for Enrollment progress or extra ClassSessions.
 */
public enum MakeupCreditStatus {
    AVAILABLE,
    USED,
    CANCELED
}
