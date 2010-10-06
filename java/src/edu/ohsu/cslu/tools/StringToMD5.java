package edu.ohsu.cslu.tools;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class StringToMD5 {

    public static final String computeMD5(final String input) throws NoSuchAlgorithmException {

        final StringBuffer sbuf = new StringBuffer();
        final MessageDigest md = MessageDigest.getInstance("MD5");
        final byte[] raw = md.digest(input.getBytes());

        for (int i = 0; i < raw.length; i++) {
            int c = raw[i];
            if (c < 0) {
                c = (Math.abs(c) - 1) ^ 255;
            }
            final String block = toHex(c >>> 4) + toHex(c & 15);
            sbuf.append(block);
        }

        return sbuf.toString();

    }

    private static final String toHex(final int s) {
        if (s < 10) {
            return new StringBuffer().append((char) ('0' + s)).toString();
        }

        return new StringBuffer().append((char) ('A' + (s - 10))).toString();
    }

}
