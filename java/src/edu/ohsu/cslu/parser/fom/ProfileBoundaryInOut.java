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
package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.io.IOException;

import org.cjunit.FilteredRunner;
import org.cjunit.PerformanceTest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.Parser.DecodeMethod;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;
import edu.ohsu.cslu.parser.fom.BoundaryPosModel.BoundaryPosFom;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FOMType;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Profiles the {@link BoundaryPosModel} edge-selector.
 * 
 * @author Aaron Dunlop
 */
@RunWith(FilteredRunner.class)
public class ProfileBoundaryInOut {

    private static SparseMatrixGrammar parentAnnotatedGrammar;
    private static BoundaryPosFom parentAnnotatedBio;

    private static SparseMatrixGrammar berkeleyGrammar;
    private static BoundaryPosFom berkeleyBio;

    private PackedArrayChart parentAnnotatedChart;
    private PackedArrayChart berkeleyChart;

    private ParseTask parentAnnotatedParseContext;
    private ParseTask berkeleyParseContext;

    @BeforeClass
    public static void suiteSetUp() throws IOException {
        if (parentAnnotatedGrammar == null) {
            parentAnnotatedGrammar = new LeftCscSparseMatrixGrammar(
                    JUnit.unitTestDataAsReader("grammars/eng.R2.P1.gr.gz"), new DecisionTreeTokenClassifier(),
                    PerfectIntPairHashPackingFunction.class);
        }
        final BoundaryPosModel parentBioModel = new BoundaryPosModel(FOMType.BoundaryPOS, parentAnnotatedGrammar,
                new BufferedReader(JUnit.unitTestDataAsReader("fom/eng.R2.P1.fom.gz")));

        parentAnnotatedBio = (BoundaryPosFom) parentBioModel.createFOM();

        if (berkeleyGrammar == null) {
            berkeleyGrammar = new LeftCscSparseMatrixGrammar(JUnit.unitTestDataAsReader("../models/eng.sm6.gr.gz"),
                    new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);
        }
        final BoundaryPosModel berkeleyBioModel = new BoundaryPosModel(FOMType.BoundaryPOS, berkeleyGrammar,
                new BufferedReader(JUnit.unitTestDataAsReader("../models/eng.sm6.fom.gz")));
        berkeleyBio = (BoundaryPosFom) berkeleyBioModel.createFOM();
    }

    @Before
    public void setUp() {
        final String sentence = "And a large slice of the first episode is devoted to efforts to get rid of some nearly worthless Japanese bonds -LRB- since when is anything Japanese nearly worthless nowadays ? -RRB- .";
        // final int[] tokens = parentAnnotatedGrammar.tokenizer.tokenizeToIndex(sentence);
        parentAnnotatedParseContext = new ParseTask(sentence, Parser.InputFormat.Text, parentAnnotatedGrammar,
                DecodeMethod.ViterbiMax);
        parentAnnotatedChart = new PackedArrayChart(parentAnnotatedParseContext, parentAnnotatedGrammar, 100, 100);
        berkeleyParseContext = new ParseTask(sentence, Parser.InputFormat.Text, parentAnnotatedGrammar,
                DecodeMethod.ViterbiMax);
        berkeleyChart = new PackedArrayChart(berkeleyParseContext, berkeleyGrammar, 100, 150);
    }

    @Test
    @PerformanceTest({ "mbp", "973", "mbp2012", "3379" })
    public void profileParentAnnotated() {

        for (int i = 0; i < 400; i++) {
            parentAnnotatedBio.initSentence(parentAnnotatedParseContext, parentAnnotatedChart);
        }
    }

    @Test
    @PerformanceTest({ "mbp", "3633", "mbp2012", "3750" })
    public void profileBerkeley() {
        for (int i = 0; i < 400; i++) {
            berkeleyBio.initSentence(berkeleyParseContext, berkeleyChart);
        }
    }
}
