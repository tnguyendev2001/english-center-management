package com.englishcenter.importdata;

import java.time.LocalDate;
import java.util.List;

public final class LegacyImportCycleCalculator {
    public static final int PACKAGE_SESSIONS = 8;

    private LegacyImportCycleCalculator() {
    }

    public static int packageCycles(int consumingSessionCount) {
        if (consumingSessionCount <= 0) {
            return 1;
        }
        return (int) Math.ceil(consumingSessionCount / (double) PACKAGE_SESSIONS);
    }

    public static int totalSessions(int packageCycles) {
        return packageCycles * PACKAGE_SESSIONS;
    }

    public static int remainingSessions(int packageCycles, int consumingSessionCount) {
        return totalSessions(packageCycles) - Math.max(consumingSessionCount, 0);
    }

    /**
     * Cycle N (1-based) starts at consuming attendance index (N-1)*8 + 1, or learningStartDate
     * for cycle 1 when no consuming attendance exists.
     */
    public static LocalDate cycleEffectiveDate(
            LocalDate learningStartDate,
            List<LocalDate> consumingSessionDates,
            int cycleNo
    ) {
        if (cycleNo < 1) {
            throw new IllegalArgumentException("cycleNo must be >= 1");
        }
        if (consumingSessionDates == null || consumingSessionDates.isEmpty()) {
            return learningStartDate;
        }
        int index = (cycleNo - 1) * PACKAGE_SESSIONS;
        if (index < consumingSessionDates.size()) {
            return consumingSessionDates.get(index);
        }
        return consumingSessionDates.getLast();
    }
}
