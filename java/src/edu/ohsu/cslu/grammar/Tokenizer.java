/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;

public class Tokenizer implements Serializable {

    private static final long serialVersionUID = 1L;

    private SymbolSet<String> lexSet;

    public Tokenizer(final SymbolSet<String> lexSet) {
        this.lexSet = lexSet;
    }

    public Tokenizer(final String lexCountFile) throws NumberFormatException, IOException {
        this(lexCountFile, 0);
    }

    // Build tokenizer from lexical count file with entries per line: <count> <word>
    public Tokenizer(final String lexCountFile, final int unkThresh) throws NumberFormatException, IOException {
        this.lexSet = new SymbolSet<String>();
        this.lexSet.addSymbol("UNK");
        final SymbolSet<String> unkWords = new SymbolSet<String>();

        // must build complete lexSet before adding UNKs because
        // it matches to the set of UNKs in the lexSet.
        final HashMap<String, Integer> lexCounts = readLexCountFile(lexCountFile);
        for (final Entry<String, Integer> entry : lexCounts.entrySet()) {
            if (entry.getValue() > unkThresh) {
                lexSet.addSymbol(entry.getKey());
            } else {
                unkWords.addSymbol(entry.getKey());
            }
        }

        for (final String word : unkWords) {
            lexSet.addSymbol(wordToUnkString(word, false));
            lexSet.addSymbol(wordToUnkString(word, true));
        }
    }

    public static HashMap<String, Integer> readLexCountFile(final String fileName) throws NumberFormatException,
            IOException {
        final HashMap<String, Integer> lexCounts = new HashMap<String, Integer>();
        String line;
        final BufferedReader f = new BufferedReader(new FileReader(fileName));
        while ((line = f.readLine()) != null) {
            final String[] toks = line.split("[ \t]+");
            if (toks.length == 2) {
                final int count = Integer.parseInt(toks[0]);
                lexCounts.put(toks[1], count);
            } else {
                System.err.println("WARNING: Unexpected line in lex count file: '" + line.trim() + "'");
            }
        }
        return lexCounts;
    }

    public boolean hasWord(final String word) {
        return lexSet.containsKey(word);
    }

    public int lexSize() {
        lexSet.finalize(); // After getting the size, make sure it doesn't change.
        return lexSet.size();
    }

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

    public String[] tokenize(final String sentence) {
        final String treebankTokens[] = treebankTokenize(sentence).split(" ");
        for (int i = 0; i < treebankTokens.length; i++) {
            treebankTokens[i] = wordToLexSetEntry(treebankTokens[i], i == 0);
        }
        return treebankTokens;
    }

    public int[] tokenizeToIndex(final String sentence) {
        // TODO This could probably be done faster with something other than a regex
        final String tokens[] = sentence.split("\\s+");
        final int tokenIndices[] = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tokenIndices[i] = wordToLexSetIndex(tokens[i], i == 0);
        }
        return tokenIndices;
    }

    public int[] tokenizeToIndex(final NaryTree<String> sentence) {
        final int tokenIndices[] = new int[sentence.leaves()];
        int i = 0;
        for (final NaryTree<String> leaf : sentence.leafTraversal()) {
            tokenIndices[i++] = wordToLexSetIndex(leaf.label(), i == 0);
        }
        return tokenIndices;
    }

    public int wordToLexSetIndex(final String word, final boolean sentenceInitial) {
        return lexSet.getIndex(wordToLexSetEntry(word, sentenceInitial));
    }

    public String wordToLexSetEntry(final String word, final boolean sentenceInitial) {
        if (lexSet.containsKey(word)) {
            return word;
        }

        return unkToUnkEntry(wordToUnkString(word, sentenceInitial));
    }

    public String wordToUnkEntry(final String word, final boolean sentenceInitial) {
        return unkToUnkEntry(wordToUnkString(word, sentenceInitial));
    }

    public String unkToUnkEntry(String unkStr) {
        // remove last feature from unk string until we find a matching entry in the lexicon
        while (!lexSet.containsKey(unkStr) && unkStr.contains("-")) {
            unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
        }

        if (lexSet.containsKey(unkStr) == false) {
            throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
        }

        return unkStr;
    }

    public String wordToUnkString(final String word, final boolean sentenceInitial) {
        return berkeleyGetSignature(word, sentenceInitial, lexSet);
    }

    // taken from Berkeley Parser SimpleLexicon.getNewSignature

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
}
