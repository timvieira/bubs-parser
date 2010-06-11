package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.util.Log;

/**
 * Sorted grammar implementation. Reads the grammar into memory and sorts non-terminals (V) according to their
 * occurrence in binary rules. This can allow more efficient iteration in grammar intersection (e.g., skipping
 * NTs only valid as left children in the right cell) and more efficient chart storage (e.g., omitting storage
 * for POS NTs in chart rows >= 2).
 * 
 * @author Aaron Dunlop
 * @since Jun 6, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class SortedGrammar extends GrammarByChild {

    public String startSymbolStr = null;

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

    /**
     * A temporary String -> String map, used to conserve memory while reading and sorting the grammar.
     * Similar to {@link String}'s own intern map, but we don't need to internalize Strings indefinitely, so
     * we map them ourselves and allow the map to be GC'd after we're done constructing the grammar.
     */
    private HashMap<String, String> internMap = new HashMap<String, String>();

    /**
     * Constructor
     */
    protected SortedGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    /**
     * Default Constructor. This constructor does an inordinate amount of work directly in the constructor
     * specifically so we can initialize final instance variables. Making the instance vars final allows the
     * JIT to inline them everywhere we use them, improving runtime efficiency considerably.
     */
    protected SortedGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {

        final HashSet<String> nonTerminals = new HashSet<String>();
        final HashSet<String> pos = new HashSet<String>();

        // Read in the lexical productions first. Label any non-terminals found in the lexicon as POS tags. We
        // assume that pre-terminals (POS) will only occur as parents in span-1 rows and as children in span-2
        // rows
        Log.info(1, "INFO: Reading lexical productions");
        final List<StringRule> lexicalRules = readLexProds(lexiconFile);
        for (final StringRule lexicalRule : lexicalRules) {
            nonTerminals.add(lexicalRule.parent);
            pos.add(lexicalRule.parent);
        }

        // Now read in the grammar file.
        Log.info(1, "INFO: Reading grammar");
        final List<StringRule> grammarRules = readGrammar(grammarFile, grammarFormat);

        // All non-terminals
        final HashSet<String> nonPosSet = new HashSet<String>();
        final HashSet<String> rightChildrenSet = new HashSet<String>();
        final HashSet<String> leftChildrenSet = new HashSet<String>();

        // Iterate through grammar rules, populating temporary non-terminal sets
        for (final StringRule grammarRule : grammarRules) {

            nonTerminals.add(grammarRule.parent);
            nonTerminals.add(grammarRule.leftChild);
            nonPosSet.add(grammarRule.leftChild);

            if (grammarRule instanceof BinaryStringRule) {
                final BinaryStringRule bsr = (BinaryStringRule) grammarRule;

                nonTerminals.add(bsr.rightChild);

                nonPosSet.add(bsr.rightChild);
                leftChildrenSet.add(bsr.leftChild);
                rightChildrenSet.add(bsr.rightChild);
            }
        }

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and
        // dummy non-terminals). Label the null symbol as a POS. I'm not sure that's right, but it seems to
        // work.
        nonTerminals.add(startSymbolStr);
        nonPosSet.add(startSymbolStr);
        nonTerminals.add(nullSymbolStr);
        pos.add(nullSymbolStr);

        // Make the POS set disjoint from the other sets.
        rightChildrenSet.removeAll(pos);
        leftChildrenSet.removeAll(pos);
        nonPosSet.removeAll(pos);

        // Add the NTs to `nonTermSet' in sorted order
        final NonTerminalComparator comparator = new PosEmbeddedComparator();
        final TreeSet<NonTerminal> sortedNonTerminals = new TreeSet<NonTerminal>(comparator);
        for (final String nt : nonTerminals) {
            sortedNonTerminals.add(create(nt, pos, nonPosSet, rightChildrenSet));
        }

        for (final NonTerminal nt : sortedNonTerminals) {
            nonTermSet.addSymbol(nt.label);
        }

        // TODO Generalize these further for right-factored grammars

        // Initialize indices
        final int[] startAndEndIndices = comparator.startAndEndIndices(nonPosSet, leftChildrenSet,
            rightChildrenSet, pos);
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
        lexicalProductions = new LinkedList<Production>();

        for (final StringRule lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild);
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex,
                lexicalRule.probability, true));
        }
        numLexProds = lexicalProductions.size();
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);

        // And unary and binary rules
        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();

        for (final StringRule grammarRule : grammarRules) {
            if (grammarRule instanceof BinaryStringRule) {
                binaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild,
                    ((BinaryStringRule) grammarRule).rightChild, grammarRule.probability));
            } else {
                unaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild,
                    grammarRule.probability, false));
            }
        }

        numUnaryProds = unaryProductions.size();
        unaryProdsByChild = storeProductionByChild(unaryProductions);

        internMap = null; // We no longer need the String intern map, so let it be GC'd
    }

    private List<StringRule> readLexProds(final Reader lexFile) throws IOException {

        final List<StringRule> rules = new LinkedList<StringRule>();
        final BufferedReader br = new BufferedReader(lexFile);

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] tokens = line.split("\\s");
            if (tokens.length == 4) {
                // expecting: A -> B prob
                rules.add(new StringRule(tokens[0], tokens[2], Float.valueOf(tokens[3])));
            } else {
                throw new IllegalArgumentException("Unexpected line in lexical file\n\t" + line);
            }
        }
        return rules;
    }

    private List<StringRule> readGrammar(final Reader gramFile, final GrammarFormatType grammarFormat)
            throws IOException {

        if (grammarFormat == GrammarFormatType.Roark) {
            startSymbolStr = "TOP";
        }

        final List<StringRule> rules = new LinkedList<StringRule>();
        final BufferedReader br = new BufferedReader(gramFile);

        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final String[] tokens = line.split("\\s");
            if (tokens.length == 1) {

                if (startSymbolStr != null) {
                    throw new IllegalArgumentException(
                        "Grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: "
                                + line);
                }

                startSymbolStr = tokens[0];

            } else if (tokens.length == 4) {
                // expecting: A -> B prob
                // should we make sure there aren't any duplicates?
                rules.add(new StringRule(tokens[0], tokens[2], Float.valueOf(tokens[3])));

            } else if (tokens.length == 5) {
                // expecting: A -> B C prob
                rules.add(new BinaryStringRule(tokens[0], tokens[2], tokens[3], Float.valueOf(tokens[4])));

            } else {
                throw new IllegalArgumentException("Unexpected line in grammar file\n\t" + line);
            }
        }

        if (startSymbolStr == null) {
            throw new IllegalArgumentException(
                "No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        return rules;
    }

    public final int numBinaryRules() {
        return binaryProductions.size();
    }

    public final int numUnaryRules() {
        return unaryProductions.size();
    }

    public final boolean isPos(final int nonTerminal) {
        return nonTerminal >= posStart && nonTerminal <= posEnd;
    }

    /**
     * Returns true if the non-terminal occurs as a left child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a left child in the grammar.
     */
    public boolean isValidRightChild(final int nonTerminal) {
        return nonTerminal >= rightChildrenStart && nonTerminal <= rightChildrenEnd;
    }

    /**
     * Returns true if the non-terminal occurs as a right child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a right child in the grammar.
     */
    public boolean isValidLeftChild(final int nonTerminal) {
        return nonTerminal >= leftChildrenStart && nonTerminal <= leftChildrenEnd;
    }

    @Override
    public String getStats() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + binaryProductions.size() + '\n');
        sb.append("Unary rules: " + unaryProductions.size() + '\n');
        sb.append("Lexical rules: " + numLexProds() + '\n');

        sb.append("Non Terminals: " + nonTermSet.size() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + numPosSymbols() + '\n');
        sb.append("Max POS index: " + maxPOSIndex + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');

        return sb.toString();
    }

    private String intern(final String s) {
        final String internedString = internMap.get(s);
        if (internedString != null) {
            return internedString;
        }
        internMap.put(s, s);
        return s;
    }

    private class StringRule {
        public final String parent;
        public final String leftChild;
        public final float probability;

        public StringRule(final String parent, final String leftChild, final float probability) {
            this.parent = intern(parent);
            this.leftChild = intern(leftChild);
            this.probability = probability;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%.3f)", parent, leftChild, probability);
        }
    }

    public final class BinaryStringRule extends StringRule {
        public final String rightChild;

        public BinaryStringRule(final String parent, final String leftChild, final String rightChild,
                final float probability) {
            super(parent, leftChild, probability);
            this.rightChild = intern(rightChild);
        }

        @Override
        public String toString() {
            return String.format("%s -> %s %s (%.3f)", parent, leftChild, rightChild, probability);
        }
    }

    public NonTerminal create(final String label, final HashSet<String> pos, final Set<String> nonPosSet,
            final Set<String> rightChildren) {
        final String internLabel = intern(label);

        if (pos.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.POS);

        } else if (nonPosSet.contains(internLabel) && !rightChildren.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY);
        }

        return new NonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);
    }

    private final class NonTerminal {
        public final String label;
        public final NonTerminalClass ntClass;

        protected NonTerminal(final String label, final NonTerminalClass ntClass) {
            this.label = label;
            this.ntClass = ntClass;
        }

        @Override
        public String toString() {
            return label + " " + ntClass.toString();
        }
    }

    public abstract static class NonTerminalComparator implements Comparator<NonTerminal> {
        HashMap<NonTerminalClass, Integer> map = new HashMap<NonTerminalClass, Integer>();

        @Override
        public int compare(final NonTerminal o1, final NonTerminal o2) {
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
         * @return an array containing leftChildStart, leftChildEnd, rightChildStart, rightChildEnd, posStart,
         *         posEnd, parentEnd
         */
        public abstract int[] startAndEndIndices(HashSet<?> nonPosSet, HashSet<?> leftChildrenSet,
                HashSet<?> rightChildrenSet, HashSet<?> posSet);
    }

    public static class PosLastComparator extends NonTerminalComparator {

        public PosLastComparator() {
            map.put(NonTerminalClass.FACTORED_SIDE_CHILDREN_ONLY, 0);
            map.put(NonTerminalClass.EITHER_CHILD, 1);
            map.put(NonTerminalClass.POS, 2);
        }

        @Override
        public int[] startAndEndIndices(final HashSet<?> nonPosSet, final HashSet<?> leftChildrenSet,
                final HashSet<?> rightChildrenSet, final HashSet<?> posSet) {

            final int total = nonPosSet.size() + posSet.size();

            final int leftChildrenStart = 0;
            final int leftChildrenEnd = total - 1;

            final int rightChildrenStart = leftChildrenEnd - rightChildrenSet.size() + 1;
            final int rightChildrenEnd = total - 1;

            final int posStart = rightChildrenEnd + 1;
            final int posEnd = total - 1;

            final int parentEnd = nonPosSet.size() - 1;

            return new int[] { leftChildrenStart, leftChildrenEnd, rightChildrenStart, rightChildrenEnd,
                    posStart, posEnd, parentEnd };
        }
    }

    public static class PosEmbeddedComparator extends NonTerminalComparator {
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

            return new int[] { leftChildrenStart, leftChildrenEnd, rightChildrenStart, rightChildrenEnd,
                    posStart, posEnd, parentEnd };
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
}
