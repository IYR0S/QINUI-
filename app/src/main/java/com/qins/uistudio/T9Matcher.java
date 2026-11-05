package com.qins.uistudio;

import java.util.Locale;

/** T9 matcher for Latin and Hebrew app labels; no keyboard or runtime dependency required. */
public final class T9Matcher {
    private T9Matcher() { }

    public static boolean matches(CharSequence value, String query) {
        if (query == null || query.length() == 0) return true;
        if (value == null) return false;
        String digits = toDigits(value.toString());
        return digits.contains(query);
    }

    private static String toDigits(String value) {
        String text = value.toLowerCase(Locale.ROOT);
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= '0' && c <= '9') {
                result.append(c);
                continue;
            }
            if (c == ' ' || c == '-' || c == '_' || c == '.') {
                result.append('0');
                continue;
            }
            char digit = digitFor(c);
            if (digit != 0) result.append(digit);
        }
        return result.toString();
    }

    private static char digitFor(char c) {
        if ("abcאבג".indexOf(c) >= 0) return '2';
        if ("defדהו".indexOf(c) >= 0) return '3';
        if ("ghiזחט".indexOf(c) >= 0) return '4';
        if ("jklיכךל".indexOf(c) >= 0) return '5';
        if ("mnoמםנןס".indexOf(c) >= 0) return '6';
        if ("pqrsעפףצ".indexOf(c) >= 0) return '7';
        if ("tuvקררש".indexOf(c) >= 0) return '8';
        if ("wxyzת".indexOf(c) >= 0) return '9';
        return 0;
    }
}
