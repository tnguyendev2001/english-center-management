package com.englishcenter.common.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.time")
public class AppTimeProperties {
    /**
     * Authoritative business timezone for finance dates (Vietnam default).
     */
    private String zoneId = "Asia/Ho_Chi_Minh";

    public ZoneId zoneId() {
        return ZoneId.of(zoneId);
    }

    public String getZoneId() {
        return zoneId;
    }

    public void setZoneId(String zoneId) {
        this.zoneId = zoneId;
    }
}
