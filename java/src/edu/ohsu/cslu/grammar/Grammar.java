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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.lela.FractionalCountGrammar;
import edu.ohsu.cslu.parser.Util;
import edu.ohsu.cslu.util.StringPool;

/**
 * Represents a Probabilistic Context Free Grammar (PCFG). Such grammars may be built up programatically or may be
 * inferred from a corpus of data. Various PCFG representations are implemented in subclasses.
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
 * Although branching of arbitrary size is possible in a grammar under this definition, this class hierarchy models
 * grammars which have been factored into binary-branching form, enabling much more efficient computational approaches
 * to be used.
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

    private final static long serialVersionUID = 3L;

    /** Marks the switch from PCFG to lexicon entries in the grammar file */
    public final static String GRAMMAR_DELIMITER = "===== GRAMMAR =====";
    public final static String LEXICON_DELIMITER = "===== LEXICON =====";

    // == Grammar Basics ==
    public GrammarFormatType grammarFormat;
    public final Vocabulary nonTermSet;

    // TODO This comment may have been true at some point (and referred to static lexSet and tokenizer instances), but
    // tests appear to pass with member variables instead, and for unit testing we construct multiple separate grammars
    // with differing lexicons.
    // The lexSet and tokenizer need to be shared across multiple grammars so that
    // token indices are identical.
    public SymbolSet<String> lexSet = new SymbolSet<String>();
    public Tokenizer tokenizer = new Tokenizer(lexSet);

    // maps from 0-based index to entry in nonTermSet. Used to reduce absolute
    // index value for feature extraction
    public final SymbolSet<Integer> posSet;
    public final SymbolSet<Integer> phraseSet; // phraseSet + posSet == nonTermSet
    // TODO: factored/non-factored phraseSet?

    protected final ArrayList<Production> binaryProductions;
    protected final ArrayList<Production> unaryProductions;
    protected final ArrayList<Production> lexicalProductions;

    protected final Collection<Production>[] unaryProductionsByChild;
    protected final Collection<Production>[] lexicalProdsByChild;
    protected final short[][] lexicalParents; // [lexIndex][valid parent ntIndex]
    protected final float[][] lexicalLogProbabilities;

    // public final SymbolSet<String> evalNonTermSet;
    // protected final int[] nt2evalntMap;

    public final static String nullSymbolStr = "<null>";
    public static Production nullProduction;
    // public static float UNSEEN_LEX_PROB = GlobalConfigProperties.singleton().getFloatProperty("unseenLexProb");
    public static float UNSEEN_LEX_PROB = -9999;

    public int nullSymbol = -1;
    public int nullWord = -1;
    public int startSymbol = -1;

    // == Grammar stats ==
    public String startSymbolStr;
    public int numPosSymbols;
    private boolean isLeftFactored;
    // public boolean isLatentVariableGrammar;
    // public boolean annotatePOS;
    public int horizontalMarkov;
    public int verticalMarkov;
    public String binarization;
    public String language;

    // == Aaron's Grammar variables ==
    public final int leftChildrenStart; // The first NT valid as a left child.
    public final int leftChildrenEnd; // The last non-POS NT valid as a left child.
    public final int rightChildrenStart; // The first non-POS NT valid as a right child.
    public final int rightChildrenEnd; // The last non-POS NT valid as a right child.
    public final int posStart; // The first POS.
    public final int posEnd; // The last POS.

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
    private StringPool stringPool;

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

        final List<StringProduction> pcfgRules = new LinkedList<StringProduction>();
        final List<StringProduction> lexicalRules = new LinkedList<StringProduction>();

        BaseLogger.singleton().finer("INFO: Reading grammar ... ");
        this.stringPool = new StringPool();
        this.grammarFormat = readPcfgAndLexicon(grammarFile, pcfgRules, lexicalRules);

        nonTermSet = new Vocabulary(grammarFormat);
        posSet = new SymbolSet<Integer>();
        phraseSet = new SymbolSet<Integer>();

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

        // NB: I'm not sure what is going in this Grammar constructor, but somewhere we
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
        // TODO Sorting with the PosFirstComparator might speed up FOM initialization a bit, but breaks OpenCL parsers.
        // Make it an option.
        final StringNonTerminalComparator comparator = new PosEmbeddedComparator();
        final TreeSet<StringNonTerminal> sortedNonTerminals = new TreeSet<StringNonTerminal>(comparator);
        for (final String nt : nonTerminals) {
            sortedNonTerminals.add(create(nt, pos, nonPosSet, rightChildrenSet));
        }

        // nt2evalntMap = new int[sortedNonTerminals.size()];
        for (final StringNonTerminal nt : sortedNonTerminals) {
            final short ntIndex = (short) nonTermSet.addSymbol(nt.label);

            // Added by nate to make Cell Constraints work again
            getOrAddNonterm(ntIndex).isFactored = grammarFormat.isFactored(nt.label);

            // final String evalNT = grammarFormat.getEvalNonTerminal(nt.label);
            // int evalNTIndex = evalNonTermSet.addSymbol(evalNT);
        }

        this.startSymbol = nonTermSet.addSymbol(startSymbolStr);
        nonTermSet.setStartSymbol((short) startSymbol);
        this.nullSymbol = nonTermSet.addSymbol(nullSymbolStr);
        this.nullWord = lexSet.addSymbol(nullSymbolStr);

        // Now that all NTs are mapped, we can create Production instances for lexical rules (we don't care
        // about sort order here)
        lexicalProductions = new ArrayList<Production>();

        for (final StringProduction lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild);
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex,
                    lexicalRule.probability, true, this));
        }

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

        unaryProductionsByChild = storeProductionByChild(unaryProductions, nonTermSet.size() - 1);
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);

        this.lexicalParents = new short[lexicalProdsByChild.length][];
        this.lexicalLogProbabilities = new float[lexicalProdsByChild.length][];
        initLexicalProbabilities();

        stringPool = null; // We no longer need the String intern map, so let it be GC'd

        // this.tokenizer = new Tokenizer(lexSet);

        // Initialize indices
        final int[] startAndEndIndices = startAndEndIndices();
        this.leftChildrenStart = startAndEndIndices[0];
        this.leftChildrenEnd = startAndEndIndices[1];
        this.rightChildrenStart = startAndEndIndices[2];
        this.rightChildrenEnd = startAndEndIndices[3];
        this.posStart = startAndEndIndices[4];
        this.posEnd = startAndEndIndices[5];

        this.numPosSymbols = posEnd - posStart + 1;
        this.maxPOSIndex = posEnd;

        // reduce range of POS indices so we can store the features more efficiently
        for (int i = 0; i < numNonTerms(); i++) {
            if (pos.contains(nonTermSet.getSymbol(i))) {
                posSet.addSymbol(i);
            } else {
                phraseSet.addSymbol(i);
            }
        }
    }

    public Grammar(final String grammarFile) throws IOException {
        this(new InputStreamReader(Util.file2inputStream(grammarFile)));
    }

    protected Grammar(final ArrayList<Production> binaryProductions, final ArrayList<Production> unaryProductions,
            final ArrayList<Production> lexicalProductions, final SymbolSet<String> vocabulary,
            final SymbolSet<String> lexicon, final GrammarFormatType grammarFormat) {
        this.nonTermSet = (Vocabulary) vocabulary;
        this.startSymbol = nonTermSet.startSymbol();
        this.nullSymbol = -1;
        this.startSymbolStr = vocabulary.getSymbol(startSymbol);
        this.nonTermInfo = null;

        this.lexSet = lexicon;
        this.tokenizer = new Tokenizer(lexicon);

        this.posSet = null;
        this.phraseSet = null;

        this.binaryProductions = binaryProductions;
        this.unaryProductions = unaryProductions;
        this.lexicalProductions = lexicalProductions;

        this.unaryProductionsByChild = null;
        this.lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);

        this.lexicalParents = new short[lexicalProdsByChild.length][];
        this.lexicalLogProbabilities = new float[lexicalProdsByChild.length][];
        initLexicalProbabilities();

        this.maxPOSIndex = -1;
        this.numPosSymbols = -1;
        this.grammarFormat = grammarFormat;

        // TODO Correct this; replace with Binarization.LEFT or Binarization.RIGHT
        this.isLeftFactored = false;

        // Initialize start and end indices
        final int[] startAndEndIndices = startAndEndIndices();
        this.leftChildrenStart = startAndEndIndices[0];
        this.leftChildrenEnd = startAndEndIndices[1];
        this.rightChildrenStart = startAndEndIndices[2];
        this.rightChildrenEnd = startAndEndIndices[3];
        this.posStart = startAndEndIndices[4];
        this.posEnd = startAndEndIndices[5];
    }

    /**
     * Construct a {@link Grammar} instance from an existing instance. This is used when constructing a subclass of
     * {@link Grammar} from a binary-serialized {@link Grammar}.
     * 
     * @param g
     */
    protected Grammar(final Grammar g) {
        this(g.binaryProductions, g.unaryProductions, g.lexicalProductions, g.nonTermSet, g.lexSet, g.grammarFormat);
        this.startSymbolStr = g.startSymbolStr;

        this.nullSymbol = g.nullSymbol;
        this.startSymbol = g.startSymbol;
        this.maxPOSIndex = g.maxPOSIndex;
        this.numPosSymbols = g.numPosSymbols;
        this.grammarFormat = g.grammarFormat;

        this.isLeftFactored = g.isLeftFactored;
    }

    // Read in the grammar file.
    private GrammarFormatType readPcfgAndLexicon(final Reader grammarFile, final List<StringProduction> pcfgRules,
            final List<StringProduction> lexicalRules) throws IOException {

        GrammarFormatType gf;
        final BufferedReader br = new BufferedReader(grammarFile);
        br.mark(50);

        // Read the first line and try to guess the grammar format
        final String firstLine = br.readLine();
        if (firstLine.contains("format=Berkeley")) {
            gf = GrammarFormatType.Berkeley;
            final HashMap<String, String> keyVals = Util.readKeyValuePairs(firstLine.trim());
            startSymbolStr = keyVals.get("start");
        } else if (firstLine.matches("^[A-Z]+_[0-9]+")) {
            gf = GrammarFormatType.Berkeley;
            startSymbolStr = firstLine;
        } else if (firstLine.contains("format=CSLU") || firstLine.contains("format=BUBS")) {
            gf = GrammarFormatType.CSLU;
            // final Pattern p = Pattern.compile("^.*start=([^ ]+).*$");
            // startSymbolStr = p.matcher(firstLine).group(1);
            final HashMap<String, String> keyVals = Util.readKeyValuePairs(firstLine.trim());
            startSymbolStr = keyVals.get("start");
            try {
                horizontalMarkov = Integer.parseInt(keyVals.get("hMarkov"));
                verticalMarkov = Integer.parseInt(keyVals.get("vMarkov"));
                language = keyVals.get("language");
                binarization = keyVals.get("binarization");
            } catch (final Exception e) {
                // If grammar doesn't contain these values, just ignore it.
            }
        } else if (firstLine.split(" ").length > 1) {
            // The first line was not a start symbol.
            // Roark-format assumes 'TOP'. Reset the reader and re-process that line
            gf = GrammarFormatType.Roark;
            startSymbolStr = "TOP";
            br.reset();
        } else {
            throw new IllegalArgumentException("Unexpected first line of grammar file: " + firstLine);
        }

        // for (String line = br.readLine(); line != null && !line.equals(DELIMITER); line = br.readLine()) {
        final Pattern p = Pattern.compile("\\s");
        for (String line = br.readLine(); !line.equals(LEXICON_DELIMITER); line = br.readLine()) {
            final String[] tokens = p.split(line);

            if ((tokens.length > 0 && tokens[0].equals("#")) || line.trim().equals("")) {
                // '#' indicates a comment. Skip line.
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
                final String[] tokens = p.split(line);
                // if ((tokens.length > 0 && tokens[0].equals("#")) || line.trim().equals("")) {
                // NB: There are lexical productions that start with '#', namely '# -> #'
                if (line.trim().equals("")) {
                    // skip blank lines
                } else if (tokens.length == 4) {
                    // expecting: A -> B prob
                    lexicalRules.add(new StringProduction(tokens[0], tokens[2], Float.valueOf(tokens[3])));
                } else {
                    throw new IllegalArgumentException("Unexpected line in grammar lexicon\n\t" + line);
                }
            }
        }

        return gf;
    }

    /**
     * Populates lexicalLogProbabilities and lexicalParents
     * 
     * @return
     */
    private void initLexicalProbabilities() {
        for (int child = 0; child < lexicalProdsByChild.length; child++) {
            lexicalParents[child] = new short[lexicalProdsByChild[child].size()];
            lexicalLogProbabilities[child] = new float[lexicalProdsByChild[child].size()];
            int j = 0;
            for (final Production p : lexicalProdsByChild[child]) {
                lexicalParents[child][j] = (short) p.parent;
                lexicalLogProbabilities[child][j++] = p.prob;
            }
        }
    }

    private int[] startAndEndIndices() {

        int leftChildrenStart = Integer.MAX_VALUE, leftChildrenEnd = 0;
        int rightChildrenStart = Integer.MAX_VALUE, rightChildrenEnd = 0;
        int posStart = Integer.MAX_VALUE, posEnd = 0;
        int parentEnd = 0;

        for (final Production p : binaryProductions) {
            if (p.leftChild < leftChildrenStart) {
                leftChildrenStart = p.leftChild;
            }
            if (p.leftChild > leftChildrenEnd) {
                leftChildrenEnd = p.leftChild;
            }
            if (p.rightChild < rightChildrenStart) {
                rightChildrenStart = p.rightChild;
            }
            if (p.rightChild > rightChildrenEnd) {
                rightChildrenEnd = p.rightChild;
            }
            if (p.parent > parentEnd) {
                parentEnd = p.parent;
            }
        }

        for (final Production p : unaryProductions) {
            if (p.leftChild < leftChildrenStart) {
                leftChildrenStart = p.leftChild;
            }
            if (p.leftChild > leftChildrenEnd) {
                leftChildrenEnd = p.leftChild;
            }
            if (p.parent > parentEnd) {
                parentEnd = p.parent;
            }
        }

        for (final Production p : lexicalProductions) {
            if (p.parent > parentEnd) {
                parentEnd = p.parent;
            }
            if (p.parent < posStart) {
                posStart = p.parent;
            }
            if (p.parent > posEnd) {
                posEnd = p.parent;
            }
        }

        if (posStart > this.nullSymbol) {
            posStart = nullSymbol;
        }
        if (posEnd < this.nullSymbol) {
            posEnd = nullSymbol;
        }

        return new int[] { leftChildrenStart, leftChildrenEnd, rightChildrenStart, rightChildrenEnd, posStart, posEnd,
                parentEnd };
    }

    public static Grammar read(final String grammarFile) throws IOException, ClassNotFoundException {
        final InputStream is = Util.file2inputStream(grammarFile);
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

    /**
     * Returns the non-terminal parents for the specified child. See also {@link #lexicalLogProbabilities(int)}.
     * 
     * @param child Word, as mapped in the lexicon
     * @return All non-terminal parents for the specified child
     */
    public final short[] lexicalParents(final int child) {
        return lexicalParents[child];
    }

    /**
     * Returns the log probabilities of the non-terminal parents returned by {@Link #lexicalParents(int)} for the
     * specified child.
     * 
     * @param child Word, as mapped in the lexicon
     * @return The log probabilities of each non-terminal parent for the specified child
     */
    public final float[] lexicalLogProbabilities(final int child) {
        return lexicalLogProbabilities[child];
    }

    public final boolean hasWord(final String s) {
        return lexSet.hasSymbol(s);
    }

    // TODO: I don't like that we have getNonterminal() and mapNonterminal()
    // methods. Should mapNonterminal return a NonTerminal instead of a string?
    public final NonTerminal getOrAddNonterm(final int index) {
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

    public final int mapLexicalEntry(final String lexStr) {
        return lexSet.getIndex(lexStr);
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
        final int i = Arrays.binarySearch(lexicalParents(child), (short) parent);
        return (i < 0) ? UNSEEN_LEX_PROB : lexicalLogProbabilities[child][i];
        // return lexicalLogProbabilityMaps[child].get((short) parent);
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
        return UNSEEN_LEX_PROB;
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

    public Grammar toUnsplitGrammar() {
        final Vocabulary baseVocabulary = nonTermSet.baseVocabulary();
        final FractionalCountGrammar unsplitGrammar = new FractionalCountGrammar(baseVocabulary, lexSet, null);

        for (final Production p : binaryProductions) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            final short unsplitLeftChild = nonTermSet.getBaseIndex((short) p.leftChild);
            final short unsplitRightChild = nonTermSet.getBaseIndex((short) p.rightChild);
            unsplitGrammar.incrementBinaryCount(unsplitParent, unsplitLeftChild, unsplitRightChild, Math.exp(p.prob));
        }

        for (final Production p : unaryProductions) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            final short unsplitChild = nonTermSet.getBaseIndex((short) p.leftChild);
            unsplitGrammar.incrementUnaryCount(unsplitParent, unsplitChild, Math.exp(p.prob));
        }

        for (final Production p : lexicalProductions) {
            final short unsplitParent = nonTermSet.getBaseIndex((short) p.parent);
            unsplitGrammar.incrementLexicalCount(unsplitParent, p.leftChild, Math.exp(p.prob));
        }

        try {
            return getClass().getConstructor(
                    new Class[] { ArrayList.class, ArrayList.class, ArrayList.class, SymbolSet.class, SymbolSet.class,
                            GrammarFormatType.class }).newInstance(
                    new Object[] { unsplitGrammar.binaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.unaryProductions(Float.NEGATIVE_INFINITY),
                            unsplitGrammar.lexicalProductions(Float.NEGATIVE_INFINITY), baseVocabulary, lexSet,
                            grammarFormat });
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
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
            if (getOrAddNonterm(mapNonterminal(nt)).isFactored()) {
                nFactored++;
            } else {
                nUnFactored++;
            }
        }

        final StringBuilder sb = new StringBuilder(256);
        sb.append("INFO:");
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
        sb.append(" binarization=" + (isLeftFactored() ? "left" : "right"));
        sb.append(" grammarFormat=" + grammarFormat);

        return sb.toString();
    }

    public String getStatsVerbose() {

        int nFactored = 0, nUnFactored = 0;
        for (final String nt : nonTermSet) {
            if (getOrAddNonterm(mapNonterminal(nt)).isFactored()) {
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
        sb.append("Binarization: " + (isLeftFactored() ? "left" : "right") + '\n');
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
    }

    private static class PosFirstComparator extends StringNonTerminalComparator {

        public PosFirstComparator() {
            map.put(NonTerminalClass.POS, 0);
            map.put(NonTerminalClass.EITHER_CHILD, 1);
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 1);
        }
    }

    private static class PosEmbeddedComparator extends StringNonTerminalComparator {

        public PosEmbeddedComparator() {
            map.put(NonTerminalClass.EITHER_CHILD, 0);
            map.put(NonTerminalClass.POS, 1);
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 2);
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(1024 * 1024);

        sb.append(nonTermSet.getSymbol(startSymbol) + '\n');

        for (final Production p : binaryProductions) {
            sb.append(String.format("%s -> %s %s %.4f\n", nonTermSet.getSymbol(p.parent),
                    nonTermSet.getSymbol(p.leftChild), nonTermSet.getSymbol(p.rightChild), p.prob));
        }

        for (final Production p : unaryProductions) {
            sb.append(String.format("%s -> %s %.4f\n", nonTermSet.getSymbol(p.parent),
                    nonTermSet.getSymbol(p.leftChild), p.prob));
        }
        sb.append("===Lexicon===\n");
        for (final Production p : lexicalProductions) {
            sb.append(String.format("%s -> %s %.4f\n", nonTermSet.getSymbol(p.parent), lexSet.getSymbol(p.leftChild),
                    p.prob));
        }
        return sb.toString();
    }

    public boolean isCoarseGrammar() {
        return false;
    }
}
