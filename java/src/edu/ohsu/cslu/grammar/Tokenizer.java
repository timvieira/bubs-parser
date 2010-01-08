package edu.ohsu.cslu.grammar;

import edu.ohsu.cslu.parser.util.ParserUtil;

public class Tokenizer {

    private SymbolSet<String> lexSet;

    public Tokenizer(final SymbolSet<String> lexSet) {
        this.lexSet = lexSet;
    }

    public class Token {

        public String word;
        public int index;
        private boolean isUnk;

        public Token(final String word) {
            this.word = word;
            setIndexAndUnk();
        }

        @Override
        public String toString() {
            return this.toString(false);
        }

        public boolean isUnk() {
            return this.isUnk;
        }

        public String toString(final boolean appendUnkStr) {
            if (appendUnkStr == true && isUnk() == true) {
                return word + "::" + lexSet.getSymbol(index);
            }
            return word;
        }

        private void setIndexAndUnk() {
            assert this.word != null;
            String unkStr;

            if (lexSet.hasLabel(word)) {
                this.index = lexSet.getIndex(word);
                this.isUnk = false;
            } else {
                this.isUnk = true;
                unkStr = wordToUnkString(word);
                // remove last feature from unk string until we find a matching entry in the lexicon
                while (!lexSet.hasLabel(unkStr) && unkStr.contains("-")) {
                    unkStr = unkStr.substring(0, unkStr.lastIndexOf('-'));
                }

                if (lexSet.hasLabel(unkStr) == false) {
                    throw new IllegalArgumentException("Word 'UNK' not found in lexicon");
                }
                this.index = lexSet.getIndex(unkStr);
            }
        }
    }

    public Token[] tokenize(final String sentence) {
        final String tokens[] = ParserUtil.tokenize(sentence);
        final Token[] sentTokens = new Token[tokens.length];

        for (int i = 0; i < tokens.length; i++) {
            sentTokens[i] = new Token(tokens[i]);
        }
        return sentTokens;
    }

    // taken from Berkeley Parser (getSignature)
    public static String wordToUnkString(final String word) {
        String unkStr = "UNK";

        // word case
        if (ParserUtil.isLowerCase(word))
            unkStr += "-LC";
        else if (ParserUtil.isUpperCase(word))
            unkStr += "-CAPS";
        else if (ParserUtil.isUpperCase(word.substring(0, 1)) && ParserUtil.isLowerCase(word.substring(1)))
            unkStr += "-INITC";

        // if (lexSet.hasLabel(word.toLowerCase())) unkStr += "-KNOWNLC";
        if (ParserUtil.containsDigit(word))
            unkStr += "-DIGIT";
        if (word.contains("-"))
            unkStr += "-DASH";

        final String lcWord = word.toLowerCase();

        if (lcWord.endsWith("s") && !lcWord.endsWith("ss") && !lcWord.endsWith("us") && !lcWord.endsWith("is"))
            unkStr += "-s";
        if (lcWord.endsWith("ed"))
            unkStr += "-ed";
        if (lcWord.endsWith("ing"))
            unkStr += "-ing";
        if (lcWord.endsWith("ion"))
            unkStr += "-ion";
        if (lcWord.endsWith("er"))
            unkStr += "-er";
        if (lcWord.endsWith("est"))
            unkStr += "-est";
        if (lcWord.endsWith("al"))
            unkStr += "-al";

        if (lcWord.endsWith("ity"))
            unkStr += "-ity";
        else if (lcWord.endsWith("ly"))
            unkStr += "-ly";
        else if (lcWord.endsWith("y"))
            unkStr += "-y";

        return unkStr;
    }
}
