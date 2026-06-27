package com.englishcenter.common.api;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(
        boolean success,
        String message,
        List<ErrorItem> errors
) {
    public static ErrorResponse of(String message) {
        return new ErrorResponse(false, message, null);
    }

    public static ErrorResponse validation(List<ErrorItem> errors) {
        return new ErrorResponse(false, "Validation failed", errors);
    }
}
