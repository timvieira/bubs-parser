package edu.ohsu.cslu.matching.approximate;

import java.io.BufferedReader;
import java.io.FileReader;

import edu.ohsu.cslu.matching.exact.NaiveMatcher;

/**
 * Matches a pattern in a text file.
 * 
 * TODO: Refactor to extend BaseCommandLineTool
 * 
 * @author Aaron Dunlop
 * @since Oct 2, 2008
 * 
 *        $Id$
 */
public class ApproximateMatch {

    public final static int FULL_DP_LIMIT = 250000000;

    /**
     * @param args
     */
    public static void main(String[] args) {
        String usage = "Usage: java ApproximateMatch [-?] [-v] [-k edits] pattern text.file";

        String pattern = null, text = null;
        String textFileName = null;
        String tmp = null;
        int edits = 0;

        for (int i = 0; i < args.length; i++) {
            if ("-k".equals(args[i])) {
                edits = (new Integer(args[i + 1])).intValue();
                i++;
            } else if (i == args.length - 2) {
                pattern = args[i];
            } else if (i == args.length - 1) {
                textFileName = args[i];
            } else {
                System.err.println(usage);
                System.exit(1);
            }
        }

        try {
            BufferedReader txtin = new BufferedReader(new FileReader(textFileName));
            StringBuffer textBuf = new StringBuffer();
            while ((tmp = txtin.readLine()) != null) {
                textBuf.append("\n"); // it's what the c does
                textBuf.append(tmp);
            }
            text = new String(textBuf);
        } catch (java.io.IOException ioex) {
            System.err.println("Couldn't open file (spelling?), exiting. " + ioex);
            System.exit(1);
        }

        System.out.println("====================");
        long startTime = System.nanoTime();
        int matches = new NaiveMatcher().matches(pattern, text);
        long endTime = System.nanoTime();
        System.out.println("Naive Exact Algorithm: " + matches + " matches in "
                + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000)
                + " microseconds)");

        System.out.println("====================");
        if (pattern != null && text != null && (long) pattern.length() * (long) text.length() > FULL_DP_LIMIT)
            System.out.println("full dynamic programming algorithm: too much memory required\n");
        else {
            startTime = System.nanoTime();
            matches = new FullDynamicMatcher(99, 100).matches(pattern, text, edits);
            endTime = System.nanoTime();
            System.out.println("Full Dynamic Programming Algorithm: " + matches + " matches in "
                    + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000)
                    + " microseconds)");
        }

        System.out.println("====================");
        startTime = System.nanoTime();
        matches = new LinearDynamicMatcher(99, 100).matches(pattern, text, edits);
        endTime = System.nanoTime();
        System.out.println("Linear Space Dynamic Programming Algorithm: " + matches + " matches in "
                + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000)
                + " microseconds)");

        System.out.println("====================");
        startTime = System.nanoTime();
        matches = new BaezaYatesPerlbergMatcher(99, 100).matches(pattern, text, edits);
        endTime = System.nanoTime();
        System.out.println("Aho-Corasick (Baeza-Yates/Perlberg) Algorithm: " + matches + " matches in "
                + ((endTime - startTime) / 1000000) + " milliseconds (" + ((endTime - startTime) / 1000)
                + " microseconds)");

        System.exit(0);
    }

}
