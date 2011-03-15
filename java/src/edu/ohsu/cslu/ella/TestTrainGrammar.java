package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.ella.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.ella.TrainGrammar.EmIterationResult;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unified tests for training of a split-merge grammar.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 * 
 * @version $Revision$ $Date$ $Author$
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
        final BufferedReader br = new BufferedReader(
                SharedNlpTests.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"), 20 * 1024 * 1024);
        br.mark(20 * 1024 * 1024);
        final ProductionListGrammar plg0 = tg.induceGrammar(br);
        br.reset();

        tg.loadConstrainingCharts(br, plg0);

        // Split 1
        final NoiseGenerator noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f);
        final ProductionListGrammar plg1 = plg0.split(noiseGenerator);
        ConstrainedCsrSparseMatrixGrammar csr1 = csrGrammar(plg1);

        float previousCorpusLikelihood = Float.NEGATIVE_INFINITY;

        for (int i = 0; i < 10; i++) {
            System.out.println("=== Split 1, iteration " + i);

            final EmIterationResult result = tg.emIteration(csr1);
            csr1 = csrGrammar(result.plGrammar);
            assertTrue(String.format("Corpus likelihood declined from %.2f to %.2f", previousCorpusLikelihood,
                    result.corpusLikelihood), result.corpusLikelihood >= previousCorpusLikelihood);
            previousCorpusLikelihood = result.corpusLikelihood;
        }

        // TODO - Split again and train with the 2-split grammar
    }

    private ConstrainedCsrSparseMatrixGrammar csrGrammar(final ProductionListGrammar plg) {
        return new ConstrainedCsrSparseMatrixGrammar(plg, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);
    }
}
