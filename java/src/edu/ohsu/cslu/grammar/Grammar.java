package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.Reader;
import java.io.Serializable;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Vector;

import edu.ohsu.cslu.parser.util.Log;

/**
 * Represents a Probabilistic Context Free Grammar (PCFG). Such grammars may be built up programatically or may be
 * inferred from a corpus of data.
 * 
 * A PCFG consists of
 * <ul>
 * <li>A set of non-terminal symbols V (alternatively referred to as 'categories')</li>
 * <li>A special start symbol (S-dagger) from V</li>
 * <li>A set of terminal symbols T</li>
 * <li>A set of rule productions P mapping from V to (V union T)</li>
 * </ul>
 * 
 * Such grammars are useful in modeling and analyzing the structure of natural language or the (secondary) structure of
 * many biological sequences.
 * 
 * Although branching of arbitrary size is possible in a grammar under this definition, this class models grammars which
 * have been factored into binary-branching form, enabling much more efficient computational approaches to be used.
 * 
 * Note - there will always be more productions than categories, but all categories except the special S-dagger category
 * are also productions. The index of a category will be the same when used as a production as it is when used as a
 * category.
 * 
 * @author Nathan Bodenstab
 * @author Aaron Dunlop
 * @since 2010
 * 
 *        $Id$
 */
public class Grammar implements Serializable {

    /** Marks the switch from PCFG to lexicon entries in the grammar file */
    public final static String DELIMITER = "===== LEXICON =====";

    protected Collection<Production> binaryProductions;
    protected Collection<Production> unaryProductions;
    protected Collection<Production> lexicalProductions;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public int nullSymbol = -1;
    public int startSymbol = -1;
    protected int maxPOSIndex = -1; // used when creating arrays to hold all POS entries
    public int numPosSymbols;
    public GrammarFormatType grammarFormat;

    // Default to left-factored
    private boolean isLeftFactored = true;

    protected SymbolSet<String> nonTermSet = new SymbolSet<String>();
    protected SymbolSet<String> lexSet = new SymbolSet<String>();
    protected Vector<NonTerminal> nonTermInfo = new Vector<NonTerminal>();

    public Tokenizer tokenizer;

    protected Grammar() {
        tokenizer = new Tokenizer(lexSet);
    }

    public Grammar(final Reader grammarFile) throws Exception {
        this.grammarFormat = init(grammarFile);
        tokenizer = new Tokenizer(lexSet);
    }

    public Grammar(final String grammarFile) throws Exception {
        this(new FileReader(grammarFile));
    }

    /**
     * Read in and initialize the grammar. This implementation simply reads in the grammar file. Subclasses may override
     * this method to do additional initialization.
     * 
     * @param grammarFile
     * @return Detected grammar format
     * @throws Exception
     */
    protected GrammarFormatType init(final Reader grammarFile) throws Exception {
        return readGrammarAndLexicon(grammarFile);
    }

    public GrammarFormatType readGrammarAndLexicon(final Reader grammarFile) throws Exception {

        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();
        lexicalProductions = new LinkedList<Production>();

        // the nullSymbol is used for start/end of sentence markers and dummy non-terminals
        nullSymbol = addNonTerm(nullSymbolStr);
        getNonterminal(nullSymbol).isPOS = true;
        nullProduction = new Production(nullSymbol, nullSymbol, nullSymbol, Float.NEGATIVE_INFINITY);

        Log.info(1, "INFO: Reading grammar ...");
        final GrammarFormatType gf = readGrammar(grammarFile);

        if (startSymbol == -1) {
            throw new IllegalArgumentException(
                    "No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        nonTermSet.finalize();
        lexSet.finalize();

        Log.info(1, "INFO: " + getStats());
        return gf;
    }

    private GrammarFormatType readGrammar(final Reader gramFile) throws Exception {

        GrammarFormatType gf = null;

        final BufferedReader br = new BufferedReader(gramFile);
        br.mark(50);

        // Read the first line and try to induce the grammar format from it
        final String sDagger = br.readLine();

        if (sDagger.matches("[A-Z]+_[0-9]+")) {
            gf = GrammarFormatType.Berkeley;
            startSymbol = addNonTerm(sDagger);
        } else if (sDagger.split(" ").length > 1) {
            gf = GrammarFormatType.Roark;
            // The first line was not a start symbol; reset the reader and re-process that line
            br.reset();
            startSymbol = addNonTerm("TOP");
        } else {
            gf = GrammarFormatType.CSLU;
            startSymbol = addNonTerm(sDagger);
        }

        for (String line = br.readLine(); line != null && !line.equals(DELIMITER); line = br.readLine()) {
            final String[] tokens = line.split("\\s");

            if (tokens.length == 1) {
                throw new IllegalArgumentException(
                        "Grammar file must contain a single line with a single string representing the START SYMBOL.\n"
                                + "More than one entry was found.  Last line: " + line);
            } else if (tokens.length == 4) {
                // Unary production: expecting: A -> B prob
                // should we make sure there aren't any duplicates?
                unaryProductions.add(new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), false));
            } else if (tokens.length == 5) {
                // Binary production: expecting: A -> B C prob
                binaryProductions.add(new Production(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4])));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar PCFG\n\t" + line);
            }
        }

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                lexicalProductions.add(new Production(tokens[0], tokens[2], Float.valueOf(tokens[3]), true));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
            }
        }

        // add the indices of factored non-terminals to their own set
        int posCount = 0;
        for (final String nt : nonTermSet) {
            final int ntIndex = nonTermSet.getIndex(nt);
            if (getNonterminal(ntIndex).isPOS()) {
                posCount++;
            }

            if (isFactoredNonTerm(nt, grammarFormat)) {
                getNonterminal(ntIndex).isFactored = true;
            }
        }

        numPosSymbols = posCount;

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

        if (numRightFactored > 0 && numLeftFactored == 0) {
            isLeftFactored = false;
        }

        return gf;
    }

    /**
     * @return The number of nonterminals modeled in this grammar (|V|)
     */
    public final int numNonTerms() {
        return nonTermSet.numSymbols();
    }

    /**
     * @return The number terminals modeled in this grammar (|T|)
     */
    public final int numLexSymbols() {
        return lexSet.size();
    }

    public final int numPosSymbols() {
        return numPosSymbols;
    }

    public int numBinaryProds() {
        return binaryProductions.size();
    }

    public int numUnaryProds() {
        return unaryProductions.size();
    }

    public int numLexProds() {
        return lexicalProductions.size();
    }

    /**
     * @return The special start symbol (S-dagger).
     */
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

    /**
     * @return true if this grammar is left-factored
     */
    public boolean isLeftFactored() {
        return isLeftFactored;
    }

    /**
     * @return true if this grammar is right-factored
     */
    public boolean isRightFactored() {
        return !isLeftFactored;
    }

    /*
     * Binary Productions
     */
    public Collection<Production> getBinaryProductions() {
        return binaryProductions;
    }

    public Production getBinaryProduction(final int parent, final int leftChild, final int rightChild) {
        for (final Production p : binaryProductions) {
            if (p.parent == parent && p.leftChild == leftChild && p.rightChild == rightChild) {
                return p;
            }
        }
        return null;
    }

    // TODO: do we really need a String interface for getBinaryProduction *and* binaryLogProb?
    // It's only reference is from CellChart#addParseTreeToChart(ParseTree)
    public Production getBinaryProduction(final String A, final String B, final String C) {
        if (nonTermSet.hasSymbol(A) && nonTermSet.hasSymbol(B) && nonTermSet.hasSymbol(C)) {
            return getBinaryProduction(nonTermSet.getIndex(A), nonTermSet.getIndex(B), nonTermSet.getIndex(C));
        }
        return null;
    }

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    public float binaryLogProbability(final int parent, final int leftChild, final int rightChild) {
        return getProductionProb(getBinaryProduction(parent, leftChild, rightChild));
    }

    /**
     * Returns the log probability of a binary rule.
     * 
     * @param parent
     * @param leftChild
     * @param rightChild
     * @return Log probability of the specified rule.
     */
    public float binaryLogProbability(final String parent, final String leftChild, final String rightChild) {
        if (nonTermSet.hasSymbol(parent) && nonTermSet.hasSymbol(leftChild) && nonTermSet.hasSymbol(rightChild)) {
            return binaryLogProbability(nonTermSet.getIndex(parent), nonTermSet.getIndex(leftChild),
                    nonTermSet.getIndex(rightChild));
        }
        return Float.NEGATIVE_INFINITY;
    }

    /*
     * Unary Productions
     */
    public Collection<Production> getUnaryProductions() {
        return unaryProductions;
    }

    public Production getUnaryProduction(final int parent, final int child) {
        for (final Production p : unaryProductions) {
            if (p.parent == parent && p.child() == child) {
                return p;
            }
        }
        return null;
    }

    public Production getUnaryProduction(final String A, final String B) {
        if (nonTermSet.hasSymbol(A) && nonTermSet.hasSymbol(B)) {
            return getUnaryProduction(nonTermSet.getIndex(A), nonTermSet.getIndex(B));
        }
        return null;
    }

    /**
     * Returns the log probability of a unary rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public float unaryLogProbability(final int parent, final int child) {
        return getProductionProb(getUnaryProduction(parent, child));
    }

    /**
     * Returns the log probability of a unary rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public float unaryLogProbability(final String parent, final String child) {
        if (nonTermSet.hasSymbol(parent) && nonTermSet.hasSymbol(child)) {
            return unaryLogProbability(nonTermSet.getIndex(parent), nonTermSet.getIndex(child));
        }
        return Float.NEGATIVE_INFINITY;
    }

    /*
     * Lexical Productions
     */
    public Collection<Production> getLexicalProductions() {
        return lexicalProductions;
    }

    public Production getLexicalProduction(final int parent, final int lex) {
        for (final Production p : lexicalProductions) {
            if (p.parent == parent && p.child() == lex) {
                return p;
            }
        }
        return null;
    }

    public Production getLexicalProduction(final String A, final String lex) {
        if (nonTermSet.hasSymbol(A) && lexSet.hasSymbol(lex)) {
            return getLexicalProduction(nonTermSet.getIndex(A), lexSet.getIndex(lex));
        }
        return null;
    }

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public float lexicalLogProbability(final int parent, final int child) {
        return getProductionProb(getLexicalProduction(parent, child));
    }

    /**
     * Returns the log probability of a lexical rule.
     * 
     * @param parent
     * @param child
     * @return Log probability of the specified rule.
     */
    public float lexicalLogProbability(final String parent, final String child) {
        if (nonTermSet.hasSymbol(parent) && lexSet.hasSymbol(child)) {
            return lexicalLogProbability(nonTermSet.getIndex(parent), lexSet.getIndex(child));
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns the log probability of a rule.
     * 
     * @param p Production
     * @return Log probability of the specified rule.
     */
    protected float getProductionProb(final Production p) {
        if (p != null) {
            return p.prob;
        }
        return Float.NEGATIVE_INFINITY;
    }

    /**
     * Returns a string representation of all child pairs recognized by this grammar.
     * 
     * @return a string representation of all child pairs recognized by this grammar.
     */
    public String recognitionMatrix() {

        final HashSet<String> validChildPairs = new HashSet<String>(binaryProductions.size() / 2);
        for (final Production p : binaryProductions) {
            validChildPairs.add(p.leftChild + "," + p.rightChild);
        }

        final StringBuilder sb = new StringBuilder(10 * 1024);
        for (final String childPair : validChildPairs) {
            sb.append(childPair + '\n');
        }

        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public String getStats() {
        String s = "";
        s += "numBinaryProds=" + numBinaryProds();
        s += " numUnaryProds=" + numUnaryProds();
        s += " numLexicalProds=" + numLexProds();
        s += " numNonTerms=" + numNonTerms();
        s += " numPosSymbols=" + numPosSymbols();
        // s += " numFactoredSymbols=" + factoredNonTermSet.size();
        s += " numLexSymbols=" + lexSet.size();
        s += " startSymbol=" + nonTermSet.getSymbol(startSymbol);
        s += " nullSymbol=" + nonTermSet.getSymbol(nullSymbol);
        s += " maxPosIndex=" + maxPOSIndex;
        s += " factorization=" + (isLeftFactored() ? "left" : "right");

        return s;
    }

    public final class Production implements Serializable {

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

        public Production(final String parent, final String child, final float prob, final boolean isLex) {
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

    static public enum GrammarFormatType {
        CSLU, Roark, Berkeley;

        public String unsplitNonTerminal(final String nonTerminal) {
            switch (this) {
            case Berkeley:
                return nonTerminal.replaceFirst("_[0-9]+$", "");
            case CSLU:
                return nonTerminal.replaceFirst("[|^]<([A-Z]+)?>$", "");
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");

            }
        }

        public String factoredNonTerminal(final String nonTerminal) {
            switch (this) {
            case Berkeley:
                return "@" + nonTerminal;
            case CSLU:
                return nonTerminal + "|";
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");
            }

        }

        public boolean isFactored(final String nonTerminal) {
            switch (this) {
            case CSLU:
                return nonTerminal.contains("|");
            case Berkeley:
                return nonTerminal.startsWith("@");
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");
            }
        }
    }
}
