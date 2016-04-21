package com.radicalninja.logger;

public class StringUtil {

    public static String rlTrim(final String string, final Character ... chars) {
        int start = 0;
        int end = string.length();
        // Inspecting start of string
        for (int i = 0; i < string.length(); i++) {
            if (arrayContains(chars, string.charAt(i))) {
                start = i;
                break;
            }
        }
        for (int i = string.length() - 1; i > 0; i--) {
            if (arrayContains(chars, string.charAt(i))) {
                end = i;
                break;
            }
        }
        return string.substring(start, end);
    }

    public static <T extends Object> boolean arrayContains(final T[] array, final T object) {
        for (final T item : array) {
            if (item.equals(object)) {
                return true;
            }
        }
        return false;
    }

}
