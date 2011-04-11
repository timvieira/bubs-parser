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
package edu.ohsu.cslu.ella;

import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree.Factorization;
import edu.ohsu.cslu.ella.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.ella.TrainGrammar.EmIterationResult;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.tests.JUnit;

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
     * Learns a 2-split grammar from a small corpus (WSJ section 24). Verifies that corpus likelihood
     * increases with successive EM runs.
     * 
     * @throws IOException
     */
    @Test
    public void testWithoutMerging() throws IOException {
        final TrainGrammar tg = new TrainGrammar();
        tg.factorization = Factorization.RIGHT;
        tg.grammarFormatType = GrammarFormatType.Berkeley;
        final BufferedReader br = new BufferedReader(
            JUnit.unitTestDataAsReader("corpora/wsj/wsj_24.mrgEC.gz"), 20 * 1024 * 1024);
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
            assertTrue(String.format("Corpus likelihood declined from %.2f to %.2f",
                previousCorpusLikelihood, result.corpusLikelihood),
                result.corpusLikelihood >= previousCorpusLikelihood);
            previousCorpusLikelihood = result.corpusLikelihood;
        }

        // TODO - Split again and train with the 2-split grammar
    }

    private ConstrainedCsrSparseMatrixGrammar csrGrammar(final ProductionListGrammar plg) {
        return new ConstrainedCsrSparseMatrixGrammar(plg, GrammarFormatType.Berkeley,
            SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);
    }
}
