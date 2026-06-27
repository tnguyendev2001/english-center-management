package com.englishcenter.revenue;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.revenue.dto.RevenueSummaryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/revenue")
public class RevenueController {
    private final RevenueService revenueService;

    public RevenueController(RevenueService revenueService) {
        this.revenueService = revenueService;
    }

    @GetMapping("/summary")
    public ApiResponse<RevenueSummaryResponse> getSummary() {
        return ApiResponse.success(revenueService.getSummary());
    }
}
