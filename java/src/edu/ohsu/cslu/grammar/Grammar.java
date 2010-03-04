package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import edu.ohsu.cslu.grammar.Tokenizer.Token;
import edu.ohsu.cslu.parser.ParserDriver.GrammarFormatType;
import edu.ohsu.cslu.parser.util.Log;

public class Grammar {
    protected Collection<Production> binaryProductions;
    protected Collection<Production> unaryProductions;
    private List<Production>[] unaryProdsByChild;
    protected List<Production>[] lexicalProds;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public int nullSymbol = -1;
    public int startSymbol = -1;
    public int numLexProds;
    protected int maxPOSIndex = -1; // used when creating arrays to hold all POS entries
    private boolean isLeftFactored;

    public final SymbolSet<String> nonTermSet = new SymbolSet<String>();
    public final SymbolSet<String> lexSet = new SymbolSet<String>();
    private final Vector<NonTerminal> nonTermInfo = new Vector<NonTerminal>();
    protected HashSet<Integer> posSet = new HashSet<Integer>();

    protected Tokenizer tokenizer;

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
        // nullSymbol = nonTermSet.addSymbol(nullSymbolStr);
        nullSymbol = addNonTerm(nullSymbolStr);
        nonTermInfo.get(nullSymbol).isPOS = true;
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

    @SuppressWarnings("unchecked")
    private void readLexProds(final Reader lexFile) throws NumberFormatException, Exception {
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

    @SuppressWarnings( { "cast", "unchecked" })
    private void readGrammar(final Reader gramFile, final GrammarFormatType grammarFormat) throws Exception {
        final BufferedReader br = new BufferedReader(gramFile);

        String line;
        String[] tokens;
        Production prod;

        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();

        if (grammarFormat == GrammarFormatType.Roark) {
            startSymbol = addNonTerm("TOP");
        }

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
            if (isPOS(ntIndex)) {
                posSet.add(ntIndex);
            }

            if (isFactoredNonTerm(nt, grammarFormat)) {
                nonTermInfo.get(ntIndex).isFactored = true;
            }
        }

        // figure out which way the grammar is factored
        int numLeftFactored = 0, numRightFactored = 0;
        for (final Production p : binaryProductions) {
            if (nonTermInfo.get(p.leftChild).isFactored) {
                numLeftFactored++;
            }
            if (nonTermInfo.get(p.rightChild).isFactored) {
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

        // store unary prods by child
        unaryProdsByChild = (List<Production>[]) new List[nonTermSet.size()];
        for (int i = 0; i < nonTermSet.size(); i++) {
            unaryProdsByChild[i] = null;
        }

        for (final Production p : unaryProductions) {
            if (unaryProdsByChild[p.leftChild] == null) {
                unaryProdsByChild[p.leftChild] = new LinkedList<Production>();
            }
            unaryProdsByChild[p.leftChild].add(p);
        }
    }

    public final Token[] tokenize(final String sentence) throws Exception {
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

    public final int getNonTermIndex(final String token) throws Exception {
        return nonTermSet.getIndex(token);
    }

    public final int maxPOSIndex() {
        return maxPOSIndex;
    }

    public boolean isPOS(final int nonTermIndex) {
        return nonTermInfo.get(nonTermIndex).isPOS();
    }

    public final boolean isLeftChild(final int nonTerm) {
        return nonTermInfo.get(nonTerm).isLeftChild();
    }

    public final boolean isRightChild(final int nonTerm) {
        return nonTermInfo.get(nonTerm).isRightChild();
    }

    public boolean isFactoredNonTerm(final int nonTermIndex) {
        return nonTermInfo.get(nonTermIndex).isFactored();
    }

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

    public final String mapNonterminal(final int nonterminal) {
        return nonTermSet.getSymbol(nonterminal);
    }

    public final int mapNonterminal(final String nonterminal) {
        return nonTermSet.getIndex(nonterminal);
    }

    public final String mapLexicalEntry(final int lexicalEntry) {
        return lexSet.getSymbol(lexicalEntry);
    }

    public Collection<Production> getBinaryProductions() {
        return binaryProductions;
    }

    public Collection<Production> getBinaryProductionsWithLeftChild(final int leftChild) {
        final Collection<Production> results = new LinkedList<Production>();
        for (final Production p : binaryProductions) {
            if (p.leftChild == leftChild) {
                results.add(p);
            }
        }
        return results;
    }

    public Collection<Production> getBinaryProductionsWithRightChild(final int rightChild) {
        final Collection<Production> results = new LinkedList<Production>();
        for (final Production p : binaryProductions) {
            if (p.rightChild == rightChild) {
                results.add(p);
            }
        }
        return results;
    }

    public Collection<Production> getBinaryProductionsWithChildren(final int leftChild, final int rightChild) {
        final Collection<Production> results = new LinkedList<Production>();
        for (final Production p : binaryProductions) {
            if (p.leftChild == leftChild && p.rightChild == rightChild) {
                results.add(p);
            }
        }
        return results;
    }

    public Collection<Production> getUnaryProductions() {
        return unaryProductions;
    }

    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        final List<Production> prods = unaryProdsByChild[child];
        if (prods == null) {
            return new LinkedList<Production>();
        }
        return prods;
    }

    public final List<Production> getLexProdsByToken(final Token token) {
        return lexicalProds[token.index];
    }

    public final List<Production> getLexProdsByToken(final String lex) throws Exception {
        if (lexSet.hasSymbol(lex) == false) {
            return null;
        }
        return lexicalProds[lexSet.getIndex(lex)];
    }

    public Production getProduction(final String A, final String B, final String C) throws Exception {
        // final Production toFind = new Production(A, B, C, Float.NEGATIVE_INFINITY);
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
                if (p.parent == a && p.leftChild == b) {
                    return p;
                }
            }
        }
        return null;
    }

    public Production getLexProduction(final String A, final String lex) throws Exception {
        if (lexSet.hasSymbol(lex)) {
            for (final Production p : getLexProdsByToken(lex)) {
                if (p.parent == nonTermSet.getIndex(A)) {
                    return p;
                }
            }
        }
        return null;
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
        s += " numLexicalProds=" + numLexProds;
        s += " numNonTerms=" + nonTermSet.size();
        // s += " numPosSymbols=" + posSet.size();
        // s += " numFactoredSymbols=" + factoredNonTermSet.size();
        s += " numLexSymbols=" + lexSet.size();
        s += " startSymbol=" + nonTermSet.getSymbol(startSymbol);
        s += " nullSymbol=" + nonTermSet.getSymbol(nullSymbol);
        s += " maxPosIndex=" + maxPOSIndex;
        s += " factorization=" + (isLeftFactored() ? "left" : "right");

        return s;
    }

    private int addNonTerm(final String nonTerm) {
        if (nonTermSet.hasSymbol(nonTerm)) {
            return nonTermSet.getIndex(nonTerm);
        }

        // set nonTermInfo[index] = NonTerminal. May possibly need to increase the size of the vector
        final int index = nonTermSet.addSymbol(nonTerm);
        for (int i = nonTermInfo.size() - 1; i < index; i++) {
            nonTermInfo.add(null);
        }
        nonTermInfo.set(index, new NonTerminal(nonTerm));

        return index;
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

            nonTermInfo.get(leftChild).isLeftChild = true;
            nonTermInfo.get(rightChild).isRightChild = true;
        }

        // Binary production
        public Production(final String parent, final String leftChild, final String rightChild, final float prob) throws Exception {
            this(addNonTerm(parent), addNonTerm(leftChild), addNonTerm(rightChild), prob);
        }

        // Unary production
        public Production(final int parent, final int child, final float prob, final boolean isLex) throws Exception {
            this.parent = parent;
            this.leftChild = child;
            if (!isLex) {
                this.rightChild = UNARY_PRODUCTION;
            } else {
                this.rightChild = LEXICAL_PRODUCTION;
                nonTermInfo.get(parent).isPOS = true;
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
