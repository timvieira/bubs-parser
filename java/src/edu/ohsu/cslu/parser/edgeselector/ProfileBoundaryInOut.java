package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut.BoundaryInOutSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

@RunWith(FilteredRunner.class)
public class ProfileBoundaryInOut {

    private static SparseMatrixGrammar parentAnnotatedGrammar;
    private static BoundaryInOutSelector parentAnnotatedBio;

    private static SparseMatrixGrammar berkeleyGrammar;
    private static BoundaryInOutSelector berkeleyBio;

    private PackedArrayChart parentAnnotatedChart;
    private PackedArrayChart berkeleyChart;

    @BeforeClass
    public static void suiteSetUp() throws IOException {
        if (parentAnnotatedGrammar == null) {
            parentAnnotatedGrammar = new LeftCscSparseMatrixGrammar(
                    SharedNlpTests.unitTestDataAsReader("grammars/wsj.2-21.unk.L2-p1.gz"));
        }
        final BoundaryInOut parentBioFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut,
                parentAnnotatedGrammar, new BufferedReader(
                        SharedNlpTests.unitTestDataAsReader("parsing/fom.boundary.L2-p1.gold.gz")));

        parentAnnotatedBio = (BoundaryInOutSelector) parentBioFactory.createEdgeSelector(parentAnnotatedGrammar);

        if (berkeleyGrammar == null) {
            berkeleyGrammar = new LeftCscSparseMatrixGrammar(
                    SharedNlpTests.unitTestDataAsReader("grammars/berkeley.eng_sm6.nb.gz"));
        }
        final BoundaryInOut berkeleyBioFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut, berkeleyGrammar,
                new BufferedReader(SharedNlpTests.unitTestDataAsReader("parsing/fom.boundary.berk.parses.gz")));
        berkeleyBio = (BoundaryInOutSelector) berkeleyBioFactory.createEdgeSelector(berkeleyGrammar);
    }

    @Before
    public void setUp() {
        final String sentence = "And a large slice of the first episode is devoted to efforts to get rid of some nearly worthless Japanese bonds -LRB- since when is anything Japanese nearly worthless nowadays ? -RRB- .";
        final int[] tokens = parentAnnotatedGrammar.tokenizer.tokenizeToIndex(sentence);
        parentAnnotatedChart = new PackedArrayChart(tokens, parentAnnotatedGrammar, 100, 100);
        berkeleyChart = new PackedArrayChart(tokens, berkeleyGrammar, 100, 150);
    }

    @Test
    @PerformanceTest({ "mbp", "1443", "d820", "3794" })
    public void profileParentAnnotated() {

        for (int i = 0; i < 100; i++) {
            parentAnnotatedBio.init(parentAnnotatedChart);
        }
    }

    @Test
    @PerformanceTest({ "mbp", "758" })
    public void profileBerkeley() {
        for (int i = 0; i < 50; i++) {
            berkeleyBio.init(berkeleyChart);
        }
    }
}
