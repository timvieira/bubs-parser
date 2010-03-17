package edu.ohsu.cslu.grammar;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.util.Log;

public abstract class SortedGrammar extends GrammarByChild {
    public String startSymbolStr = null;

    public int rightChildOnlyStart;
    // posStart should equal eitherChildStart
    public int eitherChildStart;
    public int posStart;
    public int leftChildOnlyStart;
    public int unaryChildOnlyStart;

    protected SortedGrammar(final String grammarFile, final String lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    @SuppressWarnings("unchecked")
    protected SortedGrammar(final Reader grammarFile, final Reader lexiconFile, final GrammarFormatType grammarFormat) throws Exception {
        rightChildOnlyStart = 0;
        eitherChildStart = leftChildOnlyStart = unaryChildOnlyStart = posStart = -1;

        final HashSet<String> nonTerminals = new HashSet<String>();
        final HashSet<String> pos = new HashSet<String>();

        // Read in the lexical productions first. Label any non-terminals found in the lexicon as POS tags. We assume POS will only occur as parents in span-1 rows and as children
        // in span-2 rows
        Log.info(1, "INFO: Reading lexical productions");
        final List<StringRule> lexicalRules = readLexProds(lexiconFile);
        for (final StringRule lexicalRule : lexicalRules) {
            nonTerminals.add(lexicalRule.parent);
            pos.add(lexicalRule.parent);
        }

        // Now read in the grammar file.
        Log.info(1, "INFO: Reading grammar");
        final List<StringRule> grammarRules = readGrammar(grammarFile, grammarFormat);

        // Track which non-terminals occur as left children, which as right children, and which as children only of unary rules
        final HashSet<String> leftChildren = new HashSet<String>();
        final HashSet<String> rightChildren = new HashSet<String>();
        final HashSet<String> unaryOnly = new HashSet<String>();

        for (final StringRule grammarRule : grammarRules) {
            nonTerminals.add(grammarRule.parent);
            nonTerminals.add(grammarRule.leftChild);

            if (grammarRule instanceof BinaryStringRule) {
                final BinaryStringRule bsr = (BinaryStringRule) grammarRule;
                if (!pos.contains(bsr.leftChild)) {
                    leftChildren.add(bsr.leftChild);
                }
                if (!pos.contains(bsr.rightChild)) {
                    rightChildren.add(bsr.rightChild);
                }
                nonTerminals.add(bsr.rightChild);
            } else {
                if (!pos.contains(grammarRule.leftChild)) {
                    unaryOnly.add(grammarRule.leftChild);
                }
            }
        }

        unaryOnly.removeAll(leftChildren);
        unaryOnly.removeAll(rightChildren);

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and dummy non-terminals)
        nonTerminals.add(startSymbolStr);
        unaryOnly.add(startSymbolStr);

        nonTerminals.add(nullSymbolStr);
        // TODO: What class should the null symbol fall into? POS for the moment.
        pos.add(nullSymbolStr);

        leftChildren.removeAll(unaryOnly);

        // Intersect the left and right children together to find the set of NTs which occur as either child
        final HashSet<String> bothChildren = (HashSet<String>) leftChildren.clone();
        bothChildren.retainAll(rightChildren);

        leftChildren.removeAll(bothChildren);
        rightChildren.removeAll(bothChildren);

        // System.out.format("Right children only: %d\n", rightChildren.size());
        // System.out.format("Both children: %d\n", bothChildren.size());
        // System.out.format("Left children only: %d\n", leftChildren.size());
        // System.out.format("Unary children only: %d\n", unaryOnly.size());
        // System.out.format("POS: %d\n", pos.size());

        // Now, sort the NTs by class (see NonTerminalClass).
        final TreeSet<NonTerminal> sortedNonTerminals = new TreeSet<NonTerminal>();
        for (final String nt : nonTerminals) {
            final NonTerminal n = create(nt, leftChildren, rightChildren, bothChildren, unaryOnly, pos);
            if (n != null) {
                sortedNonTerminals.add(n);
            }
        }

        // Map all NTs with shorts (limiting the total NT count to 32767, which is probably reasonable.
        // 6000 is the most in any grammar we're currently working with).
        // Store the indices of the NT class boundaries
        NonTerminalClass ntClass = null;
        for (final NonTerminal nonTerminal : sortedNonTerminals) {
            final int index = nonTermSet.addSymbol(nonTerminal.label);

            // Record class transitions
            if (nonTerminal.ntClass != ntClass) {
                switch (nonTerminal.ntClass) {
                    case RIGHT_CHILD_ONLY:
                        rightChildOnlyStart = index;
                        break;
                    case EITHER_CHILD:
                        eitherChildStart = index;
                        break;
                    case LEFT_CHILD_ONLY:
                        leftChildOnlyStart = index;
                        break;
                    case POS:
                        posStart = index;
                        break;
                    case UNARY_CHILD_ONLY:
                        unaryChildOnlyStart = index;
                        break;
                }
                ntClass = nonTerminal.ntClass;
            }

            if (nonTerminal.ntClass == NonTerminalClass.POS) {
                posSet.add(index);
            }
        }

        // If there are no NTs which occur as right children only, set the index to the beginning of the unary set
        if (leftChildOnlyStart == -1) {
            leftChildOnlyStart = unaryChildOnlyStart;
        }

        // If there are no NTs which occur as either child, set the index to the beginning of the left child only set
        if (eitherChildStart == -1) {
            eitherChildStart = leftChildOnlyStart;
        }

        maxPOSIndex = posStart + posSet.size() - 1;

        startSymbol = nonTermSet.addSymbol(startSymbolStr);
        nullSymbol = nonTermSet.addSymbol(nullSymbolStr);

        // Now that all NTs are mapped, we can create Production instances for all rules

        // Lexical rules first
        // final List<Production> tmpProdList = new LinkedList<Production>();
        // for (final StringRule lexicalRule : lexicalRules) {
        // tmpProdList.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexSet.getIndex(lexicalRule.leftChild), lexicalRule.probability, true));
        // }
        //
        // // store lexical prods indexed by the word
        // final ArrayList<LinkedList<Production>> tmpLexicalProds = new ArrayList<LinkedList<Production>>(lexSet.numSymbols());
        // for (int i = 0; i < lexSet.numSymbols(); i++) {
        // tmpLexicalProds.add(null);
        // }
        // // numLexProds = lexicalRules.size();
        //
        // for (final Production p : tmpProdList) {
        // LinkedList<Production> list = tmpLexicalProds.get(p.leftChild);
        //
        // if (list == null) {
        // list = new LinkedList<Production>();
        // tmpLexicalProds.set(p.leftChild, list);
        // }
        // list.add(p);
        // }
        //
        // lexicalProdsByChild = tmpLexicalProds.toArray(new LinkedList[tmpLexicalProds.size()]);

        lexicalProductions = new LinkedList<Production>();
        for (final StringRule lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild); // we don't care about the sorted order of the lexSet
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex, lexicalRule.probability, true));
        }

        // And unary and binary rules
        unaryProductions = new LinkedList<Production>();
        binaryProductions = new LinkedList<Production>();

        for (final StringRule grammarRule : grammarRules) {
            if (grammarRule instanceof BinaryStringRule) {
                binaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild, ((BinaryStringRule) grammarRule).rightChild, grammarRule.probability));
            } else {
                unaryProductions.add(new Production(grammarRule.parent, grammarRule.leftChild, grammarRule.probability, false));
            }
        }

        numUnaryProds = unaryProductions.size();
        unaryProdsByChild = storeProductionByChild(unaryProductions);
        // TODO: unaryProductions is used in inherited classes. We should change that so
        // we can delete this reference.
        // unaryProductions = null; // remove from memory since we now store by child

        numLexProds = lexicalProductions.size();
        lexicalProdsByChild = storeProductionByChild(lexicalProductions, lexSet.size() - 1);
        lexicalProductions = null; // remove from memory since we now store by child

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

    private List<StringRule> readGrammar(final Reader gramFile, final GrammarFormatType grammarFormat) throws IOException {

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
                            "Grammar file must contain a single line with a single string representing the START SYMBOL.\nMore than one entry was found.  Last line: " + line);
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
            throw new IllegalArgumentException("No start symbol found in grammar file.  Expecting a single non-terminal on the first line.");
        }

        return rules;
    }

    public final int numBinaryRules() {
        return binaryProductions.size();
    }

    public final int numUnaryRules() {
        return unaryProductions.size();
    }

    public final boolean isPos(final int child) {
        return child >= posStart && child <= maxPOSIndex;
    }

    public final boolean isValidRightChild(final int child) {
        return (child >= rightChildOnlyStart && child < leftChildOnlyStart && child != nullSymbol);
    }

    public final boolean isValidLeftChild(final int child) {
        return (child >= posStart && child < unaryChildOnlyStart && child != nullSymbol);
    }

    @Override
    public String getStats() {
        final StringBuilder sb = new StringBuilder(256);
        sb.append("Binary rules: " + binaryProductions.size() + '\n');
        sb.append("Unary rules: " + unaryProductions.size() + '\n');
        sb.append("Lexical rules: " + numLexProds() + '\n');

        sb.append("Non Terminals: " + nonTermSet.size() + '\n');
        sb.append("Lexical symbols: " + lexSet.size() + '\n');
        sb.append("POS symbols: " + posSet.size() + '\n');
        sb.append("Max POS index: " + maxPOSIndex + '\n');

        sb.append("Start symbol: " + nonTermSet.getSymbol(startSymbol) + '\n');
        sb.append("Null symbol: " + nonTermSet.getSymbol(nullSymbol) + '\n');

        return sb.toString();
    }

    private class StringRule {
        public final String parent;
        public final String leftChild;
        public final float probability;

        public StringRule(final String parent, final String leftChild, final float probability) {
            this.parent = parent.intern();
            this.leftChild = leftChild.intern();
            this.probability = probability;
        }

        @Override
        public String toString() {
            return String.format("%s -> %s (%.3f)", parent, leftChild, probability);
        }
    }

    public final class BinaryStringRule extends StringRule {
        public final String rightChild;

        public BinaryStringRule(final String parent, final String leftChild, final String rightChild, final float probability) {
            super(parent, leftChild, probability);
            this.rightChild = rightChild.intern();
        }

        @Override
        public String toString() {
            return String.format("%s -> %s %s (%.3f)", parent, leftChild, rightChild, probability);
        }
    }

    public NonTerminal create(final String label, final Set<String> leftChildrenOnly, final Set<String> rightChildrenOnly, final Set<String> bothChildren,
            final Set<String> unaryChildren, final HashSet<String> pos) {
        final String internLabel = label.intern();

        if (pos.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.POS);
        } else if (leftChildrenOnly.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.LEFT_CHILD_ONLY);
        } else if (rightChildrenOnly.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.RIGHT_CHILD_ONLY);
        } else if (bothChildren.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.EITHER_CHILD);
        } else if (unaryChildren.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.UNARY_CHILD_ONLY);
        }

        return null;
        // throw new IllegalArgumentException("Could not find " + label + " in any class");
    }

    private final class NonTerminal implements Comparable<NonTerminal> {
        public final String label;
        public final NonTerminalClass ntClass;

        protected NonTerminal(final String label, final NonTerminalClass ntClass) {
            this.label = label;
            this.ntClass = ntClass;
        }

        @Override
        public int compareTo(final NonTerminal o) {
            if (ntClass.ordinal() < o.ntClass.ordinal()) {
                return -1;
            } else if (ntClass.ordinal() > o.ntClass.ordinal()) {
                return 1;
            }
            return label.compareTo(o.label);
        }

        @Override
        public String toString() {
            return label + " " + ntClass.toString();
        }
    }

    /**
     * 1 - Right child only
     * 
     * 2 - Either child of binary rules
     * 
     * 3 - Left child only of binary rules
     * 
     * 4 - POS
     * 
     * 5 - Unary children only
     */
    private enum NonTerminalClass {
        RIGHT_CHILD_ONLY, POS, EITHER_CHILD, LEFT_CHILD_ONLY, UNARY_CHILD_ONLY;
    }
}
