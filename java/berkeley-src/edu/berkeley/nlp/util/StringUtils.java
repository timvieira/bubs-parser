package edu.berkeley.nlp.util;

/**
 * StringUtils is a class for random String things.
 * 
 * TODO Most of the remaining functionality could be replaced with String.format()
 * 
 * @author Dan Klein
 * @author Christopher Manning
 * @author Tim Grow (grow@stanford.edu)
 * @author Chris Cox
 * @version 2003/02/03
 */
public class StringUtils {

    /**
     * Return a String of length a minimum of totalChars characters by padding the input String str with spaces. If str
     * is already longer than totalChars, it is returned unchanged.
     */
    public static String pad(String str, final int totalChars) {
        if (str == null)
            str = "null";
        final int slen = str.length();
        final StringBuffer sb = new StringBuffer(str);
        for (int i = 0; i < totalChars - slen; i++) {
            sb.append(" ");
        }
        return sb.toString();
    }

    /**
     * Pad or trim so as to produce a string of exactly a certain length.
     * 
     * @param str The String to be padded or truncated
     * @param num The desired length
     */
    public static String padOrTrim(String str, final int num) {
        if (str == null)
            str = "null";
        final int leng = str.length();
        if (leng < num) {
            final StringBuffer sb = new StringBuffer(str);
            for (int i = 0; i < num - leng; i++) {
                sb.append(" ");
            }
            return sb.toString();
        } else if (leng > num) {
            return str.substring(0, num);
        } else {
            return str;
        }
    }

    /**
     * Pads the given String to the left with spaces to ensure that it's at least totalChars long.
     */
    public static String padLeft(String str, final int totalChars) {
        if (str == null)
            str = "null";
        final StringBuffer sb = new StringBuffer();
        for (int i = 0; i < totalChars - str.length(); i++) {
            sb.append(" ");
        }
        sb.append(str);
        return sb.toString();
    }

    public static String escapeString(final String s, final char[] charsToEscape, final char escapeChar) {
        final StringBuffer result = new StringBuffer();
        for (int i = 0; i < s.length(); i++) {
            final char c = s.charAt(i);
            if (c == escapeChar) {
                result.append(escapeChar);
            } else {
                for (int j = 0; j < charsToEscape.length; j++) {
                    if (c == charsToEscape[j]) {
                        result.append(escapeChar);
                        break;
                    }
                }
            }
            result.append(c);
        }
        return result.toString();
    }

}
