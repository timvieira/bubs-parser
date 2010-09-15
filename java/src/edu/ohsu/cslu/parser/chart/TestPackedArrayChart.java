package edu.ohsu.cslu.parser.chart;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.SimpleShiftFunction;
import edu.ohsu.cslu.parser.ExhaustiveChartParserTestCase;
import edu.ohsu.cslu.parser.chart.Chart.ChartCell;
import edu.ohsu.cslu.parser.chart.PackedArrayChart.BoundedPriorityQueue;
import edu.ohsu.cslu.tests.FilteredRunner;
import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link PackedArrayChart}
 * 
 * @author Aaron Dunlop
 * @since Sep 9, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestPackedArrayChart {

    private PackedArrayChart chart;
    private SparseMatrixGrammar simpleGrammar2;

    /**
     * Constructs the grammar (if necessary) and a new parser instance. Run prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Before
    public void setUp() throws Exception {
        simpleGrammar2 = ExhaustiveChartParserTestCase.createSimpleGrammar2(LeftCscSparseMatrixGrammar.class,
                SimpleShiftFunction.class);
    }

    @Test
    public void testBoundedBinaryHeap() throws Exception {
        final BoundedPriorityQueue h = new BoundedPriorityQueue(5);
        h.insert((short) 1, -1f, 1, (short) 1);
        h.insert((short) 2, -2f, 2, (short) 2);
        h.insert((short) 3, -3f, 3, (short) 3);
        assertEquals(3, h.size());

        h.insert((short) 7, -7f, 7, (short) 7);
        assertEquals(4, h.size());

        h.insert((short) 6, -6f, 6, (short) 6);
        h.insert((short) 5, -5f, 5, (short) 5);
        h.insert((short) 4, -4f, 4, (short) 4);
        assertEquals(5, h.size());

        h.sortByNonterminalIndex();
        assertEquals(1, h.queueParentIndices[0]);
        assertEquals(-1f, h.queueInsideProbabilities[0], .001f);
        assertEquals(1, h.queuePackedChildren[0]);
        assertEquals(1, h.queueMidpoints[0]);

        assertEquals(2, h.queueParentIndices[1]);
        assertEquals(-2f, h.queueInsideProbabilities[1], .001f);
        assertEquals(2, h.queuePackedChildren[1]);
        assertEquals(2, h.queueMidpoints[1]);

        assertEquals(3, h.queueParentIndices[2]);
        assertEquals(-3f, h.queueInsideProbabilities[2], .001f);
        assertEquals(3, h.queuePackedChildren[2]);
        assertEquals(3, h.queueMidpoints[2]);

        assertEquals(4, h.queueParentIndices[3]);
        assertEquals(-4f, h.queueInsideProbabilities[3], .001f);
        assertEquals(4, h.queuePackedChildren[3]);
        assertEquals(4, h.queueMidpoints[3]);

        assertEquals(5, h.queueParentIndices[4]);
        assertEquals(-5f, h.queueInsideProbabilities[4], .001f);
        assertEquals(5, h.queuePackedChildren[4]);
        assertEquals(5, h.queueMidpoints[4]);
    }

    @Test
    public void testUnprunedFinalizeCell() throws Exception {
        chart = new PackedArrayChart(new int[] { 1, 2, 3, 4, 5 }, simpleGrammar2);
        final ChartCell cell_2_3 = chart.getCell(2, 3);

        // Three binary productions
        cell_2_3.updateInside(simpleGrammar2.new Production(1, 2, 3, -3f), cell_2_3, null, -3f);
        cell_2_3.updateInside(simpleGrammar2.new Production(2, 3, 4, -2f), cell_2_3, null, -2f);
        cell_2_3.updateInside(simpleGrammar2.new Production(3, 2, 2, -4f), cell_2_3, null, -4f);

        // Two unary productions, one of which will override a binary production
        cell_2_3.updateInside(simpleGrammar2.new Production(2, 3, -1.5f, false), cell_2_3, null, -1.5f);
        cell_2_3.updateInside(simpleGrammar2.new Production(4, 3, -1.5f, false), cell_2_3, null, -1.5f);
        cell_2_3.finalizeCell();

        assertEquals(4, cell_2_3.getNumNTs());
        assertEquals(-3f, cell_2_3.getInside(1), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(2), 0.01f);
        assertEquals(-4f, cell_2_3.getInside(3), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(4), 0.01f);
    }

    @Test
    public void testPrunedFinalizeCell() throws Exception {
        chart = new PackedArrayChart(new int[] { 1, 2, 3, 4, 5 }, simpleGrammar2, 2);
        final ChartCell cell_2_3 = chart.getCell(2, 3);

        // Three binary productions
        cell_2_3.updateInside(simpleGrammar2.new Production(1, 2, 3, -3f), cell_2_3, null, -3f);
        cell_2_3.updateInside(simpleGrammar2.new Production(2, 3, 4, -2f), cell_2_3, null, -2f);
        cell_2_3.updateInside(simpleGrammar2.new Production(3, 2, 2, -4f), cell_2_3, null, -4f);

        // Two unary productions, one of which will override a binary production
        cell_2_3.updateInside(simpleGrammar2.new Production(2, 3, -1.5f, false), cell_2_3, null, -1.5f);
        cell_2_3.updateInside(simpleGrammar2.new Production(4, 3, -1.5f, false), cell_2_3, null, -1.5f);
        cell_2_3.finalizeCell();

        assertEquals(2, cell_2_3.getNumNTs());
        assertEquals(-1.5f, cell_2_3.getInside(2), 0.01f);
        assertEquals(-1.5f, cell_2_3.getInside(4), 0.01f);
    }
}
