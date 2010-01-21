package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.parser.util.Log;

public abstract class BaseGrammar implements Grammar {
    protected Collection<Production> binaryProductions;
    protected Collection<Production> unaryProductions;
    public int startSymbol = -1;
    public int nullSymbol = -1;
    public String nullSymbolStr = "<null>";

    // private LinkedList<LexicalProduction>[] lexicalProds = new LinkedList<UnaryProduction>[5];
    // note: java doesn't allow generic array creation, so must do it like this:
    private List<Production>[] lexicalProds;
    public int numLexProds;
    public final SymbolSet<String> nonTermSet;
    public final SymbolSet<Integer> posSet; // index into nonTermSet of POS non terms
    private final SymbolSet<String> lexSet;
    private final Tokenizer tokenizer;
    private int maxPOSIndex = -1; // TODO: should sort nonterms so ordered by POS then NTs

    protected BaseGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        nonTermSet = new SymbolSet<String>();
        posSet = new SymbolSet<Integer>();
        lexSet = new SymbolSet<String>();

        init(grammarFile, lexiconFile, grammarFormat);

        tokenizer = new Tokenizer(lexSet);
    }

    protected BaseGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws IOException {
        Log.info(1, "INFO: Reading grammar");

        // the nullSymbol is used for start/end of sentence markers and dummy non-terminals
        nullSymbol = nonTermSet.addSymbol(nullSymbolStr);
        posSet.addSymbol(nullSymbol);

        readGrammar(grammarFile, grammarFormat);
        if (startSymbol == -1) {
            throw new IllegalArgumentException("No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        Log.info(1, "INFO: Reading lexical productions");
        readLexProds(lexiconFile);
    }

    @SuppressWarnings("unchecked")
    private void readLexProds(final Reader lexFile) throws IOException {
        String line;
        String[] tokens;
        Production lexProd;
        final List<Production> tmpProdList = new LinkedList<Production>();

        final BufferedReader br = new BufferedReader(lexFile);
        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                lexProd = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), true);
                tmpProdList.add(lexProd);
            } else {
                throw new IllegalArgumentException("Unexpected line in lexical file\n\t" + line);
            }
        }

        // store lexical prods indexed by the word
        final ArrayList<List<Production>> tmpLexicalProds = new ArrayList<List<Production>>(lexSet.numSymbols());
        for (int i = 0; i < lexSet.numSymbols(); i++) {
            tmpLexicalProds.add(null);
        }

        for (final Production p : tmpProdList) {
            if (tmpLexicalProds.get(p.leftChild) == null) {
                tmpLexicalProds.set(p.leftChild, new LinkedList<Production>());
            }
            tmpLexicalProds.get(p.leftChild).add(p);
            numLexProds++;
        }
        lexicalProds = tmpLexicalProds.toArray(new LinkedList[tmpLexicalProds.size()]);
    }

    private void readGrammar(final Reader gramFile, final GrammarFormatType grammarFormat) throws IOException {
        final BufferedReader br = new BufferedReader(gramFile);

        String line;
        String[] tokens;
        Production prod;

        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();

        if (grammarFormat == GrammarFormatType.Roark) {
            startSymbol = nonTermSet.addSymbol("TOP");
        }

        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 1) {

                if (startSymbol != -1) {
                    throw new IllegalArgumentException(
                            "Grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: " + line);
                }

                startSymbol = nonTermSet.getIndex(tokens[0]);
            } else if (tokens.length == 4) {
                // expecting: A -> B prob
                prod = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), false);
                // should we make sure there aren't any duplicates?
                unaryProductions.add(prod);
            } else if (tokens.length == 5) {
                // expecting: A -> B C prob
                prod = new Production(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4]));
                binaryProductions.add(prod);
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar file\n\t" + line);
            }
        }
    }

    public final Token[] tokenize(final String sentence) {
        return tokenizer.tokenize(sentence);
    }

    public final int numNonTerms() {
        return nonTermSet.numSymbols();
    }

    public final int numLexSymbols() {
        return lexSet.size();
    }

    public final int numPosSymbols() {
        return posSet.size();
    }

    public final String startSymbol() {
        return nonTermSet.getSymbol(startSymbol);
    }

    public final boolean hasWord(final String s) {
        return lexSet.hasSymbol(s);
    }

    public final int getNonTermIndex(final String token) {
        return nonTermSet.getIndex(token);
    }

    public final int maxPOSIndex() {
        return maxPOSIndex;
    }

    public final String mapNonterminal(final int nonterminal) {
        return nonTermSet.getSymbol(nonterminal);
    }

    public final String mapLexicalEntry(final int lexicalEntry) {
        return lexSet.getSymbol(lexicalEntry);
    }

    public final List<Production> getLexProdsForToken(final Token token) {
        /*
         * if (token.isUnk()) { // make new lexical prods for the UNK words that will be deleted after parsing the sentence List<Production> unkProds = new
         * LinkedList<Production>(); for (Production p : lexicalProds.get(token.index)){ unkProds.add(p.copy()); }
         * 
         * } else { return lexicalProds.get(token.index); }
         */
        return lexicalProds[token.index];
    }

    // TODO: not efficient. Should index by child
    public List<Production> getUnaryProdsWithChild(final int child) {
        final List<Production> matchingProds = new LinkedList<Production>();
        for (final Production p : unaryProductions) {
            if (p.leftChild == child)
                matchingProds.add(p);
        }

        return matchingProds;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     */
    @Override
    public float logProbability(final String parent, final String leftChild, final String rightChild) {
        final int parentIndex = nonTermSet.getIndex(parent);
        final int leftChildIndex = nonTermSet.getIndex(leftChild);
        final int rightChildIndex = nonTermSet.getIndex(rightChild);

        for (final Production p : binaryProductions) {
            if (p.parent == parentIndex && p.leftChild == leftChildIndex && p.rightChild == rightChildIndex) {
                return p.prob;
            }
        }

        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     */
    @Override
    public float logProbability(final String parent, final String child) {
        final int parentIndex = nonTermSet.getIndex(parent);
        final int leftChildIndex = nonTermSet.getIndex(child);

        for (final Production p : unaryProductions) {
            if (p.parent == parentIndex && p.leftChild == leftChildIndex) {
                return p.prob;
            }
        }

        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     */
    @Override
    public float lexicalLogProbability(final String parent, final String child) {
        final int parentIndex = nonTermSet.getIndex(parent);
        if (!lexSet.hasSymbol(child)) {
            return Float.NEGATIVE_INFINITY;
        }

        for (final Production p : lexicalProds[lexSet.getIndex(child)]) {
            if (p.parent == parentIndex) {
                return p.prob;
            }
        }
        return Float.NEGATIVE_INFINITY;
    }

    public String getStats() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + binaryProductions.size() + '\n');
        sb.append("Unary rules: " + unaryProductions.size() + '\n');
        sb.append("Lexical rules: " + numLexProds + '\n');

        sb.append("Non Terminals: " + nonTermSet.size() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + posSet.size() + '\n');
        sb.append("Max POS index: " + maxPOSIndex + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');

        return sb.toString();
    }

    public final class Production {

        // if rightChild == -1, it's a unary prod, if -2, it's a lexical prod
        public final static int UNARY_PRODUCTION = -1;
        public final static int LEXICAL_PRODUCTION = -2;

        public final int parent, leftChild, rightChild;
        public final float prob;

        // Binary production
        public Production(final int parent, final int leftChild, final int rightChild, final float prob) {
            this.parent = parent;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.prob = prob;
        }

        // Binary production
        public Production(final String parent, final String leftChild, final String rightChild, final float prob) {
            this(nonTermSet.getIndex(parent), nonTermSet.getIndex(leftChild), nonTermSet.getIndex(rightChild), prob);
        }

        // Unary production
        public Production(final int parent, final int child, final float prob, final boolean isLex) {
            this.parent = parent;
            if (!isLex) {
                this.leftChild = child;
                this.rightChild = UNARY_PRODUCTION;
            } else {
                this.leftChild = child;
                posSet.addSymbol(this.parent);
                if (this.parent > maxPOSIndex) {
                    maxPOSIndex = this.parent;
                }
                this.rightChild = LEXICAL_PRODUCTION;
            }
            this.prob = prob;
        }

        public Production(final String parent, final String child, final float prob, final boolean isLex) {
            this(nonTermSet.getIndex(parent), isLex ? lexSet.getIndex(child) : nonTermSet.getIndex(child), prob, isLex);
        }

        public final Production copy() {
            return new Production(parent, leftChild, rightChild, prob);
        }

        public boolean equals(final Production otherProd) {
            if (parent != otherProd.parent)
                return false;
            if (leftChild != otherProd.leftChild)
                return false;
            if (rightChild != otherProd.rightChild)
                return false;

            return true;
        }

        public final boolean isUnaryProd() {
            return rightChild == UNARY_PRODUCTION;
        }

        public final boolean isLexProd() {
            return rightChild == LEXICAL_PRODUCTION;
        }

        public final String parentToString() {
            return nonTermSet.getSymbol(parent);
        }

        public String childrenToString() {
            if (isLexProd()) {
                return lexSet.getSymbol(leftChild);
            } else if (isUnaryProd()) {
                return nonTermSet.getSymbol(leftChild);
            } else {
                return nonTermSet.getSymbol(leftChild) + " " + nonTermSet.getSymbol(rightChild);
            }
        }

        @Override
        public String toString() {
            return parentToString() + " -> " + childrenToString() + " (p=" + Double.toString(prob) + ")";
        }
    }
}
