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

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;

import edu.ohsu.cslu.datastructs.narytree.BinaryTree;
import edu.ohsu.cslu.grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.lela.FractionalCountGrammar.ZeroNoiseGenerator;

/**
 * Shared functionality for {@link TestConstrainedChart} and {@link TestConstrainingChart}
 */
public class ChartTestCase {

    protected final static short[][] OPEN_CELLS = new short[][] { { 0, 1 }, { 1, 2 }, { 2, 3 }, { 3, 4 }, { 4, 5 },
            { 0, 2 }, { 3, 5 }, { 0, 3 }, { 0, 5 } };

    protected FractionalCountGrammar grammar0;
    protected ConstrainedInsideOutsideGrammar cscGrammar0;

    FractionalCountGrammar grammar1;
    ConstrainedInsideOutsideGrammar cscGrammar1;

    protected short top;
    protected short a;
    protected short b;
    protected short c;
    protected short d;
    protected int e;
    protected int f;

    @Before
    public void setUp() throws IOException {
        // Induce a grammar from the sample tree
        final StringCountGrammar sg = new StringCountGrammar(new StringReader(AllLelaTests.STRING_SAMPLE_TREE), null,
                null);

        // Construct a SparseMatrixGrammar from the induced grammar
        grammar0 = sg.toFractionalCountGrammar();
        cscGrammar0 = new ConstrainedInsideOutsideGrammar(grammar0, GrammarFormatType.Berkeley,
                SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class);

        final SymbolSet<String> vocabulary = grammar0.vocabulary;
        top = (short) vocabulary.getIndex("top");
        a = (short) vocabulary.getIndex("a");
        b = (short) vocabulary.getIndex("b");
        c = (short) vocabulary.getIndex("c");
        d = (short) vocabulary.getIndex("d");
        e = grammar0.lexicon.getIndex("e");
        f = grammar0.lexicon.getIndex("f");

        // Split the grammar
        grammar1 = grammar0.split(new ZeroNoiseGenerator());
        cscGrammar1 = new ConstrainedInsideOutsideGrammar(grammar1, GrammarFormatType.Berkeley,
                PerfectIntPairHashPackingFunction.class);
    }

    /**
     * @return a populated 1-split {@link ConstrainedChart}
     */
    protected ConstrainedChart create1SplitConstrainedChart() {

        // Construct a basic 0-split constraining chart
        final ConstrainingChart chart0 = new ConstrainingChart(BinaryTree.read(AllLelaTests.STRING_SAMPLE_TREE,
                String.class), cscGrammar0);

        // Create and populate a 1-split ConstrainedChart
        final ConstrainedChart chart = new ConstrainedChart(chart0, cscGrammar1);

        // 0,1
        int offset = chart.offset(chart.cellIndex(0, 1));
        chart.nonTerminalIndices[offset] = 6; // c_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(e);

        chart.nonTerminalIndices[offset + 1] = 7; // c_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(e);

        // 1,2
        offset = chart.offset(chart.cellIndex(1, 2));
        chart.nonTerminalIndices[offset] = 6; // c_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(e);

        chart.nonTerminalIndices[offset + 1] = 7; // c_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(e);

        // 2,3
        offset = chart.offset(chart.cellIndex(2, 3));
        chart.nonTerminalIndices[offset] = 8; // d_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(f);

        chart.nonTerminalIndices[offset + 1] = 9; // d_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(f);

        // 3,4
        offset = chart.offset(chart.cellIndex(3, 4));
        chart.nonTerminalIndices[offset] = 4; // b_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 5; // b_1
        chart.insideProbabilities[offset + 1] = -2;
        chart.outsideProbabilities[offset + 1] = -2;

        chart.nonTerminalIndices[offset + 2] = 8; // d_0
        chart.insideProbabilities[offset + 2] = -2;
        chart.outsideProbabilities[offset + 2] = -2;
        chart.packedChildren[offset + 2] = cscGrammar1.packingFunction.packLexical(f);

        chart.nonTerminalIndices[offset + 3] = 9; // d_1 *
        chart.insideProbabilities[offset + 3] = -1;
        chart.outsideProbabilities[offset + 3] = -1;
        chart.packedChildren[offset + 3] = cscGrammar1.packingFunction.packLexical(f);

        // 4,5
        offset = chart.offset(chart.cellIndex(4, 5));
        chart.nonTerminalIndices[offset] = 6; // c_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;
        chart.packedChildren[offset] = cscGrammar1.packingFunction.packLexical(f);

        chart.nonTerminalIndices[offset + 1] = 7; // c_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;
        chart.packedChildren[offset + 1] = cscGrammar1.packingFunction.packLexical(f);

        // 0,2
        offset = chart.offset(chart.cellIndex(0, 2));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 3,5
        offset = chart.offset(chart.cellIndex(3, 5));
        chart.nonTerminalIndices[offset] = 4; // b_0
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;

        chart.nonTerminalIndices[offset + 1] = 5; // b_1 *
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 0,3
        offset = chart.offset(chart.cellIndex(0, 3));
        chart.nonTerminalIndices[offset] = 2; // a_0 *
        chart.insideProbabilities[offset] = -1;
        chart.outsideProbabilities[offset] = -1;

        chart.nonTerminalIndices[offset + 1] = 3; // a_1
        chart.insideProbabilities[offset + 1] = -1;
        chart.outsideProbabilities[offset + 1] = -1;

        // 0,5
        offset = chart.offset(chart.cellIndex(0, 5));
        chart.nonTerminalIndices[offset] = 0; // top
        chart.insideProbabilities[offset] = -2;
        chart.outsideProbabilities[offset] = -2;

        chart.nonTerminalIndices[offset + 2] = 2; // a_0
        chart.insideProbabilities[offset + 2] = -2;
        chart.outsideProbabilities[offset + 2] = -2;

        chart.nonTerminalIndices[offset + 3] = 3; // a_1 *
        chart.insideProbabilities[offset + 3] = -1;
        chart.outsideProbabilities[offset + 3] = -1;

        return chart;
    }
}
