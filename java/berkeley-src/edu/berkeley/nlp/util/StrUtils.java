package edu.berkeley.nlp.util;

import java.util.Arrays;
import java.util.List;

// TODO Merge with StringUtils or with Strings.java
public class StrUtils {

    public static <T> String join(final T[] objs) {
        if (objs == null) {
            return "";
        }
        final List<T> objs1 = Arrays.asList(objs);
        return join(objs1, " ", 0, objs1.size());
    }

    public static <T> String join(final List<T> objs, final String delim, final int start, final int end) {
        if (objs == null) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (int i = start; i < end; i++) {
            if (!first) {
                sb.append(delim);
            }
            sb.append(objs.get(i));
            first = false;
        }
        return sb.toString();
    }

    static boolean isEmpty(final String s) {
        return s == null || s.equals("");
    }
}
