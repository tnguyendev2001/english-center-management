package com.englishcenter.importdata;

import java.time.LocalDate;
import java.util.List;

public final class LegacyImportCycleCalculator {
    public static final int PACKAGE_SESSIONS = 8;

    private LegacyImportCycleCalculator() {
    }

    public static int packageCycles(int eligibleSessionCount) {
        if (eligibleSessionCount <= 0) {
            return 1;
        }
        return (int) Math.ceil(eligibleSessionCount / (double) PACKAGE_SESSIONS);
    }

    public static int totalSessions(int packageCycles) {
        return packageCycles * PACKAGE_SESSIONS;
    }

    public static int remainingSessions(int packageCycles, int eligibleSessionCount) {
        return totalSessions(packageCycles) - Math.max(eligibleSessionCount, 0);
    }

    /**
     * Cycle N (1-based) starts at eligible session index (N-1)*8 + 1, or learningStartDate for cycle 1
     * when no sessions exist.
     */
    public static LocalDate cycleEffectiveDate(
            LocalDate learningStartDate,
            List<LocalDate> eligibleSessionDates,
            int cycleNo
    ) {
        if (cycleNo < 1) {
            throw new IllegalArgumentException("cycleNo must be >= 1");
        }
        if (eligibleSessionDates == null || eligibleSessionDates.isEmpty()) {
            return learningStartDate;
        }
        int index = (cycleNo - 1) * PACKAGE_SESSIONS;
        if (index < eligibleSessionDates.size()) {
            return eligibleSessionDates.get(index);
        }
        return eligibleSessionDates.getLast();
    }
}
