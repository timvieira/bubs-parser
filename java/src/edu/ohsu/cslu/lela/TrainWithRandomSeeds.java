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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.lela.FractionalCountGrammar.NoiseGenerator;
import edu.ohsu.cslu.lela.FractionalCountGrammar.RandomNoiseGenerator;
import edu.ohsu.cslu.lela.TrainGrammar.EmIterationResult;

public class TrainWithRandomSeeds extends BaseCommandlineTool {

    @Option(name = "-i", metaVar = "seed", usage = "Initial seed for random noise generator")
    private int initialRandomSeed = 0;

    @Override
    protected void run() throws IOException {
        final BufferedReader br = new BufferedReader(new InputStreamReader(System.in), 20 * 1024 * 1024);
        br.mark(20 * 1024 * 1024);

        for (int seed = initialRandomSeed; seed < (initialRandomSeed + 1000); seed++) {
            final TrainGrammar tg = new TrainGrammar();
            tg.binarization = Binarization.RIGHT;
            tg.grammarFormatType = GrammarFormatType.Berkeley;

            final FractionalCountGrammar grammar0 = new StringCountGrammar(br, Binarization.RIGHT,
                    GrammarFormatType.Berkeley).toFractionalCountGrammar();
            br.reset();

            tg.loadGoldTreesAndConstrainingCharts(br, grammar0);
            br.reset();

            final NoiseGenerator noiseGenerator = new RandomNoiseGenerator(seed, .01f);

            System.out.println("=== Seed: " + seed + " ===");

            // Split and train with the 1-split grammar
            System.out.println("Split 1");
            final FractionalCountGrammar split1 = grammar0.split(noiseGenerator);
            // split1.randomize(random, 0.01f);
            final FractionalCountGrammar plg1 = runEm(tg, split1);

            // Merge TOP_1 back into TOP, split again, and train with the new 2-split grammar
            final FractionalCountGrammar mergedPlg1 = plg1.merge(new short[] { 1 });
            System.out.println("Split 2");
            final FractionalCountGrammar split2 = mergedPlg1.split(noiseGenerator);
            // split2.randomize(random, 0.01f);
            final FractionalCountGrammar plg2 = runEm(tg, split2);

            // Merge TOP_1 back into TOP, split again, and train with the new 3-split grammar
            final FractionalCountGrammar mergedPlg2 = plg2.merge(new short[] { 1 });
            System.out.println("Split 3");
            final FractionalCountGrammar split3 = mergedPlg2.split(noiseGenerator);
            // split3.randomize(random, 0.01f);
            runEm(tg, split3);
        }
    }

    private FractionalCountGrammar runEm(final TrainGrammar tg, final FractionalCountGrammar splitGrammar) {
        FractionalCountGrammar currentGrammar = splitGrammar;
        EmIterationResult result = null;
        for (int i = 0; i < 50; i++) {
            result = tg.emIteration(currentGrammar, -30f);
            System.out.format("Iteration: %2d  Likelihood: %.2f\n", i, result.corpusLikelihood);
            currentGrammar = result.countGrammar;
        }
        return currentGrammar;
    }

    private ConstrainedInsideOutsideGrammar cscGrammar(final FractionalCountGrammar countGrammar) {
        return new ConstrainedInsideOutsideGrammar(countGrammar, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    public static void main(final String[] args) {
        run(args);
    }

}
