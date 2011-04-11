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
package edu.ohsu.cslu.parser.edgeselector;

import java.io.BufferedReader;
import java.io.IOException;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.edgeselector.BoundaryInOut.BoundaryInOutSelector;
import edu.ohsu.cslu.parser.edgeselector.EdgeSelector.EdgeSelectorType;
import edu.ohsu.cslu.tests.JUnit;

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
                JUnit.unitTestDataAsReader("grammars/wsj.2-21.unk.L2-p1.gz"));
        }
        final BoundaryInOut parentBioFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut,
            parentAnnotatedGrammar, new BufferedReader(
                JUnit.unitTestDataAsReader("parsing/fom.boundary.L2-p1.gold.gz")));

        parentAnnotatedBio = (BoundaryInOutSelector) parentBioFactory
            .createEdgeSelector(parentAnnotatedGrammar);

        if (berkeleyGrammar == null) {
            berkeleyGrammar = new LeftCscSparseMatrixGrammar(
                JUnit.unitTestDataAsReader("grammars/berkeley.eng_sm6.nb.gz"));
        }
        final BoundaryInOut berkeleyBioFactory = new BoundaryInOut(EdgeSelectorType.BoundaryInOut,
            berkeleyGrammar, new BufferedReader(
                JUnit.unitTestDataAsReader("parsing/fom.boundary.berk.parses.gz")));
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
