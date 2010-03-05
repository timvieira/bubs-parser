package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.parser.util.Log;

public class Grammar {
    protected Collection<Production> binaryProductions;
    protected Collection<Production> unaryProductions;
    protected Collection<Production> lexicalProductions;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public int nullSymbol = -1;
    public int startSymbol = -1;
    protected int maxPOSIndex = -1; // used when creating arrays to hold all POS entries
    private boolean isLeftFactored;

    public final SymbolSet<String> nonTermSet = new SymbolSet<String>();
    public final SymbolSet<String> lexSet = new SymbolSet<String>();

    private final Vector<NonTerminal> nonTermInfo = new Vector<NonTerminal>();
    protected HashSet<Integer> posSet = new HashSet<Integer>();

    public Tokenizer tokenizer;

    protected Grammar() {
        tokenizer = new Tokenizer(lexSet);
    }

    public Grammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        init(grammarFile, lexiconFile, grammarFormat);
        tokenizer = new Tokenizer(lexSet);
    }

    public Grammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    protected void init(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        readGrammarAndLexicon(grammarFile, lexiconFile, grammarFormat);
    }

    public void readGrammarAndLexicon(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        // the nullSymbol is used for start/end of sentence markers and dummy non-terminals
        nullSymbol = addNonTerm(nullSymbolStr);
        getNonterminal(nullSymbol).isPOS = true;
        nullProduction = new Production(nullSymbol, nullSymbol, nullSymbol, Float.NEGATIVE_INFINITY);

        // read lexical productions first so that POS tags will all be concentrated
        // at the beginning of the nonTermSet list thus decreasing the maximum index
        // for a POS tag and saving a good deal of space for array creation
        Log.info(1, "INFO: Reading lexical productions ...");
        readLexProds(lexiconFile);

        Log.info(1, "INFO: Reading grammar ...");
        readGrammar(grammarFile, grammarFormat);

        if (startSymbol == -1) {
            throw new IllegalArgumentException("No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        finalizeAllLabelSets();

        Log.info(1, "INFO: " + getStats());
    }

    private void readLexProds(final Reader lexFile) throws NumberFormatException, Exception {
        String line;
        String[] tokens;
        Production lexProd;

        lexicalProductions = new LinkedList<Production>();

        final BufferedReader br = new BufferedReader(lexFile);
        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                lexProd = new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), true);
                lexicalProductions.add(lexProd);
            } else {
                throw new IllegalArgumentException("Unexpected line in lexical file\n\t" + line);
            }
        }
    }

    private void readGrammar(final Reader gramFile, final GrammarFormatType grammarFormat) throws Exception {
        String line;
        String[] tokens;
        Production prod;

        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();

        if (grammarFormat == GrammarFormatType.Roark) {
            startSymbol = addNonTerm("TOP");
        }

        final BufferedReader br = new BufferedReader(gramFile);
        while ((line = br.readLine()) != null) {
            tokens = line.split("\\s");
            if (tokens.length == 1) {

                if (startSymbol != -1) {
                    throw new IllegalArgumentException(
                            "Grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: " + line);
                }

                startSymbol = addNonTerm(tokens[0]);
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

        // add the indices of factored non-terminals to their own set
        for (final String nt : nonTermSet) {
            final int ntIndex = nonTermSet.getIndex(nt);
            if (getNonterminal(ntIndex).isPOS()) {
                posSet.add(ntIndex);
            }

            if (isFactoredNonTerm(nt, grammarFormat)) {
                getNonterminal(ntIndex).isFactored = true;
            }
        }

        // figure out which way the grammar is factored
        int numLeftFactored = 0, numRightFactored = 0;
        for (final Production p : binaryProductions) {
            if (getNonterminal(p.leftChild).isFactored) {
                numLeftFactored++;
            }
            if (getNonterminal(p.rightChild).isFactored) {
                numRightFactored++;
            }
        }

        if (numLeftFactored > 0 && numRightFactored == 0) {
            isLeftFactored = true;
        } else if (numRightFactored > 0 && numLeftFactored == 0) {
            isLeftFactored = false;
        } else {
            Log.info(0, "ERROR: Factoring of grammar is inconsistent or unknown.  Binary rules have " + numLeftFactored + " left-factored children and " + numRightFactored
                    + " right-factored children.  Expecting one to be zero and the other nonzero.");
            System.exit(1);
        }
    }

    // public final Token[] tokenize(final String sentence) throws Exception {
    // return tokenizer.tokenize(sentence);
    // }

    public final int numNonTerms() {
        return nonTermSet.numSymbols();
    }

    public final int numLexSymbols() {
        return lexSet.size();
    }

    public final int numPosSymbols() {
        return posSet.size();
    }

    public int numUnaryProds() {
        return unaryProductions.size();
    }

    public int numLexProds() {
        return lexicalProductions.size();
    }

    public final String startSymbol() {
        return nonTermSet.getSymbol(startSymbol);
    }

    public final boolean hasWord(final String s) {
        return lexSet.hasSymbol(s);
    }

    private int addNonTerm(final String nonTerm) {
        if (nonTermSet.hasSymbol(nonTerm)) {
            return nonTermSet.getIndex(nonTerm);
        }
        return nonTermSet.addSymbol(nonTerm);
    }

    // TODO: I don't like that we have getNonterminal() and mapNonterminal()
    // methods. Should mapNonterminal return a NonTerminal instead of a string?
    public final NonTerminal getNonterminal(final int index) {
        if (nonTermInfo.size() > index && nonTermInfo.get(index) != null) {
            return nonTermInfo.get(index);
        }

        // bump up the size of nonTermInfo if necessary
        for (int i = nonTermInfo.size() - 1; i < index; i++) {
            nonTermInfo.add(null);
        }

        final NonTerminal newNonTerm = new NonTerminal(nonTermSet.getSymbol(index));
        nonTermInfo.set(index, newNonTerm);
        return nonTermInfo.get(index);
    }

    // public final int getNonTermIndex(final String token) throws Exception {
    // return nonTermSet.getIndex(token);
    // }

    public final String mapNonterminal(final int nonterminal) {
        return nonTermSet.getSymbol(nonterminal);
    }

    public final int mapNonterminal(final String nonterminal) {
        return nonTermSet.getIndex(nonterminal);
    }

    public final String mapLexicalEntry(final int lexicalEntry) {
        return lexSet.getSymbol(lexicalEntry);
    }

    // TODO: can probably get rid of this and just derive it where necessary
    public final int maxPOSIndex() {
        return maxPOSIndex;
    }

    // public boolean isPOS(final int nonTermIndex) {
    // return nonTermInfo.get(nonTermIndex).isPOS();
    // }
    //
    // public final boolean isLeftChild(final int nonTerm) {
    // return nonTermInfo.get(nonTerm).isLeftChild();
    // }
    //
    // public final boolean isRightChild(final int nonTerm) {
    // return nonTermInfo.get(nonTerm).isRightChild();
    // }
    //
    // public boolean isFactoredNonTerm(final int nonTermIndex) {
    // return nonTermInfo.get(nonTermIndex).isFactored();
    // }

    private boolean isFactoredNonTerm(final String nonTerm, final GrammarFormatType grammarType) {
        if (grammarType == GrammarFormatType.CSLU) {
            if (nonTerm.contains("|"))
                return true;
        } else {
            if (nonTerm.startsWith("@"))
                return true;
        }
        return false;
    }

    public boolean isLeftFactored() {
        return isLeftFactored;
    }

    public boolean isRightFactored() {
        return isLeftFactored == false;
    }

    public Collection<Production> getBinaryProductions() {
        return binaryProductions;
    }

    public Collection<Production> getBinaryProductionsWithLeftChild(final int leftChild) {
        // final Collection<Production> results = new LinkedList<Production>();
        // for (final Production p : binaryProductions) {
        // if (p.leftChild == leftChild) {
        // results.add(p);
        // }
        // }
        // return results;
        throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    }

    public Collection<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        // final Collection<Production> results = new LinkedList<Production>();
        // for (final Production p : binaryProductions) {
        // if (p.rightChild == rightChild) {
        // results.add(p);
        // }
        // }
        // return results;
        throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    }

    public Collection<Production> getBinaryProductionsWithChildren(final int leftChild, final int rightChild) {
        // final Collection<Production> results = new LinkedList<Production>();
        // for (final Production p : binaryProductions) {
        // if (p.leftChild == leftChild && p.rightChild == rightChild) {
        // results.add(p);
        // }
        // }
        // return results;
        throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    }

    public Collection<Production> getUnaryProductions() {
        return unaryProductions;
    }

    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    }

    public Collection<Production> getLexicalProductions() {
        return lexicalProductions;
    }

    public Collection<Production> getLexicalProductionsWithChild(final int child) {
        throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    }

    // public Collection<Production> getLexicalProductionsWithChild(final String childStr) {
    // throw new RuntimeException("ERROR: Unable to compute efficiently.  Please use a different grammar.");
    // }

    public Production getProduction(final String A, final String B, final String C) throws Exception {
        assert binaryProductions != null && binaryProductions.size() > 0;
        if (nonTermSet.hasSymbol(A) && nonTermSet.hasSymbol(B) && nonTermSet.hasSymbol(C)) {
            final int a = nonTermSet.getIndex(A), b = nonTermSet.getIndex(B), c = nonTermSet.getIndex(C);
            for (final Production p : binaryProductions) {
                if (p.parent == a && p.leftChild == b && p.rightChild == c) {
                    return p;
                }
            }
        }
        return null;
    }

    public Production getProduction(final String A, final String B) throws Exception {
        assert unaryProductions != null && unaryProductions.size() > 0;
        if (nonTermSet.hasSymbol(A) && nonTermSet.hasSymbol(B)) {
            final int a = nonTermSet.getIndex(A), b = nonTermSet.getIndex(B);
            for (final Production p : unaryProductions) {
                if (p.parent == a && p.child() == b) {
                    return p;
                }
            }
        }
        return null;
    }

    public Production getLexProduction(final String A, final String lex) throws Exception {
        assert lexicalProductions != null && lexicalProductions.size() > 0;
        if (nonTermSet.hasSymbol(A) && lexSet.hasSymbol(lex)) {
            final int a = nonTermSet.getIndex(A), b = lexSet.getIndex(lex);
            for (final Production p : lexicalProductions) {
                if (p.parent == a && p.child() == b) {
                    return p;
                }
            }
        }
        return null;

        // if (lexSet.hasSymbol(lex)) {
        // for (final Production p : getLexProdsByToken(lex)) {
        // if (p.parent == nonTermSet.getIndex(A)) {
        // return p;
        // }
        // }
        // }
        // return null;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     * 
     * @throws Exception
     */
    public float logProbability(final String parent, final String leftChild, final String rightChild) throws Exception {
        final Production p = getProduction(parent, leftChild, rightChild);
        if (p != null) {
            return p.prob;
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     * 
     * @throws Exception
     */
    public float logProbability(final String parent, final String child) throws Exception {
        final Production p = getProduction(parent, child);
        if (p != null) {
            return p.prob;
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Terribly inefficient; should be overridden by child classes
     * 
     * @throws Exception
     */
    public float lexicalLogProbability(final String parent, final String child) throws Exception {
        final Production p = getLexProduction(parent, child);
        if (p != null) {
            return p.prob;
        }
        return Float.NEGATIVE_INFINITY;
    }

    protected void finalizeAllLabelSets() {
        nonTermSet.finalize();
        lexSet.finalize();
    }

    protected void unfinalizeAllLabelSets() {
        nonTermSet.unfinalize();
        lexSet.unfinalize();
    }

    public String getStats() {
        String s = "";
        s += "numBinaryProds=" + binaryProductions.size();
        s += " numUnaryProds=" + unaryProductions.size();
        s += " numLexicalProds=" + lexicalProductions.size();
        s += " numNonTerms=" + nonTermSet.size();
        s += " numPosSymbols=" + numPosSymbols();
        // s += " numFactoredSymbols=" + factoredNonTermSet.size();
        s += " numLexSymbols=" + lexSet.size();
        s += " startSymbol=" + nonTermSet.getSymbol(startSymbol);
        s += " nullSymbol=" + nonTermSet.getSymbol(nullSymbol);
        s += " maxPosIndex=" + maxPOSIndex;
        s += " factorization=" + (isLeftFactored() ? "left" : "right");

        return s;
    }

    public final class Production {

        // if rightChild == -1, it's a unary prod, if -2, it's a lexical prod
        public final static int UNARY_PRODUCTION = -1;
        public final static int LEXICAL_PRODUCTION = -2;

        public final int parent, leftChild, rightChild;
        public final float prob;

        // Binary production
        public Production(final int parent, final int leftChild, final int rightChild, final float prob) {
            assert parent != -1 && leftChild != -1 && rightChild != -1;
            this.parent = parent;
            this.leftChild = leftChild;
            this.rightChild = rightChild;
            this.prob = prob;

            getNonterminal(leftChild).isLeftChild = true;
            getNonterminal(rightChild).isRightChild = true;
        }

        // Binary production
        public Production(final String parent, final String leftChild, final String rightChild, final float prob) {
            this(addNonTerm(parent), addNonTerm(leftChild), addNonTerm(rightChild), prob);
        }

        // Unary production
        public Production(final int parent, final int child, final float prob, final boolean isLex) {
            assert parent != -1 && child != -1;
            this.parent = parent;
            this.leftChild = child;
            if (!isLex) {
                this.rightChild = UNARY_PRODUCTION;
            } else {
                this.rightChild = LEXICAL_PRODUCTION;
                getNonterminal(parent).isPOS = true;
                if (parent > maxPOSIndex) {
                    maxPOSIndex = parent;
                }
            }
            this.prob = prob;
        }

        public Production(final String parent, final String child, final float prob, final boolean isLex) throws Exception {
            this(addNonTerm(parent), isLex ? lexSet.addSymbol(child) : addNonTerm(child), prob, isLex);
        }

        public final Production copy() throws Exception {
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

        public int child() {
            assert isUnaryProd() == true;
            return leftChild;
        }

        // TODO: a lexical prod IS A unary prod .. this should return true for both but i don't want to change
        // it now do to potential ramifications
        public final boolean isUnaryProd() {
            return rightChild == UNARY_PRODUCTION;
        }

        public final boolean isLexProd() {
            return rightChild == LEXICAL_PRODUCTION;
        }

        public boolean isBinaryProd() {
            return isUnaryProd() == false && isLexProd() == false;
        }

        public final String parentToString() {
            return nonTermSet.getSymbol(parent);
        }

        public String childrenToString() {
            if (isLexProd()) {
                return lexSet.getSymbol(leftChild);
            } else if (isUnaryProd()) {
                return nonTermSet.getSymbol(leftChild);
            }
            return nonTermSet.getSymbol(leftChild) + " " + nonTermSet.getSymbol(rightChild);
        }

        @Override
        public String toString() {
            return parentToString() + " -> " + childrenToString() + " (p=" + Double.toString(prob) + ")";
        }

    }
}
