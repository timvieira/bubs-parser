package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.ParameterizedType;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.parser.chart.CellChart;
import edu.ohsu.cslu.parser.chart.CellChart.ChartCell;
import edu.ohsu.cslu.parser.chart.CellChart.ChartEdge;

/**
 * Base test class for testing {@link ChartCell} implementations.
 * 
 * @author Aaron Dunlop
 * @since Jan 4, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class ChartCellTestCase<C extends ChartCell, G extends Grammar> {

    @SuppressWarnings("unchecked")
    private Class<G> grammarClass() {
        return (Class<G>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    /**
     * Tests the {@link ChartCell#addEdge(ChartEdge)} and {@link ChartCell#addEdge(edu.ohsu.cslu.grammar.Grammar.Production, ChartCell, ChartCell, float)} methods.
     * 
     * @throws Exception if something bad happens
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testAddEdge() throws Exception {
        final G simpleGrammar = (G) GrammarTestCase.createSimpleGrammar(grammarClass());
        final CellChart chart = new CellChart(2, simpleGrammar);
        final ChartCell parent = chart.getCell(0, 2);
        final ChartCell leftChild = chart.getCell(0, 1);
        final ChartCell rightChild = chart.getCell(1, 2);

        // parent.addEdge(chart.new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f), -0.5f);
        parent.updateInside(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f);
        assertEquals(1, parent.numEdgesAdded);
        assertEquals(1, parent.numEdgesConsidered);

        // Try adding another edge with lower probability. The count of edges considered should increment, but
        // not the count of edges added.
        // parent.addEdge(simpleGrammar.new Production("NP", "NP", "NN", -0.6f), leftChild, rightChild, -0.6f);
        parent.updateInside(simpleGrammar.new Production("NP", "NP", "NN", -0.6f), leftChild, rightChild, -0.6f);
        assertEquals(1, parent.numEdgesAdded);
        assertEquals(2, parent.numEdgesConsidered);
    }

    /**
     * Tests the {@link ChartCell#getBestEdge(int)} method.
     * 
     * @throws Exception if something bad happens
     */
    @SuppressWarnings("unchecked")
    @Test
    public void testGetBestEdge() throws Exception {
        final G simpleGrammar = (G) GrammarTestCase.createSimpleGrammar(grammarClass());
        final CellChart chart = new CellChart(2, simpleGrammar);
        final ChartCell parent = chart.getCell(0, 2);
        final ChartCell leftChild = chart.getCell(0, 1);
        final ChartCell rightChild = chart.getCell(1, 2);

        // Add one edge to the parent cell and replace it with one of higher probability
        parent.addEdge(chart.new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -1.5f), leftChild, rightChild, -1.5f), -1.5f);
        final ChartEdge bestNp = chart.new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f);
        parent.addEdge(bestNp, bestNp.inside);

        // And ensure it is _not_ replaced by one of lower probability
        parent.addEdge(chart.new ChartEdge(simpleGrammar.new Production("NP", "NP", "NN", -0.6f), leftChild, rightChild, -0.6f), -0.6f);

        // And a unary production (TOP -> NP)
        final ChartEdge top = chart.new ChartEdge(simpleGrammar.new Production("TOP", "NP", -0.2f, false), leftChild, null, -0.2f);
        parent.addEdge(top, top.inside);

        assertEquals(bestNp, parent.getBestEdge(simpleGrammar.mapNonterminal("NP")));
        assertEquals(top, parent.getBestEdge(simpleGrammar.mapNonterminal("TOP")));
    }
}
