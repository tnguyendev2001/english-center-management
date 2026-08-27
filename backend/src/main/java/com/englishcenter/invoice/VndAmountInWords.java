package com.englishcenter.invoice;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class VndAmountInWords {
    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] GROUP_UNITS = {
            "", "nghìn", "triệu", "tỷ", "nghìn tỷ", "triệu tỷ", "tỷ tỷ"
    };
    private static final BigInteger ONE_THOUSAND = BigInteger.valueOf(1_000);

    private VndAmountInWords() {
    }

    public static String convert(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Amount is required");
        }

        BigDecimal normalized = amount.stripTrailingZeros();
        if (normalized.scale() > 0) {
            throw new IllegalArgumentException("VND amount must be a whole number");
        }
        return convert(normalized.toBigIntegerExact());
    }

    static String convert(BigInteger amount) {
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("VND amount must not be negative");
        }
        if (amount.signum() == 0) {
            return "Không đồng";
        }

        List<Integer> groups = splitIntoGroups(amount);
        if (groups.size() > GROUP_UNITS.length) {
            throw new IllegalArgumentException("VND amount is too large");
        }

        List<String> words = new ArrayList<>();
        int highestGroupIndex = groups.size() - 1;
        for (int index = highestGroupIndex; index >= 0; index--) {
            int group = groups.get(index);
            if (group == 0) {
                continue;
            }

            boolean forceHundreds = index < highestGroupIndex && group < 100;
            words.add(readThreeDigits(group, forceHundreds));
            if (!GROUP_UNITS[index].isEmpty()) {
                words.add(GROUP_UNITS[index]);
            }
        }

        String result = String.join(" ", words) + " đồng";
        return result.substring(0, 1).toUpperCase(Locale.forLanguageTag("vi-VN")) + result.substring(1);
    }

    private static List<Integer> splitIntoGroups(BigInteger amount) {
        List<Integer> groups = new ArrayList<>();
        BigInteger remaining = amount;
        while (remaining.signum() > 0) {
            BigInteger[] division = remaining.divideAndRemainder(ONE_THOUSAND);
            groups.add(division[1].intValueExact());
            remaining = division[0];
        }
        return groups;
    }

    private static String readThreeDigits(int value, boolean forceHundreds) {
        int hundreds = value / 100;
        int tens = (value % 100) / 10;
        int units = value % 10;
        List<String> words = new ArrayList<>();

        if (hundreds > 0 || forceHundreds) {
            words.add(DIGITS[hundreds]);
            words.add("trăm");
        }

        if (tens > 1) {
            words.add(DIGITS[tens]);
            words.add("mươi");
        } else if (tens == 1) {
            words.add("mười");
        } else if (units > 0 && (hundreds > 0 || forceHundreds)) {
            words.add("lẻ");
        }

        if (units > 0) {
            words.add(readUnit(units, tens));
        }

        return String.join(" ", words);
    }

    private static String readUnit(int units, int tens) {
        if (units == 1 && tens > 1) {
            return "mốt";
        }
        if (units == 4 && tens > 1) {
            return "tư";
        }
        if (units == 5 && tens > 0) {
            return "lăm";
        }
        return DIGITS[units];
    }
}
