package com.englishcenter.classsession.dto;

public record FocusSessionTargetsResponse(
        FocusSessionTargetResponse today,
        FocusSessionTargetResponse next,
        FocusSessionTargetResponse latest
) {
}
