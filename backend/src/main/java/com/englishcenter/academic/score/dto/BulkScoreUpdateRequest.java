package com.englishcenter.academic.score.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record BulkScoreUpdateRequest(
        @NotEmpty @Valid List<ScoreRowRequest> rows
) {
}
