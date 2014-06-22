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

package edu.ohsu.cslu.grammar;

/**
 * @author Aaron Dunlop
 * @since Jul 2, 2013
 */
public class Tokenizer {

    /**
     * Performs standard Penn-Treebank-style tokenization, including marking special characters such as brackets,
     * quotes, etc. The behavior is similar to that of the sed script at
     * http://www.cis.upenn.edu/~treebank/tokenizer.sed, but adds some additional tokenizations targeted at
     * non-canonical genres.
     * 
     * Note: this implementation uses a lot of on-the-fly regex operations, so it isn't very efficient. It's not a
     * bottleneck for parsing, but probably is for finite-state operations like POS tagging.
     * 
     * @param sentence A single untokenized sentence.
     * @return Tokenized sentence, with tokens separated by single spaces
     */
    public static String treebankTokenize(final String sentence) {
        String s = sentence;
        // Directional open and close quotes
        s = s.replaceAll("^\"", "`` ");
        s = s.replaceAll("([ \\(\\[{<])\"", "$1 `` ");
        s = s.replaceAll("\"", " ''");

        // Add spaces around question marks, exclamation points, and other punctuation (excluding periods)
        s = s.replaceAll("([,;@#$%&?!\\]])", " $1 ");

        // Split _final_ periods only
        s = s.replaceAll("[.]$", " .");
        s = s.replaceAll("[.] ([\\[\\({}\\)\\]\"']*)$", " . $1");

        // The Penn Treebank splits Ph.D. -> 'Ph. D.', so we'll special-case that
        s = s.replaceAll("Ph\\.D\\.", "Ph. D.");

        // Segment ellipses and re-collapse if it was split
        s = s.replaceAll("\\.\\. ?\\.", " ...");

        // Parentheses, brackets, etc.
        s = s.replaceAll(" *\\(", " -LRB- ");
        s = s.replaceAll("\\)", " -RRB-");
        s = s.replaceAll(" *\\[", " -LSB- ");
        s = s.replaceAll("\\]", " -RSB-");
        s = s.replaceAll(" *\\{", " -LCB- ");
        s = s.replaceAll("\\}", " -RCB-");
        s = s.replaceAll("--", " -- ");

        s = s.replaceAll("$", " ");
        s = s.replaceAll("^", " ");

        s = s.replaceAll("([^'])' ", "$1 ' ");

        // Possessives, contractions, etc.
        s = s.replaceAll("'([sSmMdD]) ", " '$1 ");
        s = s.replaceAll("'ll ", " 'll ");
        s = s.replaceAll("'re ", " 're ");
        s = s.replaceAll("'ve ", " 've ");
        s = s.replaceAll("n't ", " n't ");
        s = s.replaceAll("'LL ", " 'LL ");
        s = s.replaceAll("'RE ", " 'RE ");
        s = s.replaceAll("'VE ", " 'VE ");
        s = s.replaceAll("N'T ", " N'T ");

        // Contractions and pseudo-words
        s = s.replaceAll(" ([Cc])annot ", " $1an not ");
        s = s.replaceAll(" ([Dd])'ye ", " $1' ye ");
        s = s.replaceAll(" ([Gg])imme ", " $1im me ");
        s = s.replaceAll(" ([Gg])onna ", " $1on na ");
        s = s.replaceAll(" ([Gg])otta ", " $1ot ta ");
        s = s.replaceAll(" ([Ll])emme ", " $1em me ");
        s = s.replaceAll(" ([Mm])ore'n ", " $1ore 'n ");
        s = s.replaceAll(" '([Tt])is ", " $1 is ");
        s = s.replaceAll(" '([Tt])was ", " $1 was ");
        s = s.replaceAll(" ([Ww])anna ", " $1an na ");

        // Remove spaces from abbreviations
        s = s.replaceAll(" ([A-Z]) \\.", " $1. ");

        // Collapse multiple spaces and trim whitespace from beginning and end
        return s.replaceAll("\\s+", " ").trim();
    }

}
