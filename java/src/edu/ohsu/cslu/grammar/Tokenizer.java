package edu.ohsu.cslu.grammar;

import java.io.Serializable;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.ParserUtil;

public class Tokenizer implements Serializable {

    private SymbolSet<String> lexSet;

    public Tokenizer(final SymbolSet<String> lexSet) {
        this.lexSet = lexSet;
    }

    public String[] tokenize(final String sentence) {
        final String tokens[] = ParserUtil.tokenize(sentence);
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = mapToLexSetEntry(tokens[i], i);
        }
        return tokens;
    }

    public int[] tokenizeToIndex(final String sentence) {
        final String tokens[] = ParserUtil.tokenize(sentence);
        final int tokenIndices[] = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tokenIndices[i] = lexSet.getIndex(mapToLexSetEntry(tokens[i], i));
        }
        return tokenIndices;
    }

    public int[] tokenizeToIndex(final NaryTree<String> sentence) {
        final int tokenIndices[] = new int[sentence.leaves()];
        int i = 0;
        for (final Iterator<NaryTree<String>> iter = sentence.leafIterator(); iter.hasNext();) {
            tokenIndices[i++] = lexSet.getIndex(mapToLexSetEntry(iter.next().label(), i));
        }
        return tokenIndices;
    }

    public String mapToLexSetEntry(final String word, final int sentIndex) {
        if (lexSet.hasSymbol(word)) {
            return word;
        }

        String unkStr = wordToUnkString(word, sentIndex);
        // remove last feature from unk string until we find a matching entry in the lexicon
        while (!lexSet.hasSymbol(unkStr) && unkStr.contains("-")) {
            unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
        }

        if (lexSet.hasSymbol(unkStr) == false) {
            throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
        }

        return unkStr;
    }

    public String wordToUnkString(final String word, final int sentIndex) {
        if (ParserDriver.oldUNK == true) {
            return wordToUnkStringVer1(word);
        }
        return berkeleyGetSignature(word, sentIndex);
    }

    // try to match Berkeley UNK mapping so that we can use their grammar
    private String wordToUnkStringVer1(final String word) {
        String unkStr = "UNK";

        // word case
        if (isLowerCase(word)) {
            unkStr += "-LC";
        } else if (isUpperCase(word)) {
            unkStr += "-CAPS";
        } else if (isUpperCase(word.substring(0, 1)) && isLowerCase(word.substring(1))) {
            unkStr += "-INITC";
        }

        if (lexSet.hasSymbol(word.toLowerCase())) {
            unkStr += "-KNOWNLC";
        }

        if (containsDigit(word)) {
            unkStr += "-NUM";
        }

        if (word.substring(1).contains("-")) { // don't want negative symbol
            unkStr += "-DASH";
        }

        final String lcWord = word.toLowerCase();

        if (lcWord.endsWith("s") && !lcWord.endsWith("ss") && !lcWord.endsWith("us") && !lcWord.endsWith("is")) {
            unkStr += "-s";
        } else if (lcWord.endsWith("ed")) {
            unkStr += "-ed";
        } else if (lcWord.endsWith("ing")) {
            unkStr += "-ing";
        } else if (lcWord.endsWith("ion")) {
            unkStr += "-ion";
        } else if (lcWord.endsWith("er")) {
            unkStr += "-er";
        } else if (lcWord.endsWith("est")) {
            unkStr += "-est";
        } else if (lcWord.endsWith("al")) {
            unkStr += "-al";
        } else if (lcWord.endsWith("ity")) {
            unkStr += "-ity";
        } else if (lcWord.endsWith("ly")) {
            unkStr += "-ly";
        } else if (lcWord.endsWith("y")) {
            unkStr += "-y";
        }

        return unkStr;
    }

    // taken from Berkeley Parser SimpleLexicon.getNewSignature

    /**
     * This routine returns a String that is the "signature" of the class of a word. For, example, it might represent
     * whether it is a number of ends in -s. The strings returned by convention match the pattern UNK-.* , which is just
     * assumed to not match any real word. Behavior depends on the unknownLevel (-uwm flag) passed in to the class. The
     * recognized numbers are 1-5: 5 is fairly English-specific; 4, 3, and 2 look for various word features (digits,
     * dashes, etc.) which are only vaguely English-specific; 1 uses the last two characters combined with a simple
     * classification by capitalization.
     * 
     * @param word The word to make a signature for
     * @param loc Its position in the sentence (mainly so sentence-initial capitalized words can be treated differently)
     * @return A String that is its signature (equivalence class)
     */
    private String berkeleyGetSignature(final String word, final int sentIndex) {
        final StringBuffer sb = new StringBuffer("UNK");

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
            if (sentIndex == 0 && numCaps == 1) {
                sb.append("-INITC");
                // if (isKnown(lowered)) {
                if (lexSet.hasSymbol(lowered)) {
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

    private static boolean isUpperCase(final String s) {
        return s == s.toUpperCase();
    }

    private static boolean isLowerCase(final String s) {
        return s == s.toLowerCase();
    }

    private static boolean containsDigit(final String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.isDigit(s.charAt(i)))
                return true;
        }
        return false;
    }

    // public Token[] tokenize(final String sentence) throws Exception {
    // final String tokens[] = ParserUtil.tokenize(sentence);
    // final Token[] sentTokens = new Token[tokens.length];
    //
    // for (int i = 0; i < tokens.length; i++) {
    // sentTokens[i] = new Token(tokens[i]);
    // }
    // return sentTokens;
    // }

    // public class Token {
    //
    // public String word, origWord;
    // public int index;
    // private boolean isUnk;
    //
    // public Token(final String word) throws Exception {
    // this.origWord = word;
    // setIndexAndUnk();
    // }
    //
    // @Override
    // public String toString() {
    // return this.toString(false);
    // }
    //
    // public boolean isUnk() {
    // return this.isUnk;
    // }
    //
    // public String toString(final boolean appendUnkStr) {
    // if (appendUnkStr == true && isUnk() == true) {
    // return origWord + "::" + word;
    // }
    // return origWord;
    // }
    //
    // // public String getToken(String wordStr) {
    // // if (lexSet.hasSymbol(word)) {
    // // return wordStr;
    // // }
    // // wordStr = wordToUnkString(wordStr);
    // // // remove last feature from unk string until we find a matching entry in the lexicon
    // // while (!lexSet.hasSymbol(wordStr) && wordStr.contains("-")) {
    // // wordStr = wordStr.substring(0, wordStr.lastIndexOf('-'));
    // // }
    // //
    // // if (lexSet.hasSymbol(wordStr) == false) {
    // // throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
    // // }
    // // return wordStr;
    // // }
    //
    // private void setIndexAndUnk() throws Exception {
    // assert this.origWord != null;
    // String unkStr;
    //
    // if (lexSet.hasSymbol(origWord)) {
    // this.index = lexSet.getIndex(origWord);
    // this.isUnk = false;
    // } else {
    // this.isUnk = true;
    // unkStr = wordToUnkString(origWord);
    // // remove last feature from unk string until we find a matching entry in the lexicon
    // while (!lexSet.hasSymbol(unkStr) && unkStr.contains("-")) {
    // unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
    // }
    //
    // if (lexSet.hasSymbol(unkStr) == false) {
    // throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
    // }
    //
    // this.word = unkStr;
    // this.index = lexSet.getIndex(unkStr);
    // }
    // }
    // }
}
