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

    public int eitherChildStart;
    /** The first POS. */
    public int posStart;
    /** The first POS which cannot combine with a factored NT. */
    public int posNonFactoredStart;
    /**
     * The first POS which can combine with a factored NT. Should equal {@link #posStart} and
     * {@link #eitherChildStart}
     */
    public int normalPosStart;
    /** The first left-factored NT. Should equal {@link #leftChildOnlyStart} */
    public int leftFactoredStart;
    public int leftChildOnlyStart;
    public int normalLeftChildStart;

    public int unaryChildOnlyStart;

    /** The number of non-terminals which occur as the parent of a binary rule */
    private final int binaryParents;

    /**
     * The number of normal (non-POS, non-factored) non-terminals.
     */
    private final int normalNonTerminals;

    /**
     * The number of normal (non-POS, non-factored) non-terminals which occur only as left children of binary
     * productions.
     */
    private final int normalLeftChildrenOnly;

    /**
     * The number of normal (non-POS, non-factored) non-terminals which occur only as right children of binary
     * productions.
     */
    private final int normalRightChildrenOnly;

    /**
     * The number of normal (non-POS, non-factored) non-terminals which occur as either child of binary
     * productions.
     */
    private final int normalEitherChild;

    /** The number of non-terminals which only combine as the left child with POS. */
    private final int leftChildrenOnlyWithPos;

    /** The number of non-terminals which only combine as the right child with POS. */
    private final int rightChildrenOnlyWithPos;

    /** The number of non-terminals which combine as either child with POS. */
    private final int eitherChildWithPos;

    /** The number of normal non-terminals which only combine as the left child with POS. */
    private final int normalLeftChildrenOnlyWithPos;

    /** The number of normal non-terminals which only combine as the right child with POS. */
    private final int normalRightChildrenOnlyWithPos;

    /**
     * The number of non-factored non-terminals which can combine as the right child with a factored NT as the
     * left child.
     */
    private final int rightChildrenCombinedWithFactored;

    /** Factored non-terminals */
    private final int factoredNonTerminals;

    /** Factored non-terminals found as left children */
    private final int factoredLeftChildren;

    /** Factored non-terminals found as right children */
    private final int factoredRightChildren;

    /**
     * The number of non-factored non-terminals which can combine as the left child with a factored NT as the
     * right child.
     */
    private final int leftChildrenCombinedWithFactored;

    /** The number of normal non-terminals which only combine as the left child with factored NTs. */
    private final int normalLeftChildrenOnlyWithFactored;

    /** The number of normal non-terminals which only combine as the right child with factored NTs. */
    private final int normalRightChildrenOnlyWithFactored;

    /** POS which can combine with factored rules */
    private int posEitherChildWithFactored;

    /** The number of non-terminals which occur as the parent of a unary rule */
    private final int unaryParents;

    /** The number of non-terminals which only occur as the parent of a unary rule */
    private final int unaryParentsOnly;

    /** The number of non-terminals which occur as the parent of both unary and binary rules */
    private final int unaryAndBinaryParents;

    protected SortedGrammar(final String grammarFile, final String lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        this(new FileReader(grammarFile), new FileReader(lexiconFile), grammarFormat);
    }

    @SuppressWarnings("unchecked")
    protected SortedGrammar(final Reader grammarFile, final Reader lexiconFile,
            final GrammarFormatType grammarFormat) throws Exception {
        rightChildOnlyStart = 0;
        eitherChildStart = leftChildOnlyStart = unaryChildOnlyStart = posNonFactoredStart = normalPosStart = -1;

        final HashSet<String> nonTerminals = new HashSet<String>();
        final HashSet<String> pos = new HashSet<String>();

        // Read in the lexical productions first. Label any non-terminals found in the lexicon as POS tags. We
        // assume POS will only occur as parents in span-1 rows and as children
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

        // All non-terminals
        final HashSet<String> leftChildrenSet = new HashSet<String>();
        final HashSet<String> rightChildrenSet = new HashSet<String>();
        final HashSet<String> unaryChildrenSet = new HashSet<String>();

        // 'Normal' non-terminals (not POS or factored)
        final HashSet<String> normalNonTerminalSet = new HashSet<String>();
        final HashSet<String> normalLeftChildrenSet = new HashSet<String>();
        final HashSet<String> normalRightChildrenSet = new HashSet<String>();

        // Parents
        final HashSet<String> binaryParentSet = new HashSet<String>();
        final HashSet<String> unaryParentsSet = new HashSet<String>();

        // Factored non-terminals
        final HashSet<String> factoredNonTerminalSet = new HashSet<String>();
        final HashSet<String> factoredLeftChildrenSet = new HashSet<String>();
        final HashSet<String> factoredRightChildrenSet = new HashSet<String>();

        // Children combined with POS
        final HashSet<String> leftChildrenCombinedWithPosSet = new HashSet<String>();
        final HashSet<String> rightChildrenCombinedWithPosSet = new HashSet<String>();

        // Children combined with factored NTs
        final HashSet<String> rightChildrenCombinedWithFactoredSet = new HashSet<String>();
        final HashSet<String> leftChildrenCombinedWithFactoredSet = new HashSet<String>();

        // POS
        // final HashSet<String> posAsLeftChildSet = new HashSet<String>();
        // final HashSet<String> posAsRightChildSet = new HashSet<String>();

        for (final StringRule grammarRule : grammarRules) {

            nonTerminals.add(grammarRule.parent);
            nonTerminals.add(grammarRule.leftChild);

            final boolean parentIsFactored = grammarFormat.isFactored(grammarRule.parent);
            final boolean parentIsPos = pos.contains(grammarRule.parent);
            final boolean leftIsFactored = grammarFormat.isFactored(grammarRule.leftChild);
            final boolean leftIsPos = pos.contains(grammarRule.leftChild);

            if (!parentIsFactored && !parentIsPos) {
                normalNonTerminalSet.add(grammarRule.parent);
            }
            if (!leftIsFactored && !leftIsPos) {
                normalNonTerminalSet.add(grammarRule.leftChild);
            }

            if (leftIsFactored) {
                factoredNonTerminalSet.add(grammarRule.leftChild);
            }

            if (grammarRule instanceof BinaryStringRule) {
                final BinaryStringRule bsr = (BinaryStringRule) grammarRule;

                final boolean rightIsFactored = grammarFormat.isFactored(bsr.rightChild);
                final boolean rightIsPos = pos.contains(bsr.rightChild);

                nonTerminals.add(bsr.rightChild);
                binaryParentSet.add(bsr.parent);
                leftChildrenSet.add(bsr.leftChild);
                rightChildrenSet.add(bsr.rightChild);

                if (!leftIsPos && !leftIsFactored) {
                    normalLeftChildrenSet.add(bsr.leftChild);
                }

                if (!rightIsPos && !rightIsFactored) {
                    normalNonTerminalSet.add(bsr.rightChild);
                    normalRightChildrenSet.add(bsr.rightChild);
                }

                if (leftIsPos) {
                    rightChildrenCombinedWithPosSet.add(bsr.rightChild);
                }
                if (rightIsPos) {
                    leftChildrenCombinedWithPosSet.add(bsr.leftChild);
                }

                if (leftIsFactored) {
                    factoredLeftChildrenSet.add(bsr.leftChild);
                    rightChildrenCombinedWithFactoredSet.add(bsr.rightChild);
                }

                if (rightIsFactored) {
                    factoredRightChildrenSet.add(bsr.rightChild);
                    factoredNonTerminalSet.add(bsr.rightChild);
                    leftChildrenCombinedWithFactoredSet.add(bsr.leftChild);
                }

            } else {
                // Unary Rule
                unaryParentsSet.add(grammarRule.parent);
                unaryChildrenSet.add(grammarRule.leftChild);
            }
        }

        // Special cases for the start symbol and the null symbol (used for start/end of sentence markers and
        // dummy non-terminals)
        nonTerminals.add(startSymbolStr);
        nonTerminals.add(nullSymbolStr);

        // TODO: What class should the null symbol fall into? POS for the moment.
        pos.add(nullSymbolStr);

        binaryParents = binaryParentSet.size();
        normalNonTerminals = normalNonTerminalSet.size();

        // Unary and binary parents
        HashSet<String> tmp = (HashSet<String>) unaryParentsSet.clone();
        tmp.retainAll(binaryParentSet);
        unaryAndBinaryParents = tmp.size();

        // Unary parents
        unaryParents = unaryParentsSet.size();

        final HashSet<String> unaryParentsOnlySet = new HashSet<String>(unaryParentsSet);
        unaryParentsOnlySet.removeAll(binaryParentSet);
        unaryParentsOnly = unaryParentsOnlySet.size();

        // NTs which combine with on either side
        tmp = new HashSet<String>(leftChildrenCombinedWithPosSet);
        tmp.retainAll(rightChildrenCombinedWithPosSet);
        eitherChildWithPos = tmp.size();

        // NTs which combine with POS only on one side or the other
        final HashSet<String> leftChildrenOnlyWithPosSet = new HashSet<String>(leftChildrenCombinedWithPosSet);
        leftChildrenOnlyWithPosSet.removeAll(rightChildrenCombinedWithPosSet);
        leftChildrenOnlyWithPos = leftChildrenOnlyWithPosSet.size();

        final HashSet<String> rightChildrenOnlyWithPosSet = new HashSet<String>(
            rightChildrenCombinedWithPosSet);
        rightChildrenOnlyWithPosSet.removeAll(leftChildrenCombinedWithPosSet);
        rightChildrenOnlyWithPos = rightChildrenOnlyWithPosSet.size();

        // NTs which combine with factored NTs
        rightChildrenCombinedWithFactored = rightChildrenCombinedWithFactoredSet.size();
        leftChildrenCombinedWithFactored = leftChildrenCombinedWithFactoredSet.size();

        // Normal (non-POS, non-factored) left and right children
        tmp = new HashSet<String>(normalLeftChildrenSet);
        tmp.retainAll(normalRightChildrenSet);
        normalEitherChild = tmp.size();

        final HashSet<String> normalLeftChildrenOnlySet = new HashSet<String>(normalLeftChildrenSet);
        normalLeftChildrenOnlySet.removeAll(normalRightChildrenSet);
        normalLeftChildrenOnly = normalLeftChildrenOnlySet.size();

        final HashSet<String> normalRightChildrenOnlySet = new HashSet<String>(normalLeftChildrenSet);
        normalRightChildrenOnlySet.removeAll(normalLeftChildrenSet);
        normalRightChildrenOnly = normalRightChildrenOnlySet.size();

        // Normal children specifically with POS
        final HashSet<String> normalLeftChildrenOnlyWithPosSet = new HashSet<String>(normalLeftChildrenSet);
        normalLeftChildrenOnlyWithPosSet.retainAll(leftChildrenOnlyWithPosSet);
        normalLeftChildrenOnlyWithPos = normalLeftChildrenOnlyWithPosSet.size();

        final HashSet<String> normalRightChildrenOnlyWithPosSet = new HashSet<String>(normalRightChildrenSet);
        normalRightChildrenOnlyWithPosSet.retainAll(rightChildrenOnlyWithPosSet);
        normalRightChildrenOnlyWithPos = normalRightChildrenOnlyWithPosSet.size();

        // Factored NTs
        factoredNonTerminals = factoredNonTerminalSet.size();
        factoredLeftChildren = factoredLeftChildrenSet.size();
        factoredRightChildren = factoredRightChildrenSet.size();

        // Normal children specifically with factored
        final HashSet<String> normalLeftChildrenOnlyWithFactoredSet = new HashSet<String>(
            leftChildrenCombinedWithFactoredSet);
        normalLeftChildrenOnlyWithFactoredSet.retainAll(normalLeftChildrenSet);
        normalLeftChildrenOnlyWithFactored = normalLeftChildrenOnlyWithFactoredSet.size();

        final HashSet<String> normalRightChildrenOnlyWithFactoredSet = new HashSet<String>(
            rightChildrenCombinedWithFactoredSet);
        normalRightChildrenOnlyWithFactoredSet.retainAll(normalRightChildrenSet);
        normalRightChildrenOnlyWithFactored = normalRightChildrenOnlyWithFactoredSet.size();

        final HashSet<String> posWithFactoredSet = new HashSet<String>(rightChildrenCombinedWithFactoredSet);
        posWithFactoredSet.addAll(leftChildrenCombinedWithFactoredSet);
        posWithFactoredSet.retainAll(pos);

        // Count the number of non-terminals which occur both as parents of pre-terminal rules (POS) and as
        // the parents of other rules
        binaryParentSet.retainAll(pos);

        unaryParentsSet.retainAll(pos);

        // Intersect the left and right children together to find the set of NTs which occur as either child
        final HashSet<String> bothChildrenSet = new HashSet<String>(leftChildrenSet);
        bothChildrenSet.addAll(rightChildrenSet);

        final HashSet<String> leftChildrenOnlySet = new HashSet<String>(leftChildrenSet);
        leftChildrenOnlySet.removeAll(rightChildrenSet);
        final HashSet<String> rightChildrenOnlySet = new HashSet<String>(rightChildrenSet);
        rightChildrenOnlySet.removeAll(leftChildrenSet);

        final HashSet<String> unaryChildrenOnlySet = new HashSet<String>(unaryParentsSet);
        unaryChildrenOnlySet.removeAll(leftChildrenSet);
        unaryChildrenOnlySet.removeAll(rightChildrenSet);

        // Now, sort the NTs by class (see NonTerminalClass).
        final TreeSet<NonTerminal> sortedNonTerminals = new TreeSet<NonTerminal>();
        for (final String nt : nonTerminals) {
            final NonTerminal n = create(nt, pos, posWithFactoredSet, factoredLeftChildrenSet,
                leftChildrenOnlySet, rightChildrenOnlySet, bothChildrenSet, unaryChildrenOnlySet);
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
                    case LEFT_FACTORED:
                        leftFactoredStart = index;
                        leftChildOnlyStart = index;
                        break;
                    case NORMAL_LEFT_CHILD_ONLY:
                        normalLeftChildStart = index;
                        break;
                    case POS_NON_FACTORED:
                        posNonFactoredStart = index;
                        posStart = index;
                        normalPosStart = index;
                        break;
                    case NORMAL_POS:
                        normalPosStart = index;
                        break;
                    case UNARY_CHILD_ONLY:
                        unaryChildOnlyStart = index;
                        break;
                }
                ntClass = nonTerminal.ntClass;
            }

            if (nonTerminal.ntClass == NonTerminalClass.NORMAL_POS
                    || nonTerminal.ntClass == NonTerminalClass.POS_NON_FACTORED) {
                posSet.add(index);
            }
        }

        // If there are no NTs which occur as left children only, set the index to the beginning of the unary
        // set
        if (unaryChildOnlyStart == -1) {
            unaryChildOnlyStart = sortedNonTerminals.size();
        }

        // If there are no NTs which occur as left children only, set the index to the beginning of the unary
        // set
        if (leftChildOnlyStart == -1) {
            leftChildOnlyStart = unaryChildOnlyStart;
        }

        // If there are no NTs which occur as either child, set the index to the beginning of the left child
        // only set
        if (eitherChildStart == -1) {
            eitherChildStart = leftChildOnlyStart;
        }

        maxPOSIndex = posNonFactoredStart + posSet.size() - 1;

        startSymbol = nonTermSet.addSymbol(startSymbolStr);
        nullSymbol = nonTermSet.addSymbol(nullSymbolStr);

        // Now that all NTs are mapped, we can create Production instances for all rules

        lexicalProductions = new LinkedList<Production>();
        for (final StringRule lexicalRule : lexicalRules) {
            final int lexIndex = lexSet.addSymbol(lexicalRule.leftChild); // we don't care about the sorted
            // order of the lexSet
            lexicalProductions.add(new Production(nonTermSet.getIndex(lexicalRule.parent), lexIndex,
                lexicalRule.probability, true));
        }

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

    public final boolean isPos(final int child) {
        return child >= normalPosStart && child <= maxPOSIndex;
    }

    /**
     * Returns true if the non-terminal occurs as a left child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a left child in the grammar.
     */
    public boolean isValidRightChild(final int nonTerminal) {
        return (nonTerminal >= rightChildOnlyStart && nonTerminal < leftChildOnlyStart && nonTerminal != nullSymbol);
    }

    /**
     * Returns true if the non-terminal occurs as a right child in the grammar.
     * 
     * @param nonTerminal
     * @return true if the non-terminal occurs as a right child in the grammar.
     */
    public boolean isValidLeftChild(final int nonTerminal) {
        return (nonTerminal >= normalPosStart && nonTerminal < unaryChildOnlyStart && nonTerminal != nullSymbol);
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

        sb.append("Binary Parents: " + binaryParents + '\n');
        sb.append("Unary Parents: " + unaryParents + '\n');
        sb.append("Unary Parents Only: " + unaryParentsOnly + '\n');
        sb.append("Unary And Binary Parents: " + unaryAndBinaryParents + '\n');

        sb.append("Normal NTs: " + normalNonTerminals + '\n');
        sb.append("Normal NTs occurring on left side only of binary rule: " + normalLeftChildrenOnly + '\n');
        sb
            .append("Normal NTs occurring on right side only of binary rule: " + normalRightChildrenOnly
                    + '\n');
        sb.append("Normal NTs occurring on either side of binary rule: " + normalEitherChild + '\n');
        sb.append('\n');

        sb.append("NTs occurring on left side only with POS: " + leftChildrenOnlyWithPos + '\n');
        sb.append("NTs occurring on right side only with POS: " + rightChildrenOnlyWithPos + '\n');
        sb.append("NTs occurring on either side of binary rule with POS: " + eitherChildWithPos + '\n');
        sb.append('\n');

        sb.append("Normal NTs occurring on left side only with POS: " + normalLeftChildrenOnlyWithPos + '\n');
        sb.append("Normal NTs occurring on right side only with POS: " + normalRightChildrenOnlyWithPos
                + '\n');
        sb.append('\n');

        sb.append("NTs which combine as the right child with a factored NT: "
                + rightChildrenCombinedWithFactored + '\n');
        sb.append("NTs which combine as the left child with a factored NT: "
                + leftChildrenCombinedWithFactored + '\n');
        sb.append('\n');

        sb.append("Normal NTs occurring on left side only with a factored NT: "
                + normalLeftChildrenOnlyWithFactored + '\n');
        sb.append("Normal NTs occurring on right side only with a factired NT: "
                + normalRightChildrenOnlyWithFactored + '\n');
        sb.append('\n');

        sb.append("Factored non-terminals:: " + factoredNonTerminals + '\n');
        sb.append("Factored non-terminals as left children: " + factoredLeftChildren + '\n');
        sb.append("Factored non-terminals as right children: " + factoredRightChildren + '\n');

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

        public BinaryStringRule(final String parent, final String leftChild, final String rightChild,
                final float probability) {
            super(parent, leftChild, probability);
            this.rightChild = rightChild.intern();
        }

        @Override
        public String toString() {
            return String.format("%s -> %s %s (%.3f)", parent, leftChild, rightChild, probability);
        }
    }

    public NonTerminal create(final String label, final HashSet<String> pos,
            final HashSet<String> posWithFactoredSet, final Set<String> leftFactored,
            final Set<String> leftChildrenOnly, final Set<String> rightChildrenOnly,
            final Set<String> bothChildren, final Set<String> unaryChildren) {
        final String internLabel = label.intern();

        if (pos.contains(internLabel) && !posWithFactoredSet.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.POS_NON_FACTORED);

        } else if (pos.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.NORMAL_POS);

        } else if (leftFactored.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.LEFT_FACTORED);

        } else if (leftChildrenOnly.contains(internLabel)) {
            return new NonTerminal(internLabel, NonTerminalClass.NORMAL_LEFT_CHILD_ONLY);

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
     * 2 - POS which cannot combine with factored rules
     * 
     * 3 - All other POS
     * 
     * 4 - Either child of binary rules
     * 
     * 5 - Left-factored NTs (which only occur as the left child)
     * 
     * 6 - Other NTs which only occur as the left child
     * 
     * 7 - Unary children only
     */
    private enum NonTerminalClass {
        // TODO Add factored classes
        RIGHT_CHILD_ONLY, POS_NON_FACTORED, NORMAL_POS, EITHER_CHILD, LEFT_FACTORED, NORMAL_LEFT_CHILD_ONLY, UNARY_CHILD_ONLY;
    }
}
