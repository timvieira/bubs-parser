package edu.ohsu.cslu.counters;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;

/**
 * Counts occurrences within a sentence, taking word order into account.
 * 
 * @author Aaron Dunlop
 * @since Aug 19, 2008
 * 
 *        $Id$
 */
public class BigramCounter extends CoocurrenceCounter {

    public BigramCounter(Reader reader) throws IOException {
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            countSentence(line.split(" "));
        }
        br.close();

        trim();
    }

    protected void countSentence(String[] wordArray) {
        // Count the first word separately
        incrementCount(wordArray[0]);

        // Count all bigram pairs
        for (int i = 1; i < wordArray.length; i++) {
            final String w = wordArray[i];
            incrementCount(w);
            incrementCount(wordArray[i - 1], w);
        }
    }

}
