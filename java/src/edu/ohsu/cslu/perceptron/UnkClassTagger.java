/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.perceptron;

import edu.ohsu.cslu.util.Strings;

/**
 * Classifies unknown-words into clusters, using lexical features and the surrounding syntax.
 * 
 * @author Aaron Dunlop
 * @since Jul 1, 2013
 */
public class UnkClassTagger extends Tagger {

    @Override
    protected void run() throws Exception {
        boolean foundInput = false;
        int expectedFields = 0;
        for (final String line : inputLines()) {
            // Skip everything up to and including '@data'
            if (!foundInput) {
                if (line.equals("@data")) {
                    foundInput = true;
                }
                continue;
            }

            final String[] split = Strings.splitOn(line, ',', '\'');
            if (expectedFields == 0) {
                expectedFields = split.length;
                System.out.format("%d fields\n", expectedFields);
            }

            if (split.length != expectedFields) {
                System.err.println(line);
            }
        }
    }

    /**
     * @param args
     */
    public static void main(final String[] args) {
        run(args);
    }

}
