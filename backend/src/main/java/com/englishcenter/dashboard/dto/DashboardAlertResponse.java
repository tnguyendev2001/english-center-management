package com.englishcenter.dashboard.dto;

import com.englishcenter.dashboard.DashboardAlertSeverity;
import com.englishcenter.dashboard.DashboardAlertType;

public record DashboardAlertResponse(
        DashboardAlertType type,
        DashboardAlertSeverity severity,
        String title,
        String description,
        long count,
        String actionLabel,
        String actionUrl,
        int priority
) {
}
