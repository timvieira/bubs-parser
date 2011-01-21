package edu.ohsu.cslu.grammar;

import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
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
import java.util.zip.GZIPInputStream;

import cltool4j.GlobalLogger;
import edu.ohsu.cslu.parser.ParserUtil;
import edu.ohsu.cslu.util.StringPool;

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
 * TODO Fix this comment
 * 
 * Note - there will always be more productions than categories, but all categories except the special S-dagger category
 * are also productions. The index of a category will be the same when used as a production as it is when used as a
 * category.
 * 
 * TODO Implement an even simpler grammar for use as an intermediate form when transforming one grammar to another.
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
    private final static long serialVersionUID = 3L;

    // == Grammar Basics ==
    public GrammarFormatType grammarFormat;
    public final SymbolSet<String> nonTermSet;
    public SymbolSet<String> lexSet;
    public final Tokenizer tokenizer;
    // maps from 0-based index to entry in nonTermSet. Used to reduce absolute index value for feature extraction
    public final SymbolSet<Integer> posSet;
    public final SymbolSet<Integer> phraseSet; // phraseSet + posSet == nonTermSet
    // TODO: factored/non-factored phraseSet?

    protected final ArrayList<Production> binaryProductions;
    protected final ArrayList<Production> unaryProductions;
    protected final ArrayList<Production> lexicalProductions;

    protected final Collection<Production>[] unaryProductionsByChild;
    protected final Collection<Production>[] lexicalProdsByChild;
    protected final short[][] lexicalParents; // [lexIndex][valid parent ntIndex]
    protected final Short2FloatOpenHashMap[] lexicalLogProbabilities;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;

    public int nullSymbol = -1;
    public int nullWord = -1;
    public int startSymbol = -1;

    // == Grammar stats ==
    public int numPosSymbols;
    public int numFactoredSymbols;
    public int numNonFactoredSymbols;
    private boolean isLeftFactored;
    public boolean annotatePOS;
    public boolean isLatentVariableGrammar;
    public int horizontalMarkov;
    public int verticalMarkov;

    // == Aaron's Grammar variables ==
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

    // == Nate's Grammar variables ==
    // Nate's way of keeping meta data on each NonTerm; Aaron orders them and returns
    // info based on index range.
    private ArrayList<NonTerminal> nonTermInfo = new ArrayList<NonTerminal>();
    protected int maxPOSIndex = -1; // used when creating arrays to hold all POS entries

    /**
     * A temporary String -> String map, used to conserve memory while reading and sorting the grammar. We don't need to
     * internalize Strings indefinitely, so we map them ourselves and allow the map to be GC'd after we're done
     * constructing the grammar.
     */
    private StringPool stringPool = new StringPool();

    /**
     * Signature of the first 2 bytes of a binary Java Serialized Object. Allows us to use the same command-line option
     * for serialized and text grammars and auto-detect the format
     */
    private final static short OBJECT_SIGNATURE = (short) 0xACED;

    // public Grammar() {
    // nonTermSet = new SymbolSet<String>();
    // lexSet = new SymbolSet<String>();
    // }

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
        posSet = new SymbolSet<Integer>();
        phraseSet = new SymbolSet<Integer>();

        final List<StringProduction> pcfgRules = new LinkedList<StringProduction>();
        final List<StringProduction> lexicalRules = new LinkedList<StringProduction>();

        GlobalLogger.singleton().fine("Reading grammar ... ");
        this.grammarFormat = readPcfgAndLexicon(grammarFile, pcfgRules, lexicalRules);

        GlobalLogger.singleton().fine("transforming ... ");
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

        // NATE: I'm not sure what is going in this Grammar constructor, but somewhere we
        // lost the function to decide if the grammar is right/left factored. Maybe this code should
        // go somewhere else?
        assert leftChildrenSet.size() > 0 && rightChildrenSet.size() > 0;
        // System.out.println("#left=" + leftChildrenSet.size() + " #right=" + rightChildrenSet.size());
        if (leftChildrenSet.size() < rightChildrenSet.size()) {
            this.isLeftFactored = true;
        } else {
            this.isLeftFactored = false;
        }

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and
        // dummy non-terminals). Label null symbol as a POS, and start symbol as not.
        nonTerminals.add(nullSymbolStr);
        pos.add(nullSymbolStr);

        nonTerminals.add(startSymbolStr);
        nonPosSet.add(startSymbolStr);

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
            final int ntIndex = nonTermSet.addSymbol(nt.label);

            // Added by nate to make Cell Constraints work again
            getNonterminal(ntIndex).isFactored = grammarFormat.isFactored(nt.label);
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
        nullWord = lexSet.addSymbol(nullSymbolStr);

        // Now that all NTs are mapped, we can create Production instances for lexical rules (we don't care
        // about sort order here)
        lexicalProductions = new ArrayList<Production>();

        for (final StringProduction lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild);
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex,
                    lexicalRule.probability, true, this));
        }
        numLexProds = lexicalProductions.size();

        // And unary and binary rules
        binaryProductions = new ArrayList<Production>();
        unaryProductions = new ArrayList<Production>();

        for (final StringProduction grammarRule : pcfgRules) {
            if (grammarRule instanceof BinaryStringProduction) {
                binaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild,
                        ((BinaryStringProduction) grammarRule).rightChild, grammarRule.probability, this));
            } else {
                unaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild, grammarRule.probability,
                        false, nonTermSet, lexSet));
            }
        }

        numBinaryProds = binaryProductions.size();
        numUnaryProds = unaryProductions.size();

        unaryProductionsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);

        lexicalParents = new short[lexicalProdsByChild.length][];
        lexicalLogProbabilities = new Short2FloatOpenHashMap[lexicalProdsByChild.length];
        for (int child = 0; child < lexicalProdsByChild.length; child++) {
            lexicalParents[child] = new short[lexicalProdsByChild[child].size()];
            lexicalLogProbabilities[child] = new Short2FloatOpenHashMap(lexicalProdsByChild[child].size());
            int j = 0;
            for (final Production p : lexicalProdsByChild[child]) {
                lexicalParents[child][j++] = (short) p.parent;
                lexicalLogProbabilities[child].put((short) p.parent, p.prob);
            }
        }

        stringPool = null; // We no longer need the String intern map, so let it be GC'd

        this.tokenizer = new Tokenizer(lexSet);

        // reduce range of POS indicies so we can store the features more efficiently
        for (int i = 0; i < numNonTerms(); i++) {
            // TODO: I don't think this works for non-sorted grammars like Nate's
            // if (isPos(i)) {
            if (pos.contains(nonTermSet.getSymbol(i))) {
                posSet.addSymbol(i);
            } else {
                phraseSet.addSymbol(i);
            }
        }

        GlobalLogger.singleton().fine("done.");
    }

    public Grammar(final String grammarFile) throws IOException {
        this(new FileReader(grammarFile));
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
        this.lexicalParents = g.lexicalParents;
        this.lexicalLogProbabilities = g.lexicalLogProbabilities;

        this.nullSymbol = g.nullSymbol;
        this.startSymbol = g.startSymbol;
        this.maxPOSIndex = g.maxPOSIndex;
        this.numPosSymbols = g.numPosSymbols;
        this.grammarFormat = g.grammarFormat;

        this.isLeftFactored = g.isLeftFactored;

        this.nonTermSet = g.nonTermSet;
        this.lexSet = g.lexSet;
        this.posSet = g.posSet;
        this.phraseSet = g.phraseSet;
        this.nonTermInfo = g.nonTermInfo;

        this.tokenizer = g.tokenizer;
    }

    protected Grammar(final ArrayList<Production> binaryProductions, final ArrayList<Production> unaryProductions,
            final ArrayList<Production> lexicalProductions, final SymbolSet<String> vocabulary,
            final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat) {
        this.nonTermSet = vocabulary;
        this.lexSet = lexicon;
        this.startSymbol = 0;
        this.nullSymbol = -1;
        this.startSymbolStr = vocabulary.getSymbol(startSymbol);
        this.nonTermInfo = null;
        this.tokenizer = new Tokenizer(lexicon);

        this.posSet = null;
        this.phraseSet = null;

        this.leftChildrenStart = -1;
        this.leftChildrenEnd = -1;
        this.rightChildrenStart = -1;
        this.rightChildrenEnd = -1;
        this.posStart = -1;
        this.posEnd = -1;
        this.parentEnd = -1;

        this.numBinaryProds = binaryProductions.size();
        this.numUnaryProds = unaryProductions.size();
        this.numLexProds = lexicalProductions.size();

        this.binaryProductions = binaryProductions;
        this.unaryProductions = unaryProductions;
        this.lexicalProductions = lexicalProductions;

        this.unaryProductionsByChild = null;
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
        this.lexicalParents = null;
        this.lexicalLogProbabilities = null;

        this.maxPOSIndex = -1;
        this.numPosSymbols = -1;
        this.grammarFormat = grammarFormat;

        this.isLeftFactored = false;
    }

    private GrammarFormatType readPcfgAndLexicon(final Reader grammarFile, final List<StringProduction> pcfgRules,
            final List<StringProduction> lexicalRules) throws IOException {
        // Read in the grammar file.

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

        // for (String line = br.readLine(); line != null && !line.equals(DELIMITER); line = br.readLine()) {
        for (String line = br.readLine(); !line.equals(DELIMITER); line = br.readLine()) {
            final String[] tokens = line.split("\\s");

            if (tokens.length == 1) {
                throw new IllegalArgumentException(
                        "Grammar file must contain a single line with a single string representing the START SYMBOL.\n"
                                + "More than one entry was found.  Last line: " + line);
            } else if (tokens.length == 4) {
                // Unary production: expecting: A -> B prob
                // TODO: Should we make sure there aren't any duplicates?
                pcfgRules.add(new StringProduction(tokens[0], tokens[2], Float.valueOf(tokens[3])));
            } else if (tokens.length == 5) {
                // Binary production: expecting: A -> B C prob
                pcfgRules.add(new BinaryStringProduction(stringPool.intern(tokens[0]), stringPool.intern(tokens[2]),
                        stringPool.intern(tokens[3]), Float.valueOf(tokens[4])));
            } else {
                throw new IllegalArgumentException("Unexpected line in grammar PCFG\n\t" + line);
            }
        }

        // Read Lexicon after finding DELIMITER
        for (String line = br.readLine(); line != null || lexicalRules.size() == 0; line = br.readLine()) {
            if (line != null) {
                final String[] tokens = line.split("\\s");
                if (tokens.length == 4) {
                    // expecting: A -> B prob
                    lexicalRules.add(new StringProduction(tokens[0], tokens[2], Float.valueOf(tokens[3])));
                } else {
                    throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
                }
            }
        }

        return gf;
    }

    public static Grammar read(final String grammarFile) throws IOException, ClassNotFoundException {
        InputStream is = new FileInputStream(grammarFile);
        if (grammarFile.endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        final Grammar grammar = Grammar.read(is);
        is.close();
        return grammar;
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

        for (int i = 0; i < prodsByChild.length; i++) {
            prodsByChild[i] = new LinkedList<Production>();
        }

        for (final Production p : prods) {
            prodsByChild[p.child()].add(p);
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

    public final short[] lexicalParents(final int child) {
        return lexicalParents[child];
    }

    public final boolean hasWord(final String s) {
        return lexSet.hasSymbol(s);
    }

    int addNonTerm(final String nonTerm) {
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

    public Collection<Production> getFactoredBinaryProductions() {
        final List<Production> factoredProductions = new LinkedList<Production>();
        for (final Production p : binaryProductions) {
            if (p.isBinaryProd() && grammarFormat.isFactored(mapNonterminal(p.parent))) {
                factoredProductions.add(p);
            }
        }
        return factoredProductions;
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
        // return getProductionProb(getLexicalProduction(parent, child));
        return lexicalLogProbabilities[child].get((short) parent);
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
        int nFactored = 0, nUnFactored = 0;
        for (final String nt : nonTermSet) {
            if (getNonterminal(mapNonterminal(nt)).isFactored()) {
                nFactored++;
            } else {
                nUnFactored++;
            }
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("INFO: grammar:");
        sb.append(" binaryRules=" + numBinaryProds());
        sb.append(" unaryRules=" + numUnaryProds());
        sb.append(" lexicalRules=" + numLexProds());

        sb.append(" nonTerminals=" + numNonTerms());
        sb.append(" lexicalSymbols=" + lexSet.size());
        sb.append(" posSymbols=" + numPosSymbols());
        sb.append(" maxPosIndex=" + maxPOSIndex);
        sb.append(" factoredNTs=" + nFactored);
        sb.append(" unfactoredNTs=" + nUnFactored);

        sb.append(" startSymbol=" + nonTermSet.getSymbol(startSymbol));
        sb.append(" nullSymbol=" + nonTermSet.getSymbol(nullSymbol));
        sb.append(" factorization=" + (isLeftFactored() ? "left" : "right"));
        sb.append(" grammarFormat=" + grammarFormat);

        return sb.toString();
    }

    public String getStatsVerbose() {

        int nFactored = 0, nUnFactored = 0;
        for (final String nt : nonTermSet) {
            if (getNonterminal(mapNonterminal(nt)).isFactored()) {
                nFactored++;
            } else {
                nUnFactored++;
            }
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + numBinaryProds() + '\n');
        sb.append("Unary rules: " + numUnaryProds() + '\n');
        sb.append("Lexical rules: " + numLexProds() + '\n');

        sb.append("Non Terminals: " + numNonTerms() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + numPosSymbols() + '\n');
        sb.append("Max POS index: " + maxPOSIndex + '\n');
        sb.append("Factored NTs: " + nFactored + '\n');
        sb.append("UnFactored NTs: " + nUnFactored + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');
        sb.append("Factorization: " + (isLeftFactored() ? "left" : "right") + '\n');
        sb.append("GrammarFormat: " + grammarFormat + '\n');

        return sb.toString();
    }

    private StringNonTerminal create(final String label, final HashSet<String> pos, final Set<String> nonPosSet,
            final Set<String> rightChildren) {
        final String internLabel = stringPool.intern(label);

        if (startSymbolStr.equals(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);

        } else if (pos.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.POS);

        } else if (nonPosSet.contains(internLabel) && !rightChildren.contains(internLabel)) {
            return new StringNonTerminal(internLabel, NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY);
        }

        return new StringNonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);
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
            final int rightChildrenEnd = rightChildrenStart + rightChildrenSet.size() + posSet.size();

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
                // TODO We shouldn't really need to check for a single '@' symbol - that should only be a terminal.
                // Trace back and eliminate calling isFactored() for terminals.
                return nonTerminal.startsWith("@") && nonTerminal.length() > 1;
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");
            }
        }

        public String getBaseNT(String contents) {
            switch (this) {
            case CSLU:
                if (isFactored(contents)) {
                    contents = contents.substring(0, contents.indexOf("|"));
                }
                if (contents.contains("^")) {
                    contents = contents.substring(0, contents.indexOf("^"));
                }
                return contents;
            case Berkeley:
                if (isFactored(contents)) {
                    return contents.substring(1);
                }
                return contents;
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");
            }
        }

        public String createFactoredNT(final String unfactoredParent, final LinkedList<String> markovChildrenStr) {
            switch (this) {
            case CSLU:
                return unfactoredParent + "|<" + ParserUtil.join(markovChildrenStr, "-") + ">";
            case Berkeley:
                if (markovChildrenStr.size() > 0) {
                    GlobalLogger.singleton().info(
                            "ERROR: Berkeley grammar does not support horizontal markov smoothing for factored nodes");
                    System.exit(1);
                }
                return "@" + unfactoredParent;
            case Roark:
                // TODO Support Roark format
            default:
                throw new IllegalArgumentException("Unsupported format");
            }
        }

        public String createParentNT(final String contents, final LinkedList<String> parentsStr) {
            switch (this) {
            case CSLU:
                String base = contents,
                rest = "";

                if (isFactored(contents)) {
                    final int i = contents.indexOf("|");
                    base = contents.substring(0, i);
                    rest = contents.substring(i);
                }
                return base + "^<" + ParserUtil.join(parentsStr, "-") + ">" + rest;
            case Berkeley:
            case Roark:
            default:
                throw new IllegalArgumentException("Unsupported format");
            }
        }
    }

    public int horizontalMarkov() {
        return horizontalMarkov;
    }

    public int verticalMarkov() {
        return verticalMarkov;
    }

    public boolean annotatePOS() {
        return annotatePOS;
    }

    public String toMappingString() {
        final StringBuilder sb = new StringBuilder(numNonTerms() * 25);
        for (int i = 0; i < numNonTerms(); i++) {
            sb.append(i + " -> " + nonTermSet.getSymbol(i) + '\n');
        }
        sb.append("===Lexicon===\n");
        for (int i = 0; i < numLexSymbols(); i++) {
            sb.append(i + " -> " + lexSet.getSymbol(i) + '\n');
        }
        return sb.toString();
    }
}
