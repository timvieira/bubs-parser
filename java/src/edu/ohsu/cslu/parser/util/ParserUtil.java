package edu.ohsu.cslu.parser.util;

import java.util.StringTokenizer;

public class ParserUtil {

    // parse 'line' into tokens and return them in an array
    public static String[] tokenize(String line) {
        StringTokenizer st = new StringTokenizer(line);
        int numTokens = st.countTokens();
        String[] tokens = new String[numTokens];

        int i = 0;
        while (st.hasMoreTokens()) {
            tokens[i++] = st.nextToken();
        }

        return tokens;
    }

    public static boolean isUpperCase(String s) {
        return s == s.toUpperCase();
    }

    public static boolean isLowerCase(String s) {
        return s == s.toLowerCase();
    }

    public static boolean containsDigit(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i)))
                return true;
        }
        return false;
    }

    public class Pair<Type1, Type2> {

        public final Type1 one;
        public final Type2 two;

        public Pair(Type1 a, Type2 b) {
            one = a;
            two = b;
        }
    }
}
