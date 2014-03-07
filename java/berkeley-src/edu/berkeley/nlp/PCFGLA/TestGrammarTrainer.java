/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

package edu.berkeley.nlp.PCFGLA;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import it.unimi.dsi.fastutil.floats.FloatArrayList;
import it.unimi.dsi.fastutil.floats.FloatList;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.cjunit.DetailedTest;
import org.junit.BeforeClass;
import org.junit.Test;

import cltool4j.BaseCommandlineTool;
import cltool4j.GlobalConfigProperties;
import cltool4j.ToolTestCase;
import edu.berkeley.nlp.util.Numberer;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.cellselector.CompleteClosureModel;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.tests.JUnit;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;

/**
 * Integration tests for {@link GrammarTrainer}
 * 
 * @author Aaron Dunlop
 */
public class TestGrammarTrainer extends ToolTestCase {

    private final static int CYCLES = 2;

    /**
     * <pre>
     *            top
     *             |
     *             a 
     *             |
     *       --------------
     *       |           |
     *       a           b
     *       |           |
     *    -------     --------
     *    |     |     |      |
     *    a     d     b      c
     *    |     |     |      |
     *  -----   f     d      f
     *  |   |         |
     *  c   c         f
     *  |   |
     *  e   e
     *  
     *  top -> a 1
     *  a -> a b 1/3
     *  a -> a d 1/3
     *  a -> c c 1/3
     *  c -> e 2/3
     *  c -> f 1/3
     *  b -> b c 1/2
     *  b -> d 1/2
     *  d -> f 1
     * </pre>
     */
    public final static String STRING_SAMPLE_TREE = "(top (a (a (a (c e) (c e)) (d f)) (b (b (d f)) (c f))))";

    public final static String TREE_WITH_LONG_UNARY_CHAIN = "(TOP (S (NP (NP (RB Not) (PDT all) (DT those)) (SBAR (WHNP (WP who)) (S (VP (VBD wrote))))) (VP (VBP oppose) (NP (DT the) (NNS changes))) (. .)))";

    private static CompleteClosureModel ccModel;

    @BeforeClass
    public static void suiteSetUp() throws ClassNotFoundException, IOException {
        ccModel = new CompleteClosureModel(JUnit.unitTestDataAsStream("cellconstraints/wsj_cc.mdl.995"), null);
    }

    /**
     * Learns a 2-split grammar from a 1-sentence sample corpus.
     * 
     * @throws Exception
     */
    @Test
    public void testSample() throws Exception {
        trainGrammar(CYCLES, "-rs 1", STRING_SAMPLE_TREE, false);
    }

    /**
     * Learns a 2-split grammar from short 1-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentence1() throws Exception {
        trainGrammar(CYCLES, "-rs 1", "(ROOT (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))",
                false);
    }

    /**
     * Learns a 2-split grammar from short 1-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentence2() throws Exception {
        trainGrammar(CYCLES, "-rs 1", "(ROOT (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))", false);
    }

    /**
     * Learns a 2-split grammar from longer 1-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentence3() throws Exception {
        trainGrammar(
                CYCLES,
                "-rs 1",
                "(ROOT (S (S (CC But) (NP (NNS investors)) (VP (MD should) (VP (VB keep) (PP (IN in) (NP (NN mind))) (, ,) (PP (IN before) (S (VP (VBG paying) (ADVP (RB too) (JJ much))))) (, ,) (SBAR (IN that) (S (NP (NP (DT the) (JJ average) (JJ annual) (NN return)) (PP (IN for) (NP (NN stock) (NNS holdings)))) (, ,) (ADVP (JJ long-term)) (, ,) (VP (AUX is) (NP (NP (QP (CD 9) (NN %) (TO to) (CD 10) (NN %))) (NP (DT a) (NN year))))))))) (: ;) (S (NP (NP (DT a) (NN return)) (PP (IN of) (NP (CD 15) (NN %)))) (VP (AUX is) (VP (VBN considered) (S (ADJP (JJ praiseworthy)))))) (. .)))",
                false);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentence1Twice() throws Exception {
        final String sentence1 = "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))";
        trainGrammar(CYCLES, "-rs 1", sentence1 + '\n' + sentence1, false);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentence2Twice() throws Exception {
        final String sentence2 = "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))";
        trainGrammar(CYCLES, "-rs 1", sentence2 + '\n' + sentence2, false);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void testSentences1and2() throws Exception {
        trainGrammar(CYCLES, "-rs 1", "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))\n"
                + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))", false);
    }

    /**
     * Learns a 2-split grammar from 3-sentence 'corpus'.
     * 
     * @throws Exception
     */
    @Test
    public void test3Sentences() throws Exception {
        trainGrammar(
                CYCLES,
                "-rs 1",
                "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))\n"
                        + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))\n"
                        + "(TOP (S (S (CC But) (NP (NNS investors)) (VP (MD should) (VP (VB keep) (PP (IN in) (NP (NN mind))) (, ,) (PP (IN before) (S (VP (VBG paying) (ADVP (RB too) (JJ much))))) (, ,) (SBAR (IN that) (S (NP (NP (DT the) (JJ average) (JJ annual) (NN return)) (PP (IN for) (NP (NN stock) (NNS holdings)))) (, ,) (ADVP (JJ long-term)) (, ,) (VP (AUX is) (NP (NP (QP (CD 9) (NN %) (TO to) (CD 10) (NN %))) (NP (DT a) (NN year))))))))) (: ;) (S (NP (NP (DT a) (NN return)) (PP (IN of) (NP (CD 15) (NN %)))) (VP (AUX is) (VP (VBN considered) (S (ADJP (JJ praiseworthy)))))) (. .)))",
                false);

    }

    /**
     * Tests the default likelihood-based merge objective
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @DetailedTest
    public void testLikelihoodMergeObjective() throws Exception {
        testOnWsj24("");
    }

    /**
     * Tests the default likelihood-based merge objective
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @DetailedTest
    public void testTotalRuleCountMergeObjective() throws Exception {
        testOnWsj24("-mrf TotalRuleCount -O efficiencyLambda=.35");
    }

    /**
     * Tests the modeled merge objective ({@link ModeledMergeObjectiveFunction})
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @DetailedTest
    public void testModeledMergeObjective() throws Exception {
        testOnWsj24("-mrf modeled -O efficiencyLambda=.35 -O vPhraseCoefficient=-1.27 -O binaryRuleCoefficient=2.646e-3 -O unaryRuleCoefficient=-2.714e-2 -O medRowDensityCoefficient=-.9997 -O medColDensityCoefficient=-25.35");
    }

    /**
     * Tests the inference-informed ('discriminative') merge objective ({@link DiscriminativeMergeObjectiveFunction})
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @DetailedTest
    public void testInferenceInformedMergeObjective() throws Exception {
        testOnWsj24("-mrf discriminative -td -O efficiencyLambda=.35");
    }

    /**
     * Tests the sampling merge objective ({@link SamplingMergeObjective})
     * 
     * @throws Exception if something bad happens
     */
    @Test
    @DetailedTest
    public void testSampingMergeObjective() throws Exception {
        testOnWsj24("-mrf sampled -td -O sampleIterations=3 -O efficiencyLambda=.35");
    }

    /**
     * Learns a 2-cycle split-merge grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood
     * increases with successive EM iteration. Parses the training corpus and verifies that F-score increases after each
     * cycle. Parsing accuracy isn't guaranteed to increase, but in this test, we're evaluating on the training set, so
     * if parse accuracy declines, something's probably terribly wrong.
     * 
     * @throws IOException
     * @throws Exception
     */
    private void testOnWsj24(final String mergeObjectiveParam) throws IOException, Exception {
        final String trainingCorpus = JUnit.unitTestDataAsString("corpora/wsj/wsj.24.mrgNB.trees.gz");
        final byte[][] trainedGrammars = trainGrammar(CYCLES, "-rs 1 " + mergeObjectiveParam, trainingCorpus, true);

        // Parse the training corpus with each trained grammar (split-merge cycles 1, 2, and 3) and report F-score.
        double previousFScore = 0;
        for (int i = 0; i < trainedGrammars.length; i++) {
            System.out.format("Parsing with cycle %d grammar\n", i + 1);

            previousFScore = verifyFscoreIncrease(new LeftCscSparseMatrixGrammar(new InputStreamReader(
                    new GZIPInputStream(new ByteArrayInputStream(trainedGrammars[i]))),
                    new DecisionTreeTokenClassifier()), i / 3 + 1, previousFScore, trainingCorpus);
        }
    }

    /**
     * Parses the training corpus with the new grammar, reports F-score, and verifies that it's increasing
     * 
     * @throws IOException
     */
    private double verifyFscoreIncrease(final Grammar currentGrammar, final int cycle, final double previousFScore,
            final String inputTrees) throws IOException {

        // Initialize the grammar and parser
        final long t0 = System.currentTimeMillis();
        final LeftCscSparseMatrixGrammar cscGrammar = new LeftCscSparseMatrixGrammar(currentGrammar);
        final ParserDriver opts = new ParserDriver();
        opts.fomModel = new InsideProb();
        opts.cellSelectorModel = ccModel;

        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAX_BEAM_WIDTH, "40");
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_LEXICAL_ROW_BEAM_WIDTH, "40");
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_LEXICAL_ROW_UNARIES, "10");

        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, cscGrammar);

        final BracketEvaluator evaluator = new BracketEvaluator();
        final BufferedReader br = new BufferedReader(new StringReader(inputTrees));

        // Parse all input trees and evaluate F-score
        int tree = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            final ParseTask context = parser.parseSentence(goldTree.toString());

            if (context.binaryParse != null) {
                evaluator.evaluate(goldTree, context.binaryParse.unfactor(cscGrammar.grammarFormat));
            }
            BaseCommandlineTool.progressBar(25, 500, tree++);
        }

        // Evaluate the result and compare to the best previous score
        final EvalbResult evalbResult = evaluator.accumulatedResult();
        final double fScore = evalbResult.f1();
        System.out.format("F-score: %.3f  Parse Time: %.1f seconds\n", fScore * 100,
                (System.currentTimeMillis() - t0) / 1000f);
        assertTrue("Expected f-score to increase on cycle " + cycle, fScore >= previousFScore);
        return fScore;
    }

    /**
     * Executes {@link GrammarTrainer} with the specified command-line options. Returns the final grammar produced.
     * 
     * @param commandLineOptions Command-line options
     * @param input Input as a String
     * @param teeToStdout
     * @return All trained grammars, in String form
     * 
     * @throws Exception if grammar training fails
     */
    private byte[][] trainGrammar(final int cycles, final String commandLineOptions, final String input,
            final boolean teeToStdout) throws Exception {

        // EM guarantees that likelihood will always increase, but rule pruning may break that guarantee, so allow a
        // small epsilon.
        final float EPSILON = 25f;

        final GrammarTrainer gt = new GrammarTrainer();
        gt.ccModel = ccModel;

        // Initialize output streams for each grammar we'll train
        gt.outputGrammarStreams = new OutputStream[cycles];
        for (int i = 0; i < gt.outputGrammarStreams.length; i++) {
            gt.outputGrammarStreams[i] = new ByteArrayOutputStream();
        }
        final String output = executeTool(gt, "-cycles " + cycles + " " + commandLineOptions
                + " -gd <null> -writeIntermediateGrammars", input, teeToStdout);

        // Cleanup the static tag mapping (TODO Get rid of static mappings entirely)
        Numberer.clearGlobalNumberer("tags");

        final List<FloatList[]> results = trainingSetLikelihood(output);

        //
        // We expect the likelihood to increase on every EM iteration
        //
        assertFalse("Empty results", results.isEmpty());

        for (int i = 0; i < cycles; i++) {
            final FloatList[] cycle = results.get(i);

            // Note - don't check the smoothing step, since likelihoods will often decline then
            for (int j = 0; j < 2; j++) {
                final FloatList likelihoods = cycle[j];
                float previousLikelihood = likelihoods.get(0);

                for (int k = 1; k < likelihoods.size(); k++) {
                    final float ll = likelihoods.getFloat(k);
                    assertTrue(String.format(
                            "Unexpected likelihood decline from %.3f to %.3f on cycle %d, iteration %d",
                            previousLikelihood, ll, i + 1, k + 1), ll + EPSILON >= previousLikelihood);
                    previousLikelihood = ll;
                }
            }
        }

        final byte[][] grammars = new byte[gt.outputGrammarStreams.length][];
        for (int i = 0; i < gt.outputGrammarStreams.length; i++) {
            grammars[i] = ((ByteArrayOutputStream) gt.outputGrammarStreams[i]).toByteArray();
        }
        return grammars;
    }

    /**
     * Parses output from {@link GrammarTrainer} and returns the training-set likelihoods reported
     * 
     * @param output Output from a {@link GrammarTrainer} run
     * @return Training-set likelihoods - indexed by cycle, [split,merge,smoothing (0,1,2)], and iteration
     * @throws IOException
     */
    private List<FloatList[]> trainingSetLikelihood(final String output) throws IOException {

        final List<FloatList[]> results = new ArrayList<FloatList[]>();

        final BufferedReader br = new BufferedReader(new StringReader(output));
        int cycle = 1, previousIteration = 0;
        int splitMergeSmoothing = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            // End of a cycle?
            if (line.startsWith("Completed training cycle")) {
                splitMergeSmoothing = 0;
                previousIteration = 0;
                cycle++;
                continue;
            }

            // We only care about lines reporting an iteration
            if (!(line.startsWith("Iteration:") && line.contains("likelihood:"))) {
                continue;
            }

            if (results.size() < cycle) {
                final FloatList[] newCycle = new FloatList[3];
                for (int i = 0; i < 3; i++) {
                    newCycle[i] = new FloatArrayList();
                }
                results.add(newCycle);
            }
            final FloatList[] currentCycle = results.get(cycle - 1);

            final String[] split = line.split("\\s+");
            final int iteration = Integer.parseInt(split[1]);

            // Starting a new split/merge/smoothing run?
            if (iteration < previousIteration) {
                splitMergeSmoothing++;
            }
            previousIteration = iteration;

            final float ll = Float.parseFloat(split[5]);
            currentCycle[splitMergeSmoothing].add(ll);
        }

        return results;
    }
}
