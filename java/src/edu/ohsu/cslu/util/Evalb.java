package edu.ohsu.cslu.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Argument;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.GrammarFormatType;

/**
 * A simple Java implementation of Collins evalb. Usable as a standalone command-line application or programmatically
 * during parsing or grammar learning.
 */
public class Evalb extends BaseCommandlineTool {

    @Argument(index = 0, required = true, usage = "Gold tree file")
    File goldTrees;

    @Option(name = "-f", metaVar = "format", usage = "Grammar format")
    private GrammarFormatType grammarFormatType = GrammarFormatType.Berkeley;

    @Override
    protected void run() throws Exception {

        final EvalbResult result = eval(new FileReader(goldTrees), inputAsBufferedReader(), grammarFormatType,
                BaseLogger.singleton().isLoggable(Level.FINE));

        System.out.format("LP: %.2f LR: %.2f F1: %.2f Exact: %.2f\n", result.precision() * 100, result.recall() * 100,
                result.f1() * 100, result.exactMatch());
    }

    public static void main(final String[] args) {
        run(args);
    }

    public static EvalbResult eval(final Reader goldTreeReader, final Reader parseTreeReader) throws IOException {
        return eval(goldTreeReader, parseTreeReader, null, false);
    }

    public static EvalbResult eval(final Reader goldTreeReader, final Reader parsedTreeReader,
            final GrammarFormatType grammarFormatType, final boolean verbose) throws IOException {

        final BufferedReader br1 = new BufferedReader(goldTreeReader);
        final BufferedReader br2 = new BufferedReader(parsedTreeReader);

        final BracketEvaluator evaluator = new BracketEvaluator();

        int n = 1;
        for (String goldLine = br1.readLine(); goldLine != null; goldLine = br1.readLine()) {
            final String parsedLine = br2.readLine();

            final NaryTree<String> goldTree = NaryTree.read(goldLine, String.class);

            // Skip empty trees (Note: BracketEvaluator can accumulate the negative score impact of empty and null
            // parses, but we skip them in the command-line tool to match the behavior of evalb)
            if (parsedLine.equals("()") || parsedLine.equals("")) {
                continue;
            }

            NaryTree<String> parsedTree = NaryTree.read(parsedLine, String.class);
            if (parsedTree.isBinaryTree()) {
                parsedTree = BinaryTree.read(parsedLine, String.class).unfactor(grammarFormatType);
            }

            try {
                final EvalbResult result = evaluator.evaluate(goldTree, parsedTree);
                if (verbose) {
                    // TODO Populate length
                    // ID, Length, Recall, Precision, Matched Brackets, Gold Brackets, Parse Brackets
                    System.out
                            .format("%4d%5d%7.2f%7.2f%7d%7d%7d\n", n, 0, result.recall() * 100,
                                    result.precision() * 100, result.matchedBrackets, result.goldBrackets,
                                    result.parseBrackets);
                }
            } catch (final Exception e) {
                System.err.println("Exception " + e.getMessage() + " caused by " + parsedTree.toString());
            }
            n++;
        }

        return evaluator.accumulatedResult();
    }

    /**
     * Java implementation of bracket evaluation. Used in the {@link Evalb} command-line tool, and embedded directly
     * into BUBS for on-the-fly evaluation when testing with gold-standard trees.
     * 
     * Configurable using an implementation of {@link EvalbConfig}.
     * 
     * @author Aaron Dunlop
     */
    public static class BracketEvaluator {

        /**
         * Alternate configuration (as implemented in evalb using .prm files). Allows customization when evaluating
         * non-English / WSJ trees.
         */
        private final EvalbConfig config;

        /** Only consider phrase-level labels. Ignore nodes of height <= 2 (terminals and pre-terminals) */
        private final int minHeight = 3;

        /** Accumulated result over all individual trees */
        EvalbResult accumResult = new EvalbResult();

        public BracketEvaluator(final EvalbConfig config) {
            this.config = config;
        }

        public BracketEvaluator() {
            this.config = new DefaultConfig();
        }

        /**
         * Evaluates the <code>parseTree</code> against the gold standard <code>goldTree</code>. Returns the single-tree
         * evaluation result, and adds that result to the total accumulated result for this {@link BracketEvaluator}
         * (that accumulated result is available via {@link #accumulatedResult()}).
         * 
         * @param goldTree
         * @param parseTree
         * @return The single-tree evaluation result
         */
        public EvalbResult evaluate(final Tree<String> goldTree, final Tree<String> parseTree) {

            final Map<String, Integer> goldBrackets = goldTree.removeAll(config.ignoredLabels(), config.ignoreDepth())
                    .labeledSpans(minHeight, config.ignoredLabels(), config.equivalentLabels());
            final int goldBracketCount = totalCount(goldBrackets);

            // Evaluate tagging accuracy
            final String[] goldPreterminalLabels = goldTree.preterminalLabels();
            EvalbResult treeEvalb;

            if (parseTree != null) {
                final String[] parsePreterminalLabels = parseTree.preterminalLabels();
                int matchedTags = 0;
                for (int i = 0; i < goldPreterminalLabels.length; i++) {
                    if (goldPreterminalLabels[i].equals(parsePreterminalLabels[i])) {
                        matchedTags++;
                    }
                }

                // Ensure all ignored pre-terminals in the gold tree are also properly labeled in the parse tree (so
                // that
                // when we remove them, the node indices will match).
                final Tree<String> parseTreeClone = parseTree.clone();
                int i = 0;
                for (final Tree<String> leaf : parseTreeClone.leafTraversal()) {
                    if (config.ignoredLabels().contains(goldPreterminalLabels[i])
                            || config.ignoredLabels().contains(leaf.parent().label())) {
                        leaf.parent().setLabel(goldPreterminalLabels[i]);
                    }
                    i++;
                }

                final Map<String, Integer> parseBrackets = parseTreeClone.removeAll(config.ignoredLabels(),
                        config.ignoreDepth())
                        .labeledSpans(minHeight, config.ignoredLabels(), config.equivalentLabels());
                final int parseBracketCount = totalCount(parseBrackets);

                final int matchedBrackets = intersectionSize(parseBrackets, goldBrackets);

                treeEvalb = new EvalbResult(matchedBrackets, goldBracketCount, parseBracketCount, matchedTags,
                        goldPreterminalLabels.length);
            } else {
                // Handle failed parses
                treeEvalb = new EvalbResult(0, goldBracketCount, 0, 0, goldPreterminalLabels.length);
            }

            accumResult.add(treeEvalb);
            return treeEvalb;
        }

        private int intersectionSize(final Map<String, Integer> s1, final Map<String, Integer> s2) {
            int matches = 0;
            for (final String key : s1.keySet()) {
                if (s2.containsKey(key)) {
                    matches += java.lang.Math.min(s1.get(key), s2.get(key));
                }
            }
            return matches;
        }

        private int totalCount(final Map<String, Integer> map) {
            int count = 0;
            for (final int value : map.values()) {
                count += value;
            }
            return count;
        }

        /**
         * @return The accumulated result of all evaluations performed with this {@link BracketEvaluator}.
         */
        public EvalbResult accumulatedResult() {
            return accumResult;
        }
    }

    /**
     * Represents the result of an evaluation run, including bracket counts and match counts. Computes summary scores,
     * including precision, recall, and F1 (the harmonic mean of P and R)
     */
    public static class EvalbResult {
        public int matchedBrackets;
        public int goldBrackets;
        public int parseBrackets;
        public int goldTags;
        public int matchedTags;
        public int exactMatches;
        public int numTrees;

        public EvalbResult() {
            this(0, 0, 0, 0, 0, 0, 0);
        }

        // EvalbResults for a single tree
        public EvalbResult(final int matchedBrackets, final int goldBrackets, final int parseBrackets,
                final int matchedTags, final int goldTags) {
            this(matchedBrackets, goldBrackets, parseBrackets, matchedTags, goldTags,
                    matchedBrackets == goldBrackets ? 1 : 0, 1);
        }

        protected EvalbResult(final int matchedBrackets, final int goldBrackets, final int parseBrackets,
                final int matchedTags, final int goldTags, final int exactMatches, final int numTrees) {
            this.matchedBrackets = matchedBrackets;
            this.goldBrackets = goldBrackets;
            this.parseBrackets = parseBrackets;
            this.matchedTags = matchedTags;
            this.goldTags = goldTags;
            this.exactMatches = exactMatches;
            this.numTrees = numTrees;
        }

        /**
         * @return F1, the harmonic mean of precision and recall
         */
        public double f1() {
            final double precision = precision();
            final double recall = recall();
            return precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
        }

        /**
         * @return Precision, the percentage of predicted brackets which were observed in the gold tree(s).
         */
        public double precision() {
            return parseBrackets == 0 ? 0 : matchedBrackets * 1.0 / parseBrackets;
        }

        /**
         * @return Precision, the percentage of gold brackets which were observed in the parse tree(s).
         */
        public double recall() {
            return goldBrackets == 0 ? 0 : matchedBrackets * 1.0 / goldBrackets;
        }

        /**
         * @return The percentage of parse trees which exactly matched the gold trees
         */
        public double exactMatch() {
            return numTrees == 0 ? 0 : exactMatches * 1.0 / numTrees;
        }

        /**
         * @return The percentage of preterminals which were correctly predicted.
         */
        public double taggingAccuracy() {
            return matchedTags == 0 ? 0 : matchedTags * 1.0 / goldTags;
        }

        /**
         * Adds another result to this instance
         * 
         * @param other
         */
        public void add(final EvalbResult other) {
            matchedBrackets += other.matchedBrackets;
            goldBrackets += other.goldBrackets;
            parseBrackets += other.parseBrackets;
            matchedTags += other.matchedTags;
            goldTags += other.goldTags;
            exactMatches += other.exactMatches;
            numTrees += other.numTrees;
        }

        @Override
        public String toString() {
            return String.format("%7.2f%7.2f%7.2f%7d%7d%7d\n", recall() * 100, precision() * 100, f1() * 100,
                    matchedBrackets, goldBrackets, parseBrackets);
        }
    }

    /**
     * Implements an alternate configuration, e.g., ignoring certain phrase-level labels, altering the minimum depth,
     * etc. (as configured via .prm files for Collins' <code>evalb</code>).
     */
    public static interface EvalbConfig {

        /**
         * @return The set of labels which should be ignored during evaluation (e.g. TOP, -NONE-, punctuatuation, as per
         *         collins.prm).
         */
        public Set<String> ignoredLabels();

        /**
         * @return The set of labels which should be considered as 'quote' labels (e.g. ``, '', POS, as per new.prm).
         */
        public Set<String> quoteLabels();

        /**
         * @return Depth of subtrees which should be ignored for labels matching {@link #ignoredLabels()}.
         */
        public int ignoreDepth();

        /**
         * @return A mapping of labels which should be considered as equivalent (e.g. ADVP, PRT as per collins.prm).
         */
        public Map<String, String> equivalentLabels();
    }

    /**
     * Default configuration, matching the defaults in Collins <code>evalb</code>.
     */
    public static class DefaultConfig implements EvalbConfig {

        @Override
        public Set<String> ignoredLabels() {
            return new HashSet<String>();
        }

        @Override
        public Set<String> quoteLabels() {
            return new HashSet<String>();
        }

        @Override
        public int ignoreDepth() {
            return 2;
        }

        @Override
        public Map<String, String> equivalentLabels() {
            return new HashMap<String, String>();
        }
    }

    /**
     * 'New' config from http://nlp.cs.nyu.edu/evalb/
     */
    public static class NewConfig implements EvalbConfig {

        private final Set<String> ignoredLabels;
        private final Set<String> quoteLabels;
        private final Map<String, String> equivalentLabels;

        public NewConfig() {
            this.ignoredLabels = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "TOP",
                    "ROOT", "ROOT_0", "-NONE-", ",", ":", "``", "''", ".", "POS" })));

            this.quoteLabels = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(new String[] { "``", "''",
                    "POS" })));

            final HashMap<String, String> tmpEquivalentLabels = new HashMap<String, String>();
            tmpEquivalentLabels.put("ADVP", "PRT");
            this.equivalentLabels = Collections.unmodifiableMap(tmpEquivalentLabels);
        }

        @Override
        public Set<String> ignoredLabels() {
            return ignoredLabels;
        }

        @Override
        public Set<String> quoteLabels() {
            return quoteLabels;
        }

        @Override
        public int ignoreDepth() {
            return 2;
        }

        @Override
        public Map<String, String> equivalentLabels() {
            return equivalentLabels;
        }
    }
}
