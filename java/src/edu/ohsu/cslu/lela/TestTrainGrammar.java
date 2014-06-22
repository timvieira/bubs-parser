/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.lela;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.cjunit.DetailedTest;
import org.junit.Ignore;
import org.junit.Test;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.lela.FractionalCountGrammar.RandomNoiseGenerator;
import edu.ohsu.cslu.lela.TrainGrammar.EmIterationResult;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.fom.InsideProb;
import edu.ohsu.cslu.parser.ml.CartesianProductHashSpmlParser;
import edu.ohsu.cslu.tests.JUnit;
import edu.ohsu.cslu.util.Evalb.BracketEvaluator;
import edu.ohsu.cslu.util.Evalb.EvalbResult;
import edu.ohsu.cslu.util.Strings;

/**
 * Unified tests for training of a split-merge grammar.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 */
public class TestTrainGrammar {

    /**
     * Learns a 2-split grammar from a 1-sentence sample corpus. Verifies that likelihood increases with successive EM
     * runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSample() throws IOException {
        testEmTraining(new BufferedReader(new StringReader(AllLelaTests.STRING_SAMPLE_TREE)));
    }

    /**
     * Learns a 2-split grammar from short 1-sentence 'corpus'. Verifies that corpus likelihood increases with
     * successive EM runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentence1() throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(
                "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))"), 1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from short 1-sentence 'corpus'. Verifies that corpus likelihood increases with
     * successive EM runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentence2() throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(
                "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))"), 1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from longer 1-sentence 'corpus'. Verifies that corpus likelihood increases with
     * successive EM runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentence3() throws IOException {
        final BufferedReader br = new BufferedReader(
                new StringReader(
                        "(TOP (S (S (CC But) (NP (NNS investors)) (VP (MD should) (VP (VB keep) (PP (IN in) (NP (NN mind))) (, ,) (PP (IN before) (S (VP (VBG paying) (ADVP (RB too) (JJ much))))) (, ,) (SBAR (IN that) (S (NP (NP (DT the) (JJ average) (JJ annual) (NN return)) (PP (IN for) (NP (NN stock) (NNS holdings)))) (, ,) (ADVP (JJ long-term)) (, ,) (VP (AUX is) (NP (NP (QP (CD 9) (NN %) (TO to) (CD 10) (NN %))) (NP (DT a) (NN year))))))))) (: ;) (S (NP (NP (DT a) (NN return)) (PP (IN of) (NP (CD 15) (NN %)))) (VP (AUX is) (VP (VBN considered) (S (ADJP (JJ praiseworthy)))))) (. .)))"),
                1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'. Verifies that corpus likelihood increases with successive EM
     * runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentence1Twice() throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(
                "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))\n"
                        + "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))"), 1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'. Verifies that corpus likelihood increases with successive EM
     * runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentence2Twice() throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(
                "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))\n"
                        + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))"), 1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from 2-sentence 'corpus'. Verifies that corpus likelihood increases with successive EM
     * runs.
     * 
     * @throws IOException
     */
    @Test
    public void testSentences1and2() throws IOException {
        final BufferedReader br = new BufferedReader(new StringReader(
                "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))\n"
                        + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))"), 1024);

        testEmTraining(br);
    }

    /**
     * Learns a 2-split grammar from 3-sentence 'corpus'. Verifies that corpus likelihood increases with successive EM
     * runs.
     * 
     * @throws IOException
     */
    @Test
    public void test3Sentences() throws IOException {
        GlobalConfigProperties.singleton().setProperty(Parser.PROPERTY_MAX_BEAM_WIDTH, "50");
        final BufferedReader br = new BufferedReader(
                new StringReader(
                        "(TOP (S (NP (NNP FCC) (NN counsel)) (VP (VBZ joins) (NP (NN firm))) (: :)))\n"
                                + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))\n"
                                + "(TOP (S (S (CC But) (NP (NNS investors)) (VP (MD should) (VP (VB keep) (PP (IN in) (NP (NN mind))) (, ,) (PP (IN before) (S (VP (VBG paying) (ADVP (RB too) (JJ much))))) (, ,) (SBAR (IN that) (S (NP (NP (DT the) (JJ average) (JJ annual) (NN return)) (PP (IN for) (NP (NN stock) (NNS holdings)))) (, ,) (ADVP (JJ long-term)) (, ,) (VP (AUX is) (NP (NP (QP (CD 9) (NN %) (TO to) (CD 10) (NN %))) (NP (DT a) (NN year))))))))) (: ;) (S (NP (NP (DT a) (NN return)) (PP (IN of) (NP (CD 15) (NN %)))) (VP (AUX is) (VP (VBN considered) (S (ADJP (JJ praiseworthy)))))) (. .)))"),
                4096);

        testEmTraining(br);
    }

    private void testEmTraining(final BufferedReader trainingCorpusReader) throws IOException {
        final TrainGrammar tg = new TrainGrammar();
        tg.binarization = Binarization.LEFT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;

        trainingCorpusReader.mark(20 * 1024 * 1024);

        final StringCountGrammar scg = new StringCountGrammar(trainingCorpusReader, Binarization.LEFT,
                GrammarFormatType.Berkeley);
        final FractionalCountGrammar g0 = scg.toFractionalCountGrammar();
        trainingCorpusReader.reset();

        tg.corpusWordCounts = scg.wordCounts(g0.lexicon);
        tg.loadGoldTreesAndConstrainingCharts(trainingCorpusReader, g0);

        // Parse the training 'corpus' with induced grammar and report F-score
        final long t0 = System.currentTimeMillis();
        System.out.format("Initial F-score: %.3f  Time: %.1f seconds\n", parseFScore(cscGrammar(g0), tg.goldTrees),
                (System.currentTimeMillis() - t0) / 1000f);

        // Split and train with the 1-split grammar
        final FractionalCountGrammar split1 = g0.split(new RandomNoiseGenerator(0, .01f));
        // split1.randomize(new Random(), .01f);
        final FractionalCountGrammar g1 = runEm(tg, g0, split1, 1, 25, true, true);

        // Merge TOP_1 back into TOP, split again, and train with the new 2-split grammar
        final FractionalCountGrammar mergedG1 = g1.merge(new short[] { 1 });
        final FractionalCountGrammar split2 = mergedG1.split(new RandomNoiseGenerator(0, .01f));
        // split2.randomize(new Random(), .01f);
        runEm(tg, g0, split2, 2, 25, true, true);
    }

    /**
     * Learns a 3-split grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood increases with
     * successive EM iteration. Reports F-score after each split/EM run and verifies that those increase as well.
     * Parsing accuracy isn't guaranteed to increase, but in this test, we're evaluating on the training set, so if
     * parse accuracy declines, something's probably terribly wrong.
     * 
     * @throws IOException
     */
    @Test
    @DetailedTest
    @Ignore
    public void test3SplitWsj24WithoutMerging() throws IOException {

        final TrainGrammar tg = new TrainGrammar();
        tg.binarization = Binarization.LEFT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;
        tg.setup();

        final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"),
                20 * 1024 * 1024);
        br.mark(20 * 1024 * 1024);

        final StringCountGrammar scg = new StringCountGrammar(br, Binarization.LEFT, GrammarFormatType.Berkeley);
        final FractionalCountGrammar g0 = scg.toFractionalCountGrammar();
        br.reset();

        tg.loadGoldTreesAndConstrainingCharts(br, g0);
        tg.corpusWordCounts = scg.wordCounts(g0.lexicon);

        // Parse the training 'corpus' with induced grammar and report F-score
        final long t0 = System.currentTimeMillis();
        double previousFScore = parseFScore(cscGrammar(g0), tg.goldTrees);
        System.out.format("Initial F-score: %.3f  Parse Time: %.1f seconds\n", previousFScore * 100,
                (System.currentTimeMillis() - t0) / 1000f);

        final RandomNoiseGenerator noiseGenerator = new RandomNoiseGenerator(0, .01f);
        // Split and train with the 1-split grammar
        final FractionalCountGrammar split1 = g0.split(noiseGenerator);
        final FractionalCountGrammar g1 = runEm(tg, g0, split1, 1, 50, false, false);
        previousFScore = verifyFscoreIncrease(tg, g1, previousFScore);

        // Merge TOP_1 back into TOP, split again, and train with the new 2-split grammar
        final FractionalCountGrammar mergedG1 = g1.merge(new short[] { 1 });
        final FractionalCountGrammar split2 = mergedG1.split(noiseGenerator);
        final FractionalCountGrammar g2 = runEm(tg, g0, split2, 2, 50, false, false);
        previousFScore = verifyFscoreIncrease(tg, g2, previousFScore);

        // Merge TOP_1 back into TOP, split again, and train with the new 3-split grammar
        final FractionalCountGrammar mergedG2 = g2.merge(new short[] { 1 });
        final FractionalCountGrammar split3 = mergedG2.split(noiseGenerator);
        final FractionalCountGrammar g3 = runEm(tg, g0, split3, 3, 50, false, false);
        verifyFscoreIncrease(tg, g3, previousFScore);
    }

    /**
     * Learns a 3-split grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood increases with
     * successive EM iteration. Reports F-score after each split/EM run and verifies that those increase as well.
     * Parsing accuracy isn't guaranteed to increase, but in this test, we're evaluating on the training set, so if
     * parse accuracy declines, something's probably terribly wrong.
     * 
     * @throws IOException
     */
    @Test
    // This method is mostly for profiling
    @DetailedTest
    public void test3SplitWsj24WithMergingWithoutParse() throws IOException {

        final TrainGrammar tg = new TrainGrammar();
        tg.binarization = Binarization.LEFT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;
        tg.mergeFraction = 0.5f;
        tg.setup();

        final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"),
                20 * 1024 * 1024);
        br.mark(20 * 1024 * 1024);

        final StringCountGrammar scg = new StringCountGrammar(br, Binarization.LEFT, GrammarFormatType.Berkeley);
        final FractionalCountGrammar g0 = scg.toFractionalCountGrammar();
        br.reset();

        tg.loadGoldTreesAndConstrainingCharts(br, g0);
        tg.corpusWordCounts = scg.wordCounts(g0.lexicon);

        // Split and train with the 1-split grammar
        final RandomNoiseGenerator noiseGenerator = new RandomNoiseGenerator(0, .01f);
        final FractionalCountGrammar split1 = g0.split(noiseGenerator);
        final FractionalCountGrammar g1 = runEm(tg, g0, split1, 1, 50, false, false);

        // Re-merge half the non-terminals, split again, and train with the new 2-split grammar
        final FractionalCountGrammar mergedG1 = tg.merge(g1);
        final FractionalCountGrammar split2 = mergedG1.split(noiseGenerator);
        final FractionalCountGrammar g2 = runEm(tg, g0, split2, 2, 50, false, false);

        // Re-merge half the non-terminals , split again, and train with the new 3-split grammar
        final FractionalCountGrammar mergedG2 = tg.merge(g2);
        final FractionalCountGrammar split3 = mergedG2.split(noiseGenerator);
        runEm(tg, g0, split3, 3, 50, false, false);
    }

    /**
     * Learns a 3-split grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood increases with
     * successive EM iteration. Reports F-score after each split/EM run and verifies that those increase as well.
     * Parsing accuracy isn't guaranteed to increase, but in this test, we're evaluating on the training set, so if
     * parse accuracy declines, something's probably terribly wrong.
     * 
     * @throws IOException
     */
    @Test
    @DetailedTest
    @Ignore
    public void test3SplitWsj24WithMerging() throws IOException {

        final TrainGrammar tg = new TrainGrammar();
        tg.binarization = Binarization.LEFT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;
        tg.mergeFraction = 0.5f;
        tg.setup();

        final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"),
                20 * 1024 * 1024);
        br.mark(20 * 1024 * 1024);

        final StringCountGrammar scg = new StringCountGrammar(br, Binarization.LEFT, GrammarFormatType.Berkeley);
        final FractionalCountGrammar g0 = scg.toFractionalCountGrammar();
        br.reset();

        tg.loadGoldTreesAndConstrainingCharts(br, g0);
        tg.corpusWordCounts = scg.wordCounts(g0.lexicon);

        // Parse the training 'corpus' with induced grammar and report F-score
        final long t0 = System.currentTimeMillis();
        double previousFScore = parseFScore(cscGrammar(g0), tg.goldTrees);
        System.out.format("Initial F-score: %.3f  Parse Time: %.1f seconds\n", previousFScore * 100,
                (System.currentTimeMillis() - t0) / 1000f);

        // Split and train with the 1-split grammar
        final RandomNoiseGenerator noiseGenerator = new RandomNoiseGenerator(0, .01f);
        final FractionalCountGrammar split1 = g0.split(noiseGenerator);
        final FractionalCountGrammar g1 = runEm(tg, g0, split1, 1, 50, false, false);
        previousFScore = verifyFscoreIncrease(tg, g1, previousFScore);

        // Re-merge half the non-terminals, split again, and train with the new 2-split grammar
        final FractionalCountGrammar mergedG1 = tg.merge(g1);
        final FractionalCountGrammar split2 = mergedG1.split(noiseGenerator);
        final FractionalCountGrammar g2 = runEm(tg, g0, split2, 2, 50, false, false);
        previousFScore = verifyFscoreIncrease(tg, g2, previousFScore);

        // Re-merge half the non-terminals , split again, and train with the new 3-split grammar
        final FractionalCountGrammar mergedG2 = tg.merge(g2);
        final FractionalCountGrammar split3 = mergedG2.split(noiseGenerator);
        final FractionalCountGrammar g3 = runEm(tg, g0, split3, 3, 50, false, false);
        verifyFscoreIncrease(tg, g3, previousFScore);
    }

    /** Parses the training corpus with the new grammar, reports F-score, and verifies that it's increasing */
    private double verifyFscoreIncrease(final TrainGrammar tg, final FractionalCountGrammar currentGrammar,
            final double previousFScore) {

        final long t0 = System.currentTimeMillis();
        final double fScore = parseFScore(cscGrammar(currentGrammar), tg.goldTrees);
        System.out.format("F-score: %.3f  Parse Time: %.1f seconds\n", fScore * 100,
                (System.currentTimeMillis() - t0) / 1000f);
        assertTrue("Expected f-score to increase", fScore > previousFScore);
        return fScore;
    }

    private FractionalCountGrammar runEm(final TrainGrammar tg, final FractionalCountGrammar markov0Grammar,
            FractionalCountGrammar currentGrammar, final int split, final int iterations,
            final boolean reportFinalParseScore, final boolean reportEmIterationParseScores) {

        final int lexiconSize = currentGrammar.lexicon.size();
        double previousCorpusLikelihood = Float.NEGATIVE_INFINITY;

        final long t0 = System.currentTimeMillis();
        EmIterationResult result = null;

        for (int i = 0; i < iterations; i++) {

            result = tg.emIteration(currentGrammar, Float.NEGATIVE_INFINITY);
            currentGrammar = result.countGrammar;
            currentGrammar.totalParentCounts();
            final ConstrainedInsideOutsideGrammar cscGrammar = cscGrammar(currentGrammar);

            // Ensure we have rules matching each lexical entry
            for (int j = 0; j < lexiconSize; j++) {
                if (cscGrammar.getLexicalProductionsWithChild(j).size() == 0) {
                    System.out.println("Iteration " + (i + 1) + " : Missing parent for "
                            + cscGrammar.lexSet.getSymbol(j));
                }
                assertTrue("Iteration " + (i + 1) + " : No parents found for " + currentGrammar.lexicon.getSymbol(j),
                        cscGrammar.getLexicalProductionsWithChild(j).size() > 0);
            }

            if (i > 2) {
                // Allow a small delta on corpus likelihood comparison to avoid floating-point errors and declines due
                // to rule pruning
                assertTrue(String.format("Corpus likelihood declined from %.2f to %.2f on iteration %d",
                        previousCorpusLikelihood, result.corpusLikelihood, i + 1), result.corpusLikelihood
                        - previousCorpusLikelihood >= -.01f);
            }

            if (reportEmIterationParseScores) {
                // Parse the training corpus with the new CSC grammar and report F-score
                System.out.format("=== Split %d, iteration %d   Likelihood: %.3f   F-score: %.2f\n", split, i + 1,
                        result.corpusLikelihood, parseFScore(cscGrammar, tg.goldTrees) * 100);
            } else {
                System.out.format("=== Split %d, iteration %d   Likelihood: %.3f\n", split, i + 1,
                        result.corpusLikelihood);
            }

            previousCorpusLikelihood = result.corpusLikelihood;
        }

        final long t1 = System.currentTimeMillis();
        System.out.format("Training Time: %.1f seconds\n", (t1 - t0) / 1000f);
        System.out.format("Constrained parse time: %d ms  Count time: %d ms\n", tg.parseTime / 1000000,
                tg.countTime / 1000000);

        if (reportFinalParseScore) {
            // Parse the training corpus with the new CSC grammar and report F-score
            System.out.format("F-score: %.2f\n", parseFScore(cscGrammar(currentGrammar), tg.goldTrees) * 100);
        }

        return currentGrammar;
    }

    private ConstrainedInsideOutsideGrammar cscGrammar(final FractionalCountGrammar countGrammar) {
        return new ConstrainedInsideOutsideGrammar(countGrammar, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);
    }

    private double parseFScore(final Grammar grammar, final List<NaryTree<String>> goldTrees) {
        final LeftCscSparseMatrixGrammar cscGrammar = new LeftCscSparseMatrixGrammar(grammar);
        final ParserDriver opts = new ParserDriver();
        opts.fomModel = new InsideProb();
        final CartesianProductHashSpmlParser parser = new CartesianProductHashSpmlParser(opts, cscGrammar);

        final BracketEvaluator evaluator = new BracketEvaluator();

        for (final NaryTree<String> goldTree : goldTrees) {
            // Extract tokens from training tree, parse, and evaluate
            // TODO Parse from tree instead
            final String sentence = Strings.join(goldTree.leafLabels(), " ");
            final ParseTask context = parser.parseSentence(sentence);

            if (context.binaryParse != null) {
                evaluator.evaluate(goldTree, context.binaryParse.unfactor(cscGrammar.grammarFormat));
            }
        }

        final EvalbResult evalbResult = evaluator.accumulatedResult();
        return evalbResult.f1();
    }
}
