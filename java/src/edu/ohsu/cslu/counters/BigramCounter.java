/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the cslu-common project.
 * 
 * cslu-common is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as 
 * published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * cslu-common is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with cslu-common. If not, see <http://www.gnu.org/licenses/>
 */
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

    private static final long serialVersionUID = 1L;

    public BigramCounter(final Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            countSentence(line.split(" "));
        }
        br.close();

        trim();
    }

    protected void countSentence(final String[] wordArray) {
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
