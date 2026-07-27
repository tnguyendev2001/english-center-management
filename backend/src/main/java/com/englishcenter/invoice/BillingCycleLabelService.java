package com.englishcenter.invoice;

import org.springframework.stereotype.Service;

@Service
public class BillingCycleLabelService {
    public String buildBillingLabel(String packageName, Integer cycleNo) {
        String packagePart = packageName == null || packageName.isBlank()
                ? "Gói học"
                : packageName.trim();
        if (cycleNo == null || cycleNo < 1) {
            return packagePart + " - Kỳ chưa xác định";
        }
        return packagePart + " - Kỳ " + cycleNo;
    }

    public String buildCycleOnlyLabel(Integer cycleNo) {
        if (cycleNo == null || cycleNo < 1) {
            return "Kỳ chưa xác định";
        }
        return "Kỳ " + cycleNo;
    }
}
