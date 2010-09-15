package edu.ohsu.cslu.grammar;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

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

    private final static long serialVersionUID = 1L;

    /** Marks the switch from PCFG to lexicon entries in the grammar file */
    public final static String DELIMITER = "===== LEXICON =====";

    /** String representation of the start symbol (s-dagger) */
    public String startSymbolStr;

    /** The first NT valid as a left child. */
    public final int leftChildrenStart;

    /** The last non-POS NT valid as a left child. */
    public final int leftChildrenEnd;

    /** The first non-POS NT valid as a right child. */
    public final int rightChildrenStart;

    /** The last non-POS NT valid as a right child. */
    public final int rightChildrenEnd;

    /** The first POS. */
    public final int posStart;

    /** The last POS. */
    public final int posEnd;

    /** The last NT valid as a parent */
    public final int parentEnd;

    /** The number of binary productions modeled in this Grammar */
    protected final int numBinaryProds;

    /** The number of unary productions modeled in this Grammar */
    protected final int numUnaryProds;

    /** The number of lexical productions modeled in this Grammar */
    protected final int numLexProds;

    protected final ArrayList<Production> binaryProductions;
    protected final ArrayList<Production> unaryProductions;
    protected final ArrayList<Production> lexicalProductions;

    protected final Collection<Production>[] unaryProductionsByChild;
    protected final Collection<Production>[] lexicalProdsByChild;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public int nullSymbol = -1;
    public int startSymbol = -1;
    protected int maxPOSIndex = -1; // used when creating arrays to hold all POS entries
    public int numPosSymbols;
    public GrammarFormatType grammarFormat;

    // Default to left-factored
    private boolean isLeftFactored = true;

    public final SymbolSet<String> nonTermSet;
    public final SymbolSet<String> lexSet;
    private ArrayList<NonTerminal> nonTermInfo = new ArrayList<NonTerminal>();

    public final Tokenizer tokenizer;

    /**
     * A temporary String -> String map, used to conserve memory while reading and sorting the grammar. Similar to
     * {@link String}'s own intern map, but we don't need to internalize Strings indefinitely, so we map them ourselves
     * and allow the map to be GC'd after we're done constructing the grammar.
     */
    private HashMap<String, String> internMap = new HashMap<String, String>();

    /**
     * Signature of the first 2 bytes of a binary Java Serialized Object. Allows us to use the same command-line option
     * for serialized and text grammars and auto-detect the format
     */
    private final static short OBJECT_SIGNATURE = (short) 0xACED;

    /**
     * Default Constructor. This constructor does an inordinate amount of work directly in the constructor specifically
     * so we can initialize final instance variables. Making the instance variables final allows the JIT to inline them
     * everywhere we use them, improving runtime efficiency considerably.
     * 
     * Reads the grammar into memory and sorts non-terminals (V) according to their occurrence in binary rules. This can
     * allow more efficient iteration in grammar intersection (e.g., skipping NTs only valid as left children in the
     * right cell) and more efficient chart storage (e.g., omitting storage for POS NTs in chart rows >= 2).
     */
    public Grammar(final Reader grammarFile) throws IOException {

        nonTermSet = new SymbolSet<String>();
        lexSet = new SymbolSet<String>();

        final List<StringProduction> pcfgRules = new LinkedList<StringProduction>();
        final List<StringProduction> lexicalRules = new LinkedList<StringProduction>();

        this.grammarFormat = readPcfgAndLexicon(grammarFile, pcfgRules, lexicalRules);

        final HashSet<String> nonTerminals = new HashSet<String>();
        final HashSet<String> pos = new HashSet<String>();

        // Process the lexical productions first. Label any non-terminals found in the lexicon as POS tags. We
        // assume that pre-terminals (POS) will only occur as parents in span-1 rows and as children in span-2
        // rows
        for (final StringProduction lexicalRule : lexicalRules) {
            nonTerminals.add(lexicalRule.parent);
            pos.add(lexicalRule.parent);
        }

        // All non-terminals
        final HashSet<String> nonPosSet = new HashSet<String>();
        final HashSet<String> rightChildrenSet = new HashSet<String>();
        final HashSet<String> leftChildrenSet = new HashSet<String>();

        // Iterate through grammar rules, populating temporary non-terminal sets
        for (final StringProduction grammarRule : pcfgRules) {

            nonTerminals.add(grammarRule.parent);
            nonTerminals.add(grammarRule.leftChild);
            nonPosSet.add(grammarRule.leftChild);

            if (grammarRule instanceof BinaryStringProduction) {
                final BinaryStringProduction bsr = (BinaryStringProduction) grammarRule;

                nonTerminals.add(bsr.rightChild);

                nonPosSet.add(bsr.rightChild);
                leftChildrenSet.add(bsr.leftChild);
                rightChildrenSet.add(bsr.rightChild);
            }
        }

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and
        // dummy non-terminals). Label them as POS. I'm not sure that's right, but it seems to work.
        nonTerminals.add(startSymbolStr);
        pos.add(startSymbolStr);
        nonTerminals.add(nullSymbolStr);
        pos.add(nullSymbolStr);

        // Make the POS set disjoint from the other sets.
        rightChildrenSet.removeAll(pos);
        leftChildrenSet.removeAll(pos);
        nonPosSet.removeAll(pos);

        // Add the NTs to `nonTermSet' in sorted order
        final StringNonTerminalComparator comparator = new PosEmbeddedComparator();
        final TreeSet<StringNonTerminal> sortedNonTerminals = new TreeSet<StringNonTerminal>(comparator);
        for (final String nt : nonTerminals) {
            sortedNonTerminals.add(create(nt, pos, nonPosSet, rightChildrenSet));
        }

        for (final StringNonTerminal nt : sortedNonTerminals) {
            nonTermSet.addSymbol(nt.label);
        }

        // TODO Generalize these further for right-factored grammars

        // Initialize indices
        final int[] startAndEndIndices = comparator.startAndEndIndices(nonPosSet, leftChildrenSet, rightChildrenSet,
                pos);
        leftChildrenStart = startAndEndIndices[0];
        leftChildrenEnd = startAndEndIndices[1];

        rightChildrenStart = startAndEndIndices[2];
        rightChildrenEnd = startAndEndIndices[3];

        posStart = startAndEndIndices[4];
        posEnd = startAndEndIndices[5];

        parentEnd = startAndEndIndices[6];

        numPosSymbols = posEnd - posStart + 1;
        maxPOSIndex = posEnd;

        startSymbol = nonTermSet.addSymbol(startSymbolStr);
        nullSymbol = nonTermSet.addSymbol(nullSymbolStr);

        // Now that all NTs are mapped, we can create Production instances for lexical rules (we don't care
        // about sort order here)
        lexicalProductions = new ArrayList<Production>();

        for (final StringProduction lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild);
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex,
                    lexicalRule.probability, true));
        }
        numLexProds = lexicalProductions.size();

        // And unary and binary rules
        binaryProductions = new ArrayList<Production>();
        unaryProductions = new ArrayList<Production>();

        for (final StringProduction grammarRule : pcfgRules) {
            if (grammarRule instanceof BinaryStringProduction) {
                binaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild,
                        ((BinaryStringProduction) grammarRule).rightChild, grammarRule.probability));
            } else {
                unaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild, grammarRule.probability,
                        false));
            }
        }

        numBinaryProds = binaryProductions.size();
        numUnaryProds = unaryProductions.size();

        unaryProductionsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);

        internMap = null; // We no longer need the String intern map, so let it be GC'd

        this.tokenizer = new Tokenizer(lexSet);
    }

    /**
     * Construct a {@link Grammar} instance from an existing instance. This is used when constructing a subclass of
     * {@link Grammar} from a binary-serialized {@link Grammar}.
     * 
     * @param g
     */
    protected Grammar(final Grammar g) {
        this.startSymbolStr = g.startSymbolStr;
        this.leftChildrenStart = g.leftChildrenStart;
        this.leftChildrenEnd = g.leftChildrenEnd;
        this.rightChildrenStart = g.rightChildrenStart;
        this.rightChildrenEnd = g.rightChildrenEnd;
        this.posStart = g.posStart;
        this.posEnd = g.posEnd;
        this.parentEnd = g.parentEnd;
        this.numBinaryProds = g.numBinaryProds;
        this.numUnaryProds = g.numUnaryProds;
        this.numLexProds = g.numLexProds;

        this.binaryProductions = g.binaryProductions;
        this.unaryProductions = g.unaryProductions;
        this.lexicalProductions = g.lexicalProductions;

        this.unaryProductionsByChild = g.unaryProductionsByChild;
        this.lexicalProdsByChild = g.lexicalProdsByChild;

        this.nullSymbol = g.nullSymbol;
        this.startSymbol = g.startSymbol;
        this.maxPOSIndex = g.maxPOSIndex;
        this.numPosSymbols = g.numPosSymbols;
        this.grammarFormat = g.grammarFormat;

        this.isLeftFactored = g.isLeftFactored = true;

        this.nonTermSet = g.nonTermSet;
        this.lexSet = g.lexSet;
        this.nonTermInfo = g.nonTermInfo;

        this.tokenizer = g.tokenizer;

    }

    public Grammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
    }

    private GrammarFormatType readPcfgAndLexicon(final Reader grammarFile, final List<StringProduction> pcfgRules,
            final List<StringProduction> lexicalRules) throws IOException {
        // Read in the grammar file.
        Log.info(1, "INFO: Reading grammar");

        final BufferedReader br = new BufferedReader(grammarFile);
        br.mark(50);

        GrammarFormatType gf;

        // Read the first line and try to induce the grammar format from it
        final String sDagger = br.readLine();

        if (sDagger.matches("[A-Z]+_[0-9]+")) {
            gf = GrammarFormatType.Berkeley;
            startSymbolStr = sDagger;
        } else if (sDagger.split(" ").length > 1) {
            // The first line was not a start symbol.
            // Roark-format assumes 'TOP'. Reset the reader and re-process that line
            gf = GrammarFormatType.Roark;
            startSymbolStr = "TOP";
            br.reset();
        } else {
            gf = GrammarFormatType.CSLU;
            startSymbolStr = sDagger;
        }

        for (String line = br.readLine(); line != null && !line.equals(DELIMITER); line = br.readLine()) {
            final String[] tokens = line.split("\\s");

            if (tokens.length == 1) {
                throw new IllegalArgumentException(
                        "Grammar file must contain a single line with a single string representing the START SYMBOL.\n"
                                + "More than one entry was found.  Last line: " + line);
            } else if (tokens.length == 4) {
                // Unary production: expecting: A -> B prob
                // Should we make sure there aren't any duplicates?
                pcfgRules.add(new StringProduction(tokens[0], tokens[2], Float.valueOf(tokens[3])));
            } else if (tokens.length == 5) {
                // Binary production: expecting: A -> B C prob
                pcfgRules.add(new BinaryStringProduction(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4])));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar PCFG\n\t" + line);
            }
        }

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                lexicalRules.add(new StringProduction(tokens[0], tokens[2], Float.valueOf(tokens[3])));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
            }
        }
        return gf;
    }

    public static Grammar read(final InputStream inputStream) throws IOException, ClassNotFoundException {
        // Read the grammar in either text or binary-serialized format.
        final BufferedInputStream bis = new BufferedInputStream(inputStream);
        bis.mark(2);
        final DataInputStream dis = new DataInputStream(bis);

        // Look at the first 2 bytes of the file for the signature of a serialized java object
        final int signature = dis.readShort();
        bis.reset();

        if (signature == OBJECT_SIGNATURE) {
            final ObjectInputStream ois = new ObjectInputStream(bis);
            return (Grammar) ois.readObject();
        }

        return new Grammar(new InputStreamReader(bis));
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
        return numBinaryProds;
    }

    public int numUnaryProds() {
        return numUnaryProds;
    }

    public int numLexProds() {
        return numLexProds;
    }

    /**
     * @return The special start symbol (S-dagger).
     */
    public final String startSymbol() {
        return nonTermSet.getSymbol(startSymbol);
    }

    @SuppressWarnings({ "cast", "unchecked" })
    public static Collection<Production>[] storeProductionByChild(final Collection<Production> prods, final int maxIndex) {
        final Collection<Production>[] prodsByChild = (LinkedList<Production>[]) new LinkedList[maxIndex + 1];

        for (final Production p : prods) {
            final int child = p.child();
            if (prodsByChild[child] == null) {
                prodsByChild[child] = new LinkedList<Production>();
            }
            prodsByChild[child].add(p);
        }

        return prodsByChild;
    }

    public static Collection<Production>[] storeProductionByChild(final Collection<Production> prods) {
        int maxChildIndex = -1;
        for (final Production p : prods) {
            if (p.child() > maxChildIndex) {
                maxChildIndex = p.child();
            }
        }
        return storeProductionByChild(prods, maxChildIndex);
    }

    public Collection<Production> getUnaryProductionsWithChild(final int child) {
        if (child > unaryProductionsByChild.length - 1 || unaryProductionsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return unaryProductionsByChild[child];
    }

    public final Collection<Production> getLexicalProductionsWithChild(final int child) {
        if (child > lexicalProdsByChild.length - 1 || lexicalProdsByChild[child] == null) {
            return new LinkedList<Production>();
        }
        return lexicalProdsByChild[child];
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

    public final boolean isPos(final int nonTerminal) {
        return nonTerminal >= posStart && nonTerminal <= posEnd;
    }

    /**
     * Returns true if the non-terminal occurs as a right child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a right child in the grammar.
     */
    public boolean isValidRightChild(final int nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd;
    }

    /**
     * Returns true if the non-terminal occurs as a left child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a left child in the grammar.
     */
    public boolean isValidLeftChild(final int nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd;
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
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + numBinaryProds() + '\n');
        sb.append("Unary rules: " + numUnaryProds() + '\n');
        sb.append("Lexical rules: " + numLexProds() + '\n');

        sb.append("Non Terminals: " + numNonTerms() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + numPosSymbols() + '\n');
        sb.append("Max POS index: " + maxPOSIndex + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');
        sb.append("Factorization: " + (isLeftFactored() ? "left" : "right") + '\n');

        return sb.toString();
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

    private String intern(final String s) {
        final String internedString = internMap.get(s);
        if (internedString != null) {
            return internedString;
        }
        internMap.put(s, s);
        return s;
    }

    private StringNonTerminal create(final String label, final HashSet<String> pos, final Set<String> nonPosSet,
            final Set<String> rightChildren) {
        final String internLabel = intern(label);

        if (pos.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.POS);

        } else if (nonPosSet.contains(internLabel) && !rightChildren.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY);
        }

        return new StringNonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);
    }

    private class StringProduction {
        public final String parent;
        public final String leftChild;
        public final float probability;

        public StringProduction(final String parent, final String leftChild, final float probability) {
            this.parent = intern(parent);
            this.leftChild = intern(leftChild);
            this.probability = probability;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%.3f)", parent, leftChild, probability);
        }
    }

    private final class BinaryStringProduction extends StringProduction {
        public final String rightChild;

        public BinaryStringProduction(final String parent, final String leftChild, final String rightChild,
                final float probability) {
            super(parent, leftChild, probability);
            this.rightChild = intern(rightChild);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s %s (%.3f)", parent, leftChild, rightChild, probability);
        }
    }

    private final class StringNonTerminal {
        public final String label;
        public final NonTerminalClass ntClass;

        protected StringNonTerminal(final String label, final NonTerminalClass ntClass) {
            this.label = label;
            this.ntClass = ntClass;
        }

        @Override
        public String toString() {
            return label + " " + ntClass.toString();
        }
    }

    private abstract static class StringNonTerminalComparator implements Comparator<StringNonTerminal> {
        HashMap<NonTerminalClass, Integer> map = new HashMap<NonTerminalClass, Integer>();

        @Override
        public int compare(final StringNonTerminal o1, final StringNonTerminal o2) {
            final int i1 = map.get(o1.ntClass);
            final int i2 = map.get(o2.ntClass);

            if (i1 < i2) {
                return -1;
            } else if (i1 > i2) {
                return 1;
            }

            return o1.label.compareTo(o2.label);
        }

        /**
         * @return an array containing leftChildStart, leftChildEnd, rightChildStart, rightChildEnd, posStart, posEnd,
         *         parentEnd
         */
        public abstract int[] startAndEndIndices(HashSet<?> nonPosSet, HashSet<?> leftChildrenSet,
                HashSet<?> rightChildrenSet, HashSet<?> posSet);
    }

    // private static class PosLastComparator extends StringNonTerminalComparator {
    //
    // public PosLastComparator() {
    // map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 0);
    // map.put(NonTerminalClass.EITHER_CHILD, 1);
    // map.put(NonTerminalClass.POS, 2);
    // }
    //
    // @Override
    // public int[] startAndEndIndices(final HashSet<?> nonPosSet, final HashSet<?> leftChildrenSet,
    // final HashSet<?> rightChildrenSet, final HashSet<?> posSet) {
    //
    // final int total = nonPosSet.size() + posSet.size();
    //
    // final int leftChildrenStart = 0;
    // final int leftChildrenEnd = total - 1;
    //
    // final int rightChildrenStart = leftChildrenEnd - rightChildrenSet.size() + 1;
    // final int rightChildrenEnd = total - 1;
    //
    // final int posStart = rightChildrenEnd + 1;
    // final int posEnd = total - 1;
    //
    // final int parentEnd = nonPosSet.size() - 1;
    //
    // return new int[] { leftChildrenStart, leftChildrenEnd, rightChildrenStart, rightChildrenEnd, posStart,
    // posEnd, parentEnd };
    // }
    // }

    private static class PosEmbeddedComparator extends StringNonTerminalComparator {
        public PosEmbeddedComparator() {
            map.put(NonTerminalClass.EITHER_CHILD, 0);
            map.put(NonTerminalClass.POS, 1);
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 2);
        }

        @Override
        public int[] startAndEndIndices(final HashSet<?> nonPosSet, final HashSet<?> leftChildrenSet,
                final HashSet<?> rightChildrenSet, final HashSet<?> posSet) {

            final int total = nonPosSet.size() + posSet.size();

            final int leftChildrenStart = 0;
            final int leftChildrenEnd = total - 1;

            final int rightChildrenStart = 0;
            final int rightChildrenEnd = rightChildrenStart + rightChildrenSet.size() + posSet.size() - 1;

            final int posStart = rightChildrenStart + rightChildrenSet.size();
            final int posEnd = posStart + posSet.size() - 1;

            final int parentEnd = leftChildrenEnd;

            return new int[] { leftChildrenStart, leftChildrenEnd, rightChildrenStart, rightChildrenEnd, posStart,
                    posEnd, parentEnd };
        }
    }

    /**
     * 1 - Left child only (and unary-only, although there shouldn't be many of those)
     * 
     * 2 - Either child (or right-child only, although we don't find many of those)
     * 
     * 3 - All POS (pre-terminals)
     */
    private enum NonTerminalClass {
        FACTORED_SIDE_CHILDREN_ONLY, EITHER_CHILD, POS;
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
