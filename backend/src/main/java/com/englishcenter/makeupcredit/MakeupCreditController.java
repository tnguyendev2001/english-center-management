package com.englishcenter.makeupcredit;

import com.englishcenter.common.api.ApiResponse;
import com.englishcenter.common.api.PageMeta;
import com.englishcenter.makeupcredit.dto.MakeupCreditResponse;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MakeupCreditController {
    private final MakeupCreditService makeupCreditService;

    public MakeupCreditController(MakeupCreditService makeupCreditService) {
        this.makeupCreditService = makeupCreditService;
    }

    @GetMapping("/api/makeup-credits")
    public ApiResponse<List<MakeupCreditResponse>> getMakeupCredits(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<MakeupCreditResponse> credits = makeupCreditService.getMakeupCredits(page, size);
        PageMeta meta = new PageMeta(
                credits.getNumber(),
                credits.getSize(),
                credits.getTotalElements(),
                credits.getTotalPages()
        );
        return ApiResponse.success(credits.getContent(), meta);
    }
}
