package com.englishcenter.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.center")
public class CenterProperties {
    private String centerName = "Anh ngữ KHAI PHÓNG";
    private String centerAddress;
    private String centerPhone;
    private String tuitionNoticeText = "Học phí sẽ được thu trước khi học.";
    private String tuitionNoticeFooter = "Cảm ơn phụ huynh.";

    public String getCenterName() {
        return centerName;
    }

    public void setCenterName(String centerName) {
        this.centerName = centerName;
    }

    public String getCenterAddress() {
        return centerAddress;
    }

    public void setCenterAddress(String centerAddress) {
        this.centerAddress = centerAddress;
    }

    public String getCenterPhone() {
        return centerPhone;
    }

    public void setCenterPhone(String centerPhone) {
        this.centerPhone = centerPhone;
    }

    public String getTuitionNoticeText() {
        return tuitionNoticeText;
    }

    public void setTuitionNoticeText(String tuitionNoticeText) {
        this.tuitionNoticeText = tuitionNoticeText;
    }

    public String getTuitionNoticeFooter() {
        return tuitionNoticeFooter;
    }

    public void setTuitionNoticeFooter(String tuitionNoticeFooter) {
        this.tuitionNoticeFooter = tuitionNoticeFooter;
    }
}
