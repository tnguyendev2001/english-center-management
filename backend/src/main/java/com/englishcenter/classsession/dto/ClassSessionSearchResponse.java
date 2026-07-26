package com.englishcenter.classsession.dto;

import java.util.List;

public record ClassSessionSearchResponse(
        List<ClassSessionResponse> content,
        FocusSessionTargetResponse focusSession,
        FocusSessionTargetsResponse focusTargets
) {
}
