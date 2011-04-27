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
package edu.ohsu.cslu.lela;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.List;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.lela.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.lela.TrainGrammar.EmIterationResult;
import edu.ohsu.cslu.parser.ParseContext;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.spmv.CsrSpmvParser;
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
     * Learns a 2-split grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood increases with
     * successive EM runs.
     * 
     * @throws IOException
     */
    @Test
    public void testWithoutMerging() throws IOException {
        final TrainGrammar tg = new TrainGrammar();
        tg.factorization = Factorization.RIGHT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;

        // final BufferedReader br = new BufferedReader(
        // new StringReader(
        // "(TOP (S (NP (NNP FCC) (NN COUNSEL)) (VP (VBZ JOINS) (NP (NN FIRM))) (: :)))\n"
        // + "(TOP (X (X (SYM z)) (: -) (ADJP (RB Not) (JJ available)) (. .)))\n"
        // +
        // "(TOP (S (S (CC But) (NP (NNS investors)) (VP (MD should) (VP (VB keep) (PP (IN in) (NP (NN mind))) (, ,) (PP (IN before) (S (VP (VBG paying) (ADVP (RB too) (JJ much))))) (, ,) (SBAR (IN that) (S (NP (NP (DT the) (JJ average) (JJ annual) (NN return)) (PP (IN for) (NP (NN stock) (NNS holdings)))) (, ,) (ADVP (JJ long-term)) (, ,) (VP (AUX is) (NP (NP (QP (CD 9) (NN %) (TO to) (CD 10) (NN %))) (NP (DT a) (NN year))))))))) (: ;) (S (NP (NP (DT a) (NN return)) (PP (IN of) (NP (CD 15) (NN %)))) (VP (AUX is) (VP (VBN considered) (S (ADJP (JJ praiseworthy)))))) (. .)))"),
        // 1024);

        final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"),
                20 * 1024 * 1024);

        br.mark(20 * 1024 * 1024);

        final ProductionListGrammar plg0 = tg.induceGrammar(br);
        br.reset();

        tg.loadGoldTreesAndCharts(br, plg0);

        final NoiseGenerator noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f);

        // Split and train with the 1-split grammar
        final ProductionListGrammar plg1 = runEm(tg, plg0.split(noiseGenerator), 1, 10);

        // Split again and train with the 2-split grammar
        runEm(tg, plg1.split(noiseGenerator), 2, 10);
    }

    private ProductionListGrammar runEm(final TrainGrammar tg, final ProductionListGrammar plg, final int split,
            final int iterations) {

        ConstrainedCsrSparseMatrixGrammar csr = csrGrammar(plg);

        final int lexiconSize = csr.lexSet.size();
        float previousCorpusLikelihood = Float.NEGATIVE_INFINITY;

        EmIterationResult result = null;
        for (int i = 0; i < iterations; i++) {
            System.out.format("=== Split %d, iteration %d", split, i + 1);

            result = tg.emIteration(csr);
            csr = csrGrammar(result.plGrammar);

            for (int j = 0; j < lexiconSize; j++) {
                assertTrue("No parents found for " + csr.lexSet.getSymbol(j), csr.getLexicalProductionsWithChild(j)
                        .size() > 0);
            }

            verifyProbabilityDistribution(result.plGrammar);
            System.out.format("   Likelihood: %.3f\n", result.corpusLikelihood);

            assertTrue(String.format("Corpus likelihood declined from %.2f to %.2f on iteration %d",
                    previousCorpusLikelihood, result.corpusLikelihood, i),
                    result.corpusLikelihood >= previousCorpusLikelihood);
            previousCorpusLikelihood = result.corpusLikelihood;
        }

        // Parse the training corpus with the new CSR grammar and report F-score as well as likelihood
        final CsrSpmvParser parser = new CsrSpmvParser(new ParserDriver(), csr);

        final BracketEvaluator evaluator = new BracketEvaluator();
        for (final NaryTree<String> goldTree : tg.goldTrees) {
            // Extract tokens from training tree, parse, and evaluate
            final String sentence = Strings.join(goldTree.leafLabels(), " ");
            final ParseContext context = parser.parseSentence(sentence);
            evaluator.evaluate(goldTree, context.parse.unfactor(csr.grammarFormat));
        }

        final EvalbResult evalbResult = evaluator.accumulatedResult();
        System.out.format("F-score: %.3f\n", evalbResult.f1);

        return result.plGrammar;
    }

    private ConstrainedCsrSparseMatrixGrammar csrGrammar(final ProductionListGrammar plg) {
        return new ConstrainedCsrSparseMatrixGrammar(plg, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);
    }

    /**
     * Verifies that the grammar rules for each parent non-terminal sum to 1
     */
    private void verifyProbabilityDistribution(final ProductionListGrammar plg) {
        final List<Production>[] prodsByParent = plg.productionsByParent();
        for (int i = 0, j = 0; i < prodsByParent.length; i++, j = 0) {
            final float[] probabilities = new float[prodsByParent[i].size()];
            for (final Production p : prodsByParent[i]) {
                probabilities[j++] = p.prob;
            }
            assertEquals("Invalid probability distribution", 0, edu.ohsu.cslu.util.Math.logSumExp(probabilities), .001);
        }
    }
}
