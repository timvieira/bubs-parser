package edu.ohsu.cslu.grammar;

import java.io.Serializable;

import edu.ohsu.cslu.parser.util.ParserUtil;

public class Tokenizer implements Serializable {

    private SymbolSet<String> lexSet;

    public Tokenizer(final SymbolSet<String> lexSet) {
        this.lexSet = lexSet;
    }

    public String[] tokenize(final String sentence) {
        final String tokens[] = ParserUtil.tokenize(sentence);
        for (int i = 0; i < tokens.length; i++) {
            tokens[i] = mapToLexSetEntry(tokens[i]);
        }
        return tokens;
    }

    public int[] tokenizeToIndex(final String sentence) {
        final String tokens[] = ParserUtil.tokenize(sentence);
        final int tokenIndices[] = new int[tokens.length];
        for (int i = 0; i < tokens.length; i++) {
            tokenIndices[i] = lexSet.getIndex(mapToLexSetEntry(tokens[i]));
        }
        return tokenIndices;
    }

    public String mapToLexSetEntry(final String word) {
        if (lexSet.hasSymbol(word)) {
            return word;
        }

        String unkStr = wordToUnkString(word);
        // remove last feature from unk string until we find a matching entry in the lexicon
        while (!lexSet.hasSymbol(unkStr) && unkStr.contains("-")) {
            unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
        }

        if (lexSet.hasSymbol(unkStr) == false) {
            throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
        }

        return unkStr;
    }

    // taken from Berkeley Parser (getSignature)
    public static String wordToUnkString(final String word) {
        String unkStr = "UNK";

        // word case
        if (isLowerCase(word)) {
            unkStr += "-LC";
        } else if (isUpperCase(word)) {
            unkStr += "-CAPS";
        } else if (isUpperCase(word.substring(0, 1)) && isLowerCase(word.substring(1))) {
            unkStr += "-INITC";
        }

        // if (lexSet.hasLabel(word.toLowerCase())) unkStr += "-KNOWNLC";
        if (containsDigit(word)) {
            unkStr += "-DIGIT";
        } else if (word.contains("-")) {
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
