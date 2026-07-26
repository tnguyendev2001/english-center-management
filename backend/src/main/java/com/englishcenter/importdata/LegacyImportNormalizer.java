package com.englishcenter.importdata;

import com.englishcenter.classroom.ClassDayOfWeek;
import java.text.Normalizer;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

public final class LegacyImportNormalizer {
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final Pattern NON_CODE_CHARS = Pattern.compile("[^A-Za-z0-9\\-]");
    private static final Pattern PHONE_NOISE = Pattern.compile("[ .\\-]");

    private LegacyImportNormalizer() {
    }

    public static String normalizePersonName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return MULTI_SPACE.matcher(trimmed).replaceAll(" ");
    }

    public static String normalizePhone(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        // Keep as string; never parse as number. Preserve leading zeros.
        String normalized = PHONE_NOISE.matcher(trimmed).replaceAll("");
        return normalized.isEmpty() ? null : normalized;
    }

    public static String normalizeClassroomName(String value) {
        return normalizePersonName(value);
    }

    public static String normalizeClassroomNameKey(String value) {
        String name = normalizeClassroomName(value);
        if (name == null) {
            return null;
        }
        return name.replace(" ", "").toLowerCase(Locale.ROOT);
    }

    public static String toClassCode(String classroomName) {
        String name = normalizeClassroomName(classroomName);
        if (name == null) {
            return null;
        }
        String code = NON_CODE_CHARS.matcher(name.toUpperCase(Locale.ROOT)).replaceAll("");
        if (code.isBlank()) {
            code = "CLASS";
        }
        return code.length() <= 50 ? code : code.substring(0, 50);
    }

    public static Set<ClassDayOfWeek> parseDaysOfWeek(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }

        EnumSet<ClassDayOfWeek> days = EnumSet.noneOf(ClassDayOfWeek.class);
        String[] tokens = raw.split("[,;/|]+");
        for (String token : tokens) {
            ClassDayOfWeek day = parseDayToken(token.trim());
            if (day != null) {
                days.add(day);
            }
        }
        // Also allow space-separated compact tokens like "T2 T4"
        if (days.isEmpty()) {
            for (String token : raw.trim().split("\\s+")) {
                ClassDayOfWeek day = parseDayToken(token.trim());
                if (day != null) {
                    days.add(day);
                }
            }
        }
        return days;
    }

    private static ClassDayOfWeek parseDayToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }

        String ascii = Normalizer.normalize(token, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toUpperCase(Locale.ROOT)
                .replace('.', ' ')
                .trim();
        ascii = MULTI_SPACE.matcher(ascii).replaceAll(" ");

        return switch (ascii) {
            case "2", "T2", "MON", "MONDAY", "THU 2", "THU2" -> ClassDayOfWeek.MONDAY;
            case "3", "T3", "TUE", "TUESDAY", "THU 3", "THU3" -> ClassDayOfWeek.TUESDAY;
            case "4", "T4", "WED", "WEDNESDAY", "THU 4", "THU4" -> ClassDayOfWeek.WEDNESDAY;
            case "5", "T5", "THU", "THURSDAY", "THU 5", "THU5" -> ClassDayOfWeek.THURSDAY;
            case "6", "T6", "FRI", "FRIDAY", "THU 6", "THU6" -> ClassDayOfWeek.FRIDAY;
            case "7", "T7", "SAT", "SATURDAY", "THU 7", "THU7" -> ClassDayOfWeek.SATURDAY;
            case "CN", "8", "SUN", "SUNDAY", "CHU NHAT", "CHUNHAT" -> ClassDayOfWeek.SUNDAY;
            default -> {
                try {
                    yield ClassDayOfWeek.valueOf(ascii.replace(' ', '_'));
                } catch (IllegalArgumentException ex) {
                    yield null;
                }
            }
        };
    }
}
