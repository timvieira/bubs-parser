package edu.ohsu.cslu.parser.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringToMD5 {

    public static final String computeMD5(String input) throws NoSuchAlgorithmException {

        StringBuffer sbuf = new StringBuffer();
        MessageDigest md = MessageDigest.getInstance("MD5");
        byte[] raw = md.digest(input.getBytes());

        for (int i = 0; i < raw.length; i++) {
            int c = (int) raw[i];
            if (c < 0) {
                c = (Math.abs(c) - 1) ^ 255;
            }
            String block = toHex(c >>> 4) + toHex(c & 15);
            sbuf.append(block);
        }

        return sbuf.toString();

    }

    private static final String toHex(int s) {
        if (s < 10) {
            return new StringBuffer().append((char) ('0' + s)).toString();
        } else {
            return new StringBuffer().append((char) ('A' + (s - 10))).toString();
        }
    }

}
