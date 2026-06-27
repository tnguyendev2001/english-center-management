package com.englishcenter.student.dto;

public record StudentSearchRequest(
        String keyword,
        int page,
        int size
) {
}
