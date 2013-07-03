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

package edu.ohsu.cslu.grammar;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;

/**
 * @author Aaron Dunlop
 * @since Jul 2, 2013
 */
public class DecisionTreeTokenClassifier extends TokenClassifier {

    private static final long serialVersionUID = 1L;

    public DecisionTreeTokenClassifier(final SymbolSet<String> lexicon) {
        super(lexicon);
    }

    /**
     * Splits the supplied sentence on spaces and returns the lexicon-mapped indices of all words.
     * 
     * @param sentence
     * @return the lexicon-mapped indices of all words
     */
    @Override
    public int[] lexiconIndices(final String sentence) {
        // TODO This could probably be done faster with something other than a regex
        final String tokens[] = sentence.split("\\s+");
        final int tokenIndices[] = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tokenIndices[i] = lexiconIndex(tokens[i], i == 0);
        }
        return tokenIndices;
    }

    /**
     * Returns the lexicon-mapped indices of all words in the supplied parse tree
     * 
     * @param goldTree
     * @return the lexicon-mapped indices of all words in the supplied parse tree
     */
    @Override
    public int[] lexiconIndices(final NaryTree<String> goldTree) {
        final int tokenIndices[] = new int[goldTree.leaves()];
        int i = 0;
        for (final NaryTree<String> leaf : goldTree.leafTraversal()) {
            tokenIndices[i++] = lexiconIndex(leaf.label(), i == 0);
        }
        return tokenIndices;
    }

    public int lexiconIndex(final String word, final boolean sentenceInitial) {
        return lexicon.getIndex(lexiconEntry(word, sentenceInitial));
    }

    public String lexiconEntry(final String word, final boolean sentenceInitial) {
        if (lexicon.containsKey(word)) {
            return word;
        }
        String unkStr = DecisionTreeTokenClassifier.berkeleyGetSignature(word, sentenceInitial, lexicon);

        // remove last feature from unk string until we find a matching entry in the lexicon
        while (!lexicon.containsKey(unkStr) && unkStr.contains("-")) {
            unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
        }

        if (lexicon.containsKey(unkStr) == false) {
            throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
        }

        return unkStr;
    }

    /**
     * Returns a String that is the "signature" of the class of a word. Adapted from the Berkeley Parser version in
     * SimpleLexicon.getNewSignature(). Features represent whether the token contains a numeral, whether it in -s, etc..
     * The strings returned by convention match the pattern UNK-.* , which is assumed to not match any real word. The
     * decision-tree (and particularly the suffix-handling) is fairly English-specific.
     * 
     * @param word The word to make a signature for
     * @param sentenceInitial True if the word occurs as the first in the sentence (so sentence-initial capitalized
     *            words can be treated differently)
     * @return A String that is its signature (equivalence class)
     */
    public static String berkeleyGetSignature(final String word, final boolean sentenceInitial,
            final SymbolSet<String> lexSet) {
        final StringBuilder sb = new StringBuilder(12);
        sb.append("UNK");

        // Reformed Mar 2004 (cdm); hopefully much better now.
        // { -CAPS, -INITC ap, -LC lowercase, 0 } +
        // { -KNOWNLC, 0 } + [only for INITC]
        // { -NUM, 0 } +
        // { -DASH, 0 } +
        // { -last lowered char(s) if known discriminating suffix, 0}
        final int wlen = word.length();
        int numCaps = 0;
        boolean hasDigit = false;
        boolean hasDash = false;
        boolean hasLower = false;
        for (int i = 0; i < wlen; i++) {
            final char ch = word.charAt(i);
            if (Character.isDigit(ch)) {
                hasDigit = true;
            } else if (ch == '-') {
                hasDash = true;
            } else if (Character.isLetter(ch)) {
                if (Character.isLowerCase(ch)) {
                    hasLower = true;
                } else if (Character.isTitleCase(ch)) {
                    hasLower = true;
                    numCaps++;
                } else {
                    numCaps++;
                }
            }
        }
        final char ch0 = word.charAt(0);
        final String lowered = word.toLowerCase();
        if (Character.isUpperCase(ch0) || Character.isTitleCase(ch0)) {
            if (sentenceInitial && numCaps == 1) {
                sb.append("-INITC");
                // Condition assures word != lowered
                if (lexSet != null && lexSet.containsKey(lowered)) {
                    sb.append("-KNOWNLC");
                }
            } else {
                sb.append("-CAPS");
            }
        } else if (!Character.isLetter(ch0) && numCaps > 0) {
            sb.append("-CAPS");
        } else if (hasLower) { // (Character.isLowerCase(ch0)) {
            sb.append("-LC");
        }
        if (hasDigit) {
            sb.append("-NUM");
        }
        if (hasDash) {
            sb.append("-DASH");
        }
        if (lowered.endsWith("s") && wlen >= 3) {
            // here length 3, so you don't miss out on ones like 80s
            final char ch2 = lowered.charAt(wlen - 2);
            // not -ess suffixes or greek/latin -us, -is
            if (ch2 != 's' && ch2 != 'i' && ch2 != 'u') {
                sb.append("-s");
            }
        } else if (word.length() >= 5 && !hasDash && !(hasDigit && numCaps > 0)) {
            // don't do for very short words;
            // Implement common discriminating suffixes
            /*
             * if (Corpus.myLanguage==Corpus.GERMAN){ sb.append(lowered.substring(lowered.length()-1)); }else{
             */
            if (lowered.endsWith("ed")) {
                sb.append("-ed");
            } else if (lowered.endsWith("ing")) {
                sb.append("-ing");
            } else if (lowered.endsWith("ion")) {
                sb.append("-ion");
            } else if (lowered.endsWith("er")) {
                sb.append("-er");
            } else if (lowered.endsWith("est")) {
                sb.append("-est");
            } else if (lowered.endsWith("ly")) {
                sb.append("-ly");
            } else if (lowered.endsWith("ity")) {
                sb.append("-ity");
            } else if (lowered.endsWith("y")) {
                sb.append("-y");
            } else if (lowered.endsWith("al")) {
                sb.append("-al");
                // } else if (lowered.endsWith("ble")) {
                // sb.append("-ble");
                // } else if (lowered.endsWith("e")) {
                // sb.append("-e");
            }
        }

        return sb.toString();
    }

    @Override
    public TokenClassifierType type() {
        return TokenClassifierType.DecisionTree;
    }

}
