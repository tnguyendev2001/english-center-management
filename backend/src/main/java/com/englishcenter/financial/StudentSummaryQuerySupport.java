package com.englishcenter.financial;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

public final class StudentSummaryQuerySupport {
    private StudentSummaryQuerySupport() {
    }

    public static boolean matchesKeyword(String keyword, String... fields) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }

        String normalized = keyword.trim().toLowerCase();
        for (String field : fields) {
            if (field != null && field.toLowerCase().contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    public static <T> Page<T> paginate(List<T> items, int page, int size, int maxPageSize) {
        int pageSize = size <= 0 ? 20 : Math.min(size, maxPageSize);
        int pageNumber = Math.max(page, 0);
        PageRequest pageable = PageRequest.of(pageNumber, pageSize);
        int from = pageNumber * pageSize;
        if (from >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int to = Math.min(from + pageSize, items.size());
        return new PageImpl<>(items.subList(from, to), pageable, items.size());
    }
}
