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

import static edu.ohsu.cslu.tests.JUnit.assertLogFractionEquals;
import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.theories.DataPoint;
import org.junit.experimental.theories.Theories;
import org.junit.experimental.theories.Theory;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.narytree.Tree;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.lela.ProductionListGrammar.NoiseGenerator;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for {@link ConstrainedInsideOutsideParser}.
 * 
 * @author Aaron Dunlop
 * @since Jan 15, 2011
 */
@RunWith(Theories.class)
public class TestConstrainedInsideOutsideParser {

    @DataPoint
    public static NoiseGenerator zeroNoiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(0);

    @DataPoint
    public static NoiseGenerator biasedNoiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(0.01f);

    @DataPoint
    public static NoiseGenerator randomNoiseGenerator = new ProductionListGrammar.RandomNoiseGenerator(0.01f);

    private ProductionListGrammar plGrammar0;
    private ProductionListGrammar plGrammar1;
    private ConstrainingChart chart0;
    private ConstrainedInsideOutsideGrammar cscGrammar1;
    private ConstrainedInsideOutsideParser parser1;

    private ProductionListGrammar biasedGrammar1;
    private ConstrainedInsideOutsideGrammar biasedCscGrammar1;

    @Before
    public void setUp() throws IOException {

        // Induce a grammar from the sample tree and construct a basic constraining chart
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE), null,
                null);
        plGrammar0 = new ProductionListGrammar(sg);

        // Create a basic constraining chart
        chart0 = new ConstrainingChart(BinaryTree.read(AllLelaTests.STRING_SAMPLE_TREE, String.class),
                cscGrammar(plGrammar0));

        // Split the grammar
        plGrammar1 = plGrammar0.split(new ProductionListGrammar.BiasedNoiseGenerator(0f));
        cscGrammar1 = cscGrammar(plGrammar1);

        // Split the M0 grammar, biasing all rule splits completely to the 1st option
        // Split the grammar
        biasedGrammar1 = plGrammar0.split(new ProductionListGrammar.BiasedNoiseGenerator(1f));
        biasedCscGrammar1 = cscGrammar(biasedGrammar1);
    }

    /**
     * Parse with the split-1 grammar
     * 
     * @return extracted parse tree
     */
    private BinaryTree<String> parseWithGrammar1() {
        // Parse with the split-1 grammar
        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        parser1 = new ConstrainedInsideOutsideParser(opts, cscGrammar1);
        return parser1.findBestParse(chart0);
    }

    @Test
    public void testCscConversion() {
        final PackingFunction f = cscGrammar1.packingFunction;

        assertEquals(2, f.unpackLeftChild(f.pack((short) 2, (short) 5)));
        assertEquals(5, f.unpackRightChild(f.pack((short) 2, (short) 5)));
        assertEquals(3, f.unpackLeftChild(f.pack((short) 3, (short) 5)));
        assertEquals(5, f.unpackRightChild(f.pack((short) 3, (short) 5)));
    }

    @Test
    public void test1SplitConstrainedParse() {

        final BinaryTree<String> parseTree1 = parseWithGrammar1();
        final ConstrainedChart chart1 = parser1.chart;

        final SymbolSet<String> vocabulary = plGrammar1.vocabulary;
        final short top = (short) vocabulary.getIndex("top");
        final short a_0 = (short) vocabulary.getIndex("a_0");
        final short a_1 = (short) vocabulary.getIndex("a_1");
        final short b_0 = (short) vocabulary.getIndex("b_0");
        final short b_1 = (short) vocabulary.getIndex("b_1");
        final short c_0 = (short) vocabulary.getIndex("c_0");
        final short c_1 = (short) vocabulary.getIndex("c_1");
        final short d_0 = (short) vocabulary.getIndex("d_0");
        final short d_1 = (short) vocabulary.getIndex("d_1");

        // Verify expected inside probabilities in a few cells
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(0, 1, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(0, 1, c_1), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(1, 2, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(1, 2, c_1), .001f);
        assertLogFractionEquals(Math.log(1), chart1.getInside(2, 3, d_0), .001f);
        assertLogFractionEquals(Math.log(1), chart1.getInside(2, 3, d_1), .001f);

        assertLogFractionEquals(Math.log(1), chart1.getInside(3, 4, d_0, 1), .001f);
        assertLogFractionEquals(Math.log(1), chart1.getInside(3, 4, d_1, 1), .001f);
        assertLogFractionEquals(Math.log(1f / 2), chart1.getInside(3, 4, b_0, 2), .001f);
        assertLogFractionEquals(Math.log(1f / 2), chart1.getInside(3, 4, b_1, 2), .001f);
        assertLogFractionEquals(Math.log(1f / 3), chart1.getInside(4, 5, c_0), .001f);
        assertLogFractionEquals(Math.log(1f / 3), chart1.getInside(4, 5, c_1), .001f);

        assertLogFractionEquals(Math.log(4f / 27), chart1.getInside(0, 2, a_0), .001f);
        assertLogFractionEquals(Math.log(4f / 27), chart1.getInside(0, 2, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_1), .001f);

        assertLogFractionEquals(Math.log(1f / 12), chart1.getInside(3, 5, b_0), .001f);
        assertLogFractionEquals(Math.log(1f / 12), chart1.getInside(3, 5, b_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_1), .001f);

        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, top), .001f);
        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, a_0), .001f);
        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_1), .001f);

        // And outside probabilities
        assertLogFractionEquals(Math.log(1.0), chart1.getOutside(0, 5, top), .001f);

        final double outside05 = Math.log(1.0 / 2);
        assertLogFractionEquals(outside05, chart1.getOutside(0, 5, a_0), .001f);
        assertLogFractionEquals(outside05, chart1.getOutside(0, 5, a_1), .001f);

        final double outside03 = Math.log((1.0 / 4) * Math.exp(outside05) * (1.0 / 36) * 4);
        assertLogFractionEquals(outside03, chart1.getOutside(0, 3, a_0), .001f);
        assertLogFractionEquals(outside03, chart1.getOutside(0, 3, a_1), .001f);

        final double outside02 = Math.log((1.0 / 6) * Math.exp(outside03) * (1.0 / 2) * 4);
        assertLogFractionEquals(outside02, chart1.getOutside(0, 2, a_0), .001f);
        assertLogFractionEquals(outside02, chart1.getOutside(0, 2, a_1), .001f);

        final double outside01 = Math.log((1.0 / 6) * Math.exp(outside02) * (1.0 / 3) * 4);
        assertLogFractionEquals(outside01, chart1.getOutside(0, 1, c_0), .001f);
        assertLogFractionEquals(outside01, chart1.getOutside(0, 1, c_1), .001f);

        final double outside23 = Math.log((1.0 / 3) * Math.exp(outside03) * (1.0 / 27) * 4);
        assertLogFractionEquals(outside23, chart1.getOutside(2, 3, d_0), .001f);
        assertLogFractionEquals(outside23, chart1.getOutside(2, 3, d_1), .001f);

        final double outside35 = Math.log((1.0 / 3) * Math.exp(outside05) * (1.0 / 81) * 4);
        assertLogFractionEquals(outside35, chart1.getOutside(3, 5, b_0), .001f);
        assertLogFractionEquals(outside35, chart1.getOutside(3, 5, b_1), .001f);

        // Top-level probability for splits of b in 3,4
        final double outside34 = Math.log((1.0 / 8) * Math.exp(outside35) * (1.0 / 3) * 4);
        assertLogFractionEquals(outside34, chart1.getOutside(3, 4, b_0, 2), .001f);
        assertLogFractionEquals(outside34, chart1.getOutside(3, 4, b_1, 2), .001f);
        assertLogFractionEquals(outside34 + Math.log(1.0 / 2), chart1.getOutside(3, 4, d_0, 1), .001f);
        assertLogFractionEquals(outside34 + Math.log(1.0 / 2), chart1.getOutside(3, 4, d_1, 1), .001f);

        // And ensure that the extracted and unfactored parse matches the input gold tree
        final NaryTree<String> unfactoredTree = BinaryTree.read(parseTree1.toString(), String.class).unfactor(
                GrammarFormatType.Berkeley);
        assertEquals(AllLelaTests.STRING_SAMPLE_TREE, unfactoredTree.toString());
    }

    @Test
    public void testBiased1SplitConstrainedParse() {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        parser1 = new ConstrainedInsideOutsideParser(opts, biasedCscGrammar1);
        parser1.findBestParse(chart0);
        final ConstrainedChart chart1 = parser1.chart;

        final SymbolSet<String> vocabulary = plGrammar1.vocabulary;
        final short top = (short) vocabulary.getIndex("top");
        final short a_0 = (short) vocabulary.getIndex("a_0");
        final short a_1 = (short) vocabulary.getIndex("a_1");
        final short b_0 = (short) vocabulary.getIndex("b_0");
        final short b_1 = (short) vocabulary.getIndex("b_1");
        final short c_0 = (short) vocabulary.getIndex("c_0");
        final short c_1 = (short) vocabulary.getIndex("c_1");
        final short d_0 = (short) vocabulary.getIndex("d_0");
        final short d_1 = (short) vocabulary.getIndex("d_1");

        // Verify expected inside probabilities in a few cells
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(0, 1, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(0, 1, c_1), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(1, 2, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart1.getInside(1, 2, c_1), .001f);
        assertLogFractionEquals(Math.log(1f), chart1.getInside(2, 3, d_0), .001f);
        assertLogFractionEquals(Math.log(1f), chart1.getInside(2, 3, d_1), .001f);

        assertLogFractionEquals(Math.log(1f), chart1.getInside(3, 4, d_0, 1), .001f);
        assertLogFractionEquals(Math.log(1f), chart1.getInside(3, 4, d_1, 1), .001f);
        assertLogFractionEquals(Math.log(1f / 2), chart1.getInside(3, 4, b_0, 2), .001f);
        assertLogFractionEquals(Math.log(1f / 2), chart1.getInside(3, 4, b_1, 2), .001f);
        assertLogFractionEquals(Math.log(1f / 3), chart1.getInside(4, 5, c_0), .001f);
        assertLogFractionEquals(Math.log(1f / 3), chart1.getInside(4, 5, c_1), .001f);

        assertLogFractionEquals(Math.log(4f / 27), chart1.getInside(0, 2, a_0), .001f);
        assertLogFractionEquals(Math.log(4f / 27), chart1.getInside(0, 2, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 2, b_1), .001f);

        assertLogFractionEquals(Math.log(4f / 81), chart1.getInside(0, 3, a_0), .001f);
        assertLogFractionEquals(Math.log(4f / 81), chart1.getInside(0, 3, a_1), .001f);

        assertLogFractionEquals(Math.log(1f / 12), chart1.getInside(3, 5, b_0), .001f);
        assertLogFractionEquals(Math.log(1f / 12), chart1.getInside(3, 5, b_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(3, 5, a_1), .001f);

        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, top), .001f);
        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, a_0), .001f);
        assertLogFractionEquals(Math.log(1.0 / 729), chart1.getInside(0, 5, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart1.getInside(0, 4, b_1), .001f);

        // And outside probabilities
        assertLogFractionEquals(Math.log(1.0), chart1.getOutside(0, 5, top), .001f);

        assertLogFractionEquals(0, chart1.getOutside(0, 5, a_0), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(0, 5, a_1), .001f);

        final double outside03 = Math.log(1.0 / 72);
        assertLogFractionEquals(outside03, chart1.getOutside(0, 3, a_0), .001f);
        assertLogFractionEquals(outside03, chart1.getOutside(0, 3, a_1), .001f);

        final double outside02 = Math.log(1.0 / 216);
        assertLogFractionEquals(outside02, chart1.getOutside(0, 2, a_0), .001f);
        assertLogFractionEquals(outside02, chart1.getOutside(0, 2, a_1), .001f);

        final double outside01 = Math.log(1.0 / 972);
        assertLogFractionEquals(outside01, chart1.getOutside(0, 1, c_0), .001f);
        assertLogFractionEquals(outside01, chart1.getOutside(0, 1, c_1), .001f);

        final double outside12 = Math.log(1.0 / 486);
        assertLogFractionEquals(outside12, chart1.getOutside(1, 2, c_0), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(1, 2, a_1), .001f);

        final double outside23 = Math.log(1.0 / 729);
        assertLogFractionEquals(outside23, chart1.getOutside(2, 3, d_0), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(2, 3, d_1), .001f);

        final double outside35 = Math.log(4.0 / 243);
        assertLogFractionEquals(outside35, chart1.getOutside(3, 5, b_0), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(3, 5, b_1), .001f);

        // Top-level probability for splits of b in 3,4
        final double outside34 = Math.log(1.0 / 729);
        assertLogFractionEquals(outside34, chart1.getOutside(3, 4, b_0, 2), .001f);
        assertLogFractionEquals(outside34, chart1.getOutside(3, 4, b_1, 2), .001f);
        assertLogFractionEquals(outside34, chart1.getOutside(3, 4, d_0, 1), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(3, 4, d_1, 1), .001f);

        final double outside45 = Math.log(1.0 / 243);
        assertLogFractionEquals(outside45, chart1.getOutside(4, 5, c_0), .001f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, chart1.getOutside(4, 5, c_1), .001f);
    }

    @Test
    public void test2SplitConstrainedParse() {

        // Parse with the split-1 grammar, creating a new constraining chart.
        parseWithGrammar1();

        // Split the grammar again
        final ProductionListGrammar plGrammar2 = plGrammar1.split(new ProductionListGrammar.BiasedNoiseGenerator(0f));
        final ConstrainedInsideOutsideGrammar cscGrammar2 = cscGrammar(plGrammar2);

        //
        // Parse with the split-2 grammar, constrained by the split-1 chart
        //

        // Construct a Constraining chart based on the 1-best output of the 1-split parse
        final ConstrainingChart constrainingChart1 = new ConstrainingChart(parser1.chart);

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;

        final ConstrainedInsideOutsideParser parser2 = new ConstrainedInsideOutsideParser(opts, cscGrammar2);
        final BinaryTree<String> parseTree2 = parser2.findBestParse(constrainingChart1);

        // Verify expected inside probabilities in a few cells
        final ConstrainedChart chart2 = parser2.chart;
        final SymbolSet<String> vocabulary = plGrammar2.vocabulary;
        final short top = (short) vocabulary.getIndex("top");
        final short a_0 = (short) vocabulary.getIndex("a_0");
        final short a_1 = (short) vocabulary.getIndex("a_1");
        final short b_0 = (short) vocabulary.getIndex("b_0");
        final short b_1 = (short) vocabulary.getIndex("b_1");
        final short c_0 = (short) vocabulary.getIndex("c_0");
        final short c_1 = (short) vocabulary.getIndex("c_1");
        final short d_0 = (short) vocabulary.getIndex("d_0");
        final short d_1 = (short) vocabulary.getIndex("d_1");

        assertLogFractionEquals(Math.log(2f / 3), chart2.getInside(0, 1, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart2.getInside(0, 1, c_1), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart2.getInside(1, 2, c_0), .001f);
        assertLogFractionEquals(Math.log(2f / 3), chart2.getInside(1, 2, c_1), .001f);
        assertLogFractionEquals(Math.log(1f), chart2.getInside(2, 3, d_0), .001f);
        assertLogFractionEquals(Math.log(1f), chart2.getInside(2, 3, d_1), .001f);

        assertLogFractionEquals(Math.log(1f), chart2.getInside(3, 4, d_0), .001f);
        assertLogFractionEquals(Math.log(1f), chart2.getInside(3, 4, d_1), .001f);

        assertLogFractionEquals(Math.log(1f / 3), chart2.getInside(4, 5, c_0), .001f);
        assertLogFractionEquals(Math.log(1f / 3), chart2.getInside(4, 5, c_1), .001f);

        assertLogFractionEquals(Math.log(1f / 27), chart2.getInside(0, 2, a_0), .001f);
        assertLogFractionEquals(Math.log(1f / 27), chart2.getInside(0, 2, a_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 2, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 2, b_1), .001f);

        assertLogFractionEquals(Math.log(1f / 324), chart2.getInside(0, 3, a_0), .001f);
        assertLogFractionEquals(Math.log(1f / 324), chart2.getInside(0, 3, a_1), .001f);

        assertLogFractionEquals(Math.log(1f / 96), chart2.getInside(3, 5, b_0), .001f);
        assertLogFractionEquals(Math.log(1f / 96), chart2.getInside(3, 5, b_1), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(3, 5, a_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(3, 5, a_1), .001f);

        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 4, b_0), .001f);
        assertEquals(Float.NEGATIVE_INFINITY, chart2.getInside(0, 4, b_1), .001f);

        // And outside probabilities
        assertLogFractionEquals(Math.log(1.0), chart2.getOutside(0, 5, top), .001f);

        final double outside05 = Math.log(1.0 / 4);
        assertLogFractionEquals(outside05, chart2.getOutside(0, 5, a_0), .001f);
        assertLogFractionEquals(outside05, chart2.getOutside(0, 5, a_1), .001f);

        // 0,5,a_0 outside X 3,5,b_0 inside X P(a_0 -> a_0 b_0) X 4
        final double outside03 = outside05 + Math.log(1.0 / 1152);
        assertLogFractionEquals(outside03, chart2.getOutside(0, 3, a_0), .001f);
        assertLogFractionEquals(outside03, chart2.getOutside(0, 3, a_1), .001f);

        // 0,3,a_0 outside X 2,3,b_0 inside X P(a_0 -> a_0 b_0) X 4
        final double outside02 = outside03 + Math.log(1.0 / 12);
        assertLogFractionEquals(outside02, chart2.getOutside(0, 2, a_0), .001f);
        assertLogFractionEquals(outside02, chart2.getOutside(0, 2, a_1), .001f);

        // 0,2,a_0 outside X 1,2,a_0 inside X P(a_0 -> a_0 a_0) X 4
        final double outside01 = outside02 + Math.log(1.0 / 18);
        assertLogFractionEquals(outside01, chart2.getOutside(0, 1, c_0), .001f);
        assertLogFractionEquals(outside01, chart2.getOutside(0, 1, c_1), .001f);

        // 0,3,a_0 outside X 0,2,a_0 inside X P(a_0 -> a_0 b_0) X 4
        final double outside23 = outside03 + Math.log(1.0 / 324);
        assertLogFractionEquals(outside23, chart2.getOutside(2, 3, d_0), .001f);
        assertLogFractionEquals(outside23, chart2.getOutside(2, 3, d_1), .001f);

        // 0,5,a_0 outside X 0,3,a_0 inside X P(a_0 -> a_0 b_0) X 4
        final double outside35 = outside05 + Math.log(1.0 / 3888);
        assertLogFractionEquals(outside35, chart2.getOutside(3, 5, b_0), .001f);
        assertLogFractionEquals(outside35, chart2.getOutside(3, 5, b_1), .001f);

        // Top-level probability for splits of b in 3,4
        // 3,5,b_0 outside * 4,5,a_0 inside X P(b_0 -> b_0 a_0) X 4
        final double outside34 = outside35 + Math.log(1.0 / 24);
        assertLogFractionEquals(outside34, chart2.getOutside(3, 4, b_0, 2), .001f);
        assertLogFractionEquals(outside34, chart2.getOutside(3, 4, b_1, 2), .001f);
        assertLogFractionEquals(outside34 + Math.log(1.0 / 4), chart2.getOutside(3, 4, d_0, 1), .001f);
        assertLogFractionEquals(outside34 + Math.log(1.0 / 4), chart2.getOutside(3, 4, d_1, 1), .001f);

        // And ensure that the extracted and unfactored parse matches the input gold tree
        final NaryTree<String> unfactoredTree = BinaryTree.read(parseTree2.toString(), String.class).unfactor(
                GrammarFormatType.Berkeley);
        assertEquals(AllLelaTests.STRING_SAMPLE_TREE, unfactoredTree.toString());
    }

    @Theory
    public void testLongUnaryChain(final NoiseGenerator noiseGenerator) throws IOException {

        // Induce a grammar from the corpus and construct a basic constraining chart
        final ProductionListGrammar plg0 = induceProductionListGrammar(new StringReader(
                AllLelaTests.TREE_WITH_LONG_UNARY_CHAIN));
        final ConstrainedInsideOutsideGrammar csc0 = cscGrammar(plg0);

        // Split the grammar
        final ProductionListGrammar plg1 = plg0.split(noiseGenerator);
        final ConstrainedInsideOutsideGrammar csc1 = cscGrammar(plg1);

        // Construct a constraining chart
        final NaryTree<String> goldTree = NaryTree.read(AllLelaTests.TREE_WITH_LONG_UNARY_CHAIN, String.class);
        final BinaryTree<String> factoredTree = goldTree.factor(GrammarFormatType.Berkeley, Binarization.RIGHT);
        final ConstrainingChart constrainingChart = new ConstrainingChart(factoredTree, csc0);

        // Parse with the split-1 grammar
        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedInsideOutsideParser parser = new ConstrainedInsideOutsideParser(opts, csc1);

        // TODO Eliminate multiple conversions
        final BinaryTree<String> parseTree1 = parser.findBestParse(constrainingChart);
        final NaryTree<String> unfactoredTree = parseTree1.unfactor(GrammarFormatType.Berkeley);
        assertEquals(goldTree.toString(), unfactoredTree.toString());
    }

    private ProductionListGrammar induceProductionListGrammar(final Reader reader) throws IOException {
        final StringCountGrammar sg = new StringCountGrammar(reader, Binarization.RIGHT, GrammarFormatType.Berkeley);
        return new ProductionListGrammar(sg);
    }

    private ConstrainedInsideOutsideGrammar cscGrammar(final ProductionListGrammar plg) {
        return new ConstrainedInsideOutsideGrammar(plg.binaryProductions, plg.unaryProductions, plg.lexicalProductions,
                plg.vocabulary, plg.lexicon, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class, null);
    }

    @Theory
    public void testWsjSubset1Split(final NoiseGenerator noiseGenerator) throws Exception {
        final String corpus = "parsing/wsj.24.trees.1-20";

        // Induce a grammar from the corpus
        final ProductionListGrammar plg0 = induceProductionListGrammar(JUnit.unitTestDataAsReader(corpus));
        final ConstrainedInsideOutsideGrammar csc0 = cscGrammar(plg0);

        // Split the grammar
        final ProductionListGrammar plg1 = plg0.split(noiseGenerator);
        final ConstrainedInsideOutsideGrammar csc1 = cscGrammar(plg1);

        // Parse each tree in the training corpus with the split-1 grammar
        parseAndCheck(JUnit.unitTestDataAsReader(corpus), csc0, csc1);
    }

    private void parseAndCheck(final Reader corpus, final ConstrainedInsideOutsideGrammar unsplitGrammar,
            final ConstrainedInsideOutsideGrammar splitGrammar) throws IOException {

        final BufferedReader br = new BufferedReader(corpus);

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedInsideOutsideParser parser = new ConstrainedInsideOutsideParser(opts, splitGrammar);

        @SuppressWarnings("unused")
        int count = 0;
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            final BinaryTree<String> factoredTree = goldTree.factor(GrammarFormatType.Berkeley, Binarization.RIGHT);
            final ConstrainingChart constrainingChart = new ConstrainingChart(factoredTree, unsplitGrammar);

            // Ensure that we're constructing the constraining chart correctly
            assertEquals(factoredTree.toString(), constrainingChart.extractBestParse(0).toString());

            // TODO Eliminate multiple conversions
            final BinaryTree<String> parseTree1 = parser.findBestParse(constrainingChart);
            final NaryTree<String> unfactoredTree = parseTree1.unfactor(GrammarFormatType.Berkeley);

            // Ensure that the resulting parse matches the gold tree
            assertEquals(goldTree.toString(), unfactoredTree.toString());
            count++;
        }
    }

    @Test
    public void testWsjSubset2Splits() throws Exception {
        // We have to bias these splits
        final NoiseGenerator noiseGenerator = new ProductionListGrammar.BiasedNoiseGenerator(0.01f);
        final String corpus = "parsing/wsj.24.trees.1-20";

        // Induce a grammar from the corpus
        final ProductionListGrammar plg0 = induceProductionListGrammar(JUnit.unitTestDataAsReader(corpus));
        final ConstrainedInsideOutsideGrammar csc0 = cscGrammar(plg0);

        // Split the grammar
        final ProductionListGrammar plg1 = plg0.split(noiseGenerator);
        final ConstrainedInsideOutsideGrammar csc1 = cscGrammar(plg1);

        // Split the grammar again
        final ProductionListGrammar plg2 = plg1.split(noiseGenerator);
        final ConstrainedInsideOutsideGrammar csc2 = cscGrammar(plg2);

        // Parse each tree first with the split-1 grammar (constrained by unsplit trees), and then with the split-2
        // grammar (constrained by the split-1 parses). Convert each split-2 tree back to its split-1 form and ensure it
        // matches the split-1 parse

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        final ConstrainedInsideOutsideParser p1 = new ConstrainedInsideOutsideParser(opts, csc1);
        final ConstrainedInsideOutsideParser p2 = new ConstrainedInsideOutsideParser(opts, csc2);

        final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader(corpus));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final NaryTree<String> goldTree = NaryTree.read(line, String.class);
            final BinaryTree<String> tree0 = goldTree.factor(GrammarFormatType.Berkeley, Binarization.RIGHT);

            final ConstrainingChart constrainingChart0 = new ConstrainingChart(tree0, csc0);
            final BinaryTree<String> tree1 = p1.findBestParse(constrainingChart0);

            final ConstrainingChart constrainingChart1 = new ConstrainingChart(tree1, csc1);
            final BinaryTree<String> tree2 = p2.findBestParse(constrainingChart1);

            final BinaryTree<String> preSplitTree = toPreSplitTree(tree2);

            // Ensure that the resulting parse matches the constraining 1-split parse
            assertEquals(tree1.toString(), preSplitTree.toString());
        }
    }

    // @Theory
    // public void testWsjSubsetAfterMerge(final NoiseGenerator noiseGenerator) throws Exception {
    // final String corpus = "parsing/wsj_24.mrgEC.1-20";
    //
    // // Induce a grammar from the corpus
    // final ProductionListGrammar plg0 = induceProductionListGrammar(JUnit.unitTestDataAsReader(corpus));
    // final ConstrainedInsideOutsideGrammar csc0 = cscGrammar(plg0);
    //
    // // Split the grammar
    // final ProductionListGrammar plg1 = plg0.split(noiseGenerator);
    // final ConstrainedInsideOutsideGrammar csc1 = cscGrammar(plg1);
    //
    // // Split the grammar again
    // final ProductionListGrammar plg2 = plg1.split(noiseGenerator);
    // final ConstrainedInsideOutsideGrammar csc2 = cscGrammar(plg2);
    //
    // // Re-merge some categories in the split-2 grammar
    // final ProductionListGrammar plg2b = plg2.merge(new short[] { 2, 4, 8, 12, 20, 22, 26, 40, 46, 56, 70, 72, 100,
    // 110 });
    // final ConstrainedInsideOutsideGrammar csc2b = cscGrammar(plg2b);
    //
    // // Split the merged 2-split grammar again (producing a 3-split grammar) and parse with that grammar
    // final ProductionListGrammar plg3 = plg2b.split(noiseGenerator);
    // final ConstrainedInsideOutsideGrammar csc3 = cscGrammar(plg3);
    //
    // // Parse each tree first with the split-1 grammar (constrained by unsplit trees), and then with the
    // // split-2
    // // grammar (constrained by the split-1 parses). Convert each split-2 tree back to its split-1 form and
    // // ensure it
    // // matches the split-1 parse
    //
    // final ParserDriver opts = new ParserDriver();
    // opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
    // final ConstrainedInsideOutsideParser p1 = new ConstrainedInsideOutsideParser(opts, csc1);
    // final ConstrainedInsideOutsideParser p2 = new ConstrainedInsideOutsideParser(opts, csc2);
    // final ConstrainedInsideOutsideParser p3 = new ConstrainedInsideOutsideParser(opts, csc3);
    //
    // final BufferedReader br = new BufferedReader(JUnit.unitTestDataAsReader(corpus));
    // for (String line = br.readLine(); line != null; line = br.readLine()) {
    // final NaryTree<String> goldTree = NaryTree.read(line, String.class);
    // final BinaryTree<String> tree0 = goldTree.factor(GrammarFormatType.Berkeley, Binarization.RIGHT);
    //
    // // Parse with the split-1 grammar
    // final ConstrainingChart c0 = new ConstrainingChart(tree0, csc0);
    // p1.findBestParse(c0);
    // // Parse with the split-2 grammar (constrained by the split-1 parse)
    // p2.findBestParse(c0).toString();
    //
    // // Apply the grammar merge to the split-2 chart
    // final ConstrainedChart c2b = p2.chart.merge(csc2b);
    //
    // // Ensure that the tree encoded in the newly-merged chart matches the original un-split tree
    // assertEquals(goldTree.toString(), BinaryTree.read(c2b.extractBestParse(0).toString(), String.class)
    // .unfactor(GrammarFormatType.Berkeley).toString());
    //
    // final BinaryTree<String> tree3 = BinaryTree.read(p3.findBestParse(c0).toString(), String.class);
    //
    // // Ensure that the resulting parse matches the gold tree
    // assertEquals(goldTree.toString(), tree3.unfactor(GrammarFormatType.Berkeley).toString());
    // }
    // }

    @Test
    public void testEqualSplitRuleCount() {

        // Parse with an equal-split grammar, count (fractional) rule occurrences, and convert those counts
        // into a grammar
        parseWithGrammar1();
        final FractionalCountGrammar countGrammar = new FractionalCountGrammar(parser1.grammar);
        parser1.countRuleOccurrences(countGrammar);
        final ProductionListGrammar plg = countGrammar.toProductionListGrammar(Float.NEGATIVE_INFINITY);

        // Verify that we find the same probabilities in the original split grammar
        assertLogFractionEquals(Math.log(1f / 2), plg.unaryLogProbability("top", "a_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 2), plg.unaryLogProbability("top", "a_1"), .01f);

        assertLogFractionEquals(Math.log(2f / 3), plg.lexicalLogProbability("c_0", "e"), 0.01f);
        assertLogFractionEquals(Math.log(1f / 3), plg.lexicalLogProbability("c_1", "f"), 0.01f);

        assertLogFractionEquals(Float.NEGATIVE_INFINITY, plg.lexicalLogProbability("d_0", "e"), 0.01f);
        assertLogFractionEquals(Math.log(1f), plg.lexicalLogProbability("d_1", "f"), 0.01f);

        assertLogFractionEquals(Math.log(1f / 3 / 4), plg.binaryLogProbability("a_0", "c_0", "c_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 3 / 4), plg.binaryLogProbability("a_0", "c_1", "c_1"), .01f);

        assertLogFractionEquals(Math.log(1f / 3 / 4), plg.binaryLogProbability("a_1", "a_0", "b_0"), .01f);
        assertLogFractionEquals(Math.log(1f / 3 / 4), plg.binaryLogProbability("a_1", "a_1", "b_1"), .01f);

        assertLogFractionEquals(Math.log(1f / 2 / 4), plg.binaryLogProbability("b_0", "b_0", "c_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 4), plg.binaryLogProbability("b_1", "b_1", "c_0"), .01f);

        assertLogFractionEquals(Math.log(1f / 2 / 2), plg.unaryLogProbability("b_0", "d_1"), .01f);
        assertLogFractionEquals(Math.log(1f / 2 / 2), plg.unaryLogProbability("b_1", "d_0"), .01f);
    }

    @Test
    public void testBiasedSplitRuleCount() {

        final ParserDriver opts = new ParserDriver();
        opts.cellSelectorModel = ConstrainedCellSelector.MODEL;
        parser1 = new ConstrainedInsideOutsideParser(opts, biasedCscGrammar1);
        parser1.findBestParse(chart0);

        final FractionalCountGrammar countGrammar = new FractionalCountGrammar(parser1.grammar);
        parser1.countRuleOccurrences(countGrammar);
        final ProductionListGrammar plg = countGrammar.toProductionListGrammar(Float.NEGATIVE_INFINITY);

        // Verify that we find the same probabilities in the original split grammar
        assertLogFractionEquals(Math.log(1), plg.unaryLogProbability("top", "a_0"), .01f);
        assertLogFractionEquals(Math.log(0), plg.unaryLogProbability("top", "a_1"), .01f);

        assertLogFractionEquals(Math.log(3f / 5), plg.lexicalLogProbability("c_0", "e"), 0.01f);
        assertLogFractionEquals(Math.log(1), plg.lexicalLogProbability("c_1", "e"), 0.01f);
        assertLogFractionEquals(Math.log(2f / 5), plg.lexicalLogProbability("c_0", "f"), 0.01f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, plg.lexicalLogProbability("c_1", "f"), 0.01f);

        assertLogFractionEquals(Math.log(1), plg.lexicalLogProbability("d_0", "f"), 0.01f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, plg.lexicalLogProbability("d_1", "f"), 0.01f);

        assertLogFractionEquals(Float.NEGATIVE_INFINITY, plg.unaryLogProbability("b_0", "d_1"), .01f);
        assertLogFractionEquals(Math.log(1), plg.unaryLogProbability("b_1", "d_0"), .01f);

        assertLogFractionEquals(Math.log(1f / 8), plg.binaryLogProbability("a_0", "c_0", "c_0"), .01f);
        assertLogFractionEquals(Float.NEGATIVE_INFINITY, plg.binaryLogProbability("a_0", "c_1", "c_1"), .01f);
    }

    private BinaryTree<String> toPreSplitTree(final BinaryTree<String> splitTree) {
        final BinaryTree<String> preSplitTree = splitTree.transform(new Tree.LabelTransformer<String>() {

            @Override
            public String transform(final String label) {
                final String[] split = label.split("_");

                // Special-case for un-split labels, including lexical items and the start symbol
                if (split.length == 1) {
                    return label;
                }
                final int subcategory = Integer.parseInt(split[1]);
                return split[0] + '_' + (subcategory / 2);
            }
        });
        return preSplitTree;
    }
}
