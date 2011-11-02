package edu.ohsu.cslu.lela;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Factorization;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.lela.ProductionListGrammar.NoiseGenerator;
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
            tg.binarization = Factorization.RIGHT;
            tg.grammarFormatType = GrammarFormatType.Berkeley;

            final ProductionListGrammar plg0 = tg.induceGrammar(br);
            br.reset();

            tg.loadGoldTreesAndCharts(br, plg0);
            br.reset();

            final NoiseGenerator noiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f, seed);

            System.out.println("=== Seed: " + seed + " ===");

            // Split and train with the 1-split grammar
            System.out.println("Split 1");
            final ProductionListGrammar plg1 = runEm(tg, plg0.split(noiseGenerator));

            // Merge TOP_1 back into TOP, split again, and train with the new 2-split grammar
            final ProductionListGrammar mergedPlg1 = plg1.merge(new short[] { 1 });
            tg.reloadConstrainingCharts(cscGrammar(plg1), cscGrammar(mergedPlg1));
            System.out.println("Split 2");
            final ProductionListGrammar plg2 = runEm(tg, mergedPlg1.split(noiseGenerator));

            // Merge TOP_1 back into TOP, split again, and train with the new 3-split grammar
            final ProductionListGrammar mergedPlg2 = plg2.merge(new short[] { 1 });
            tg.reloadConstrainingCharts(cscGrammar(plg2), cscGrammar(mergedPlg2));
            System.out.println("Split 3");
            final ProductionListGrammar plg3 = runEm(tg, mergedPlg2.split(noiseGenerator));
        }
    }

    private ProductionListGrammar runEm(final TrainGrammar tg, final ProductionListGrammar split) {
        ProductionListGrammar plg = split;
        EmIterationResult result = null;
        for (int i = 0; i < 25; i++) {
            result = tg.emIteration(cscGrammar(plg), -15f);
            System.out.format("Iteration: %2d  Likelihood: %.2f\n", i, result.corpusLikelihood);
            plg = result.plGrammar;
        }
        return result.plGrammar;
    }

    private ConstrainedInsideOutsideGrammar cscGrammar(final ProductionListGrammar plg) {
        return new ConstrainedInsideOutsideGrammar(plg, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    public static void main(final String[] args) {
        run(args);
    }

}
