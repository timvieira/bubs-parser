package edu.ohsu.cslu.counters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.TreeSet;

/**
 * Counts occurrences within a sentence without regard to word order.
 * 
 * @author Aaron Dunlop
 * @since Aug 19, 2008
 * 
 *        $Id$
 */
public class SententialCoocurrenceCounter extends CoocurrenceCounter {

    public SententialCoocurrenceCounter(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            countSentence(line.split(" "));
        }
        br.close();

        trim();
    }

    protected void countSentence(String[] wordArray) {
        final TreeSet<String> words = new TreeSet<String>();

        for (final String word : wordArray) {
            words.add(word);
        }

        for (final String h : words) {
            incrementCount(h);

            for (final String w : words.tailSet(h, false)) {
                incrementCount(h, w);
            }
        }
    }
}
