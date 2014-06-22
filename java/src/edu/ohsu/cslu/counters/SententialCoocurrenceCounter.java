/*
 * Copyright 2010-2014, Oregon Health & Science University
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
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

    private static final long serialVersionUID = 1L;

    public SententialCoocurrenceCounter(final Reader reader) throws IOException {
        final BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            countSentence(line.split(" "));
        }
        br.close();

        trim();
    }

    protected void countSentence(final String[] wordArray) {
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
