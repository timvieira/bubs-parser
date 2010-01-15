package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.ParameterizedType;

import org.junit.Test;

import edu.ohsu.cslu.grammar.BaseGrammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.grammar.BaseGrammar.Production;

/**
 * Base test class for testing {@link BaseChartCell} implementations.
 * 
 * @author Aaron Dunlop
 * @since Jan 4, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class ChartCellTestCase<C extends ChartCell, G extends BaseGrammar> {

    private ChartCell createChartCell(final int start, final int end, final G grammar) throws Exception {

        @SuppressWarnings("unchecked")
        final Class<C> c = (Class<C>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];

        return c.getConstructor(new Class[] { int.class, int.class, grammar.getClass() }).newInstance(new Object[] { start, end, grammar });
    }

    @SuppressWarnings("unchecked")
    private Class<G> grammarClass() {

        return (Class<G>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[1];
    }

    /**
     * Tests the {@link BaseChartCell#addEdge(ChartEdge)} and {@link BaseChartCell#addEdge(edu.ohsu.cslu.grammar.BaseGrammar.Production, float, BaseChartCell, BaseChartCell)}
     * methods.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testAddEdge() throws Exception {
        final G simpleGrammar = (G) GrammarTestCase.createSimpleGrammar(grammarClass());
        final BaseChartCell parent = (BaseChartCell) createChartCell(0, 1, simpleGrammar);
        final BaseChartCell leftChild = (BaseChartCell) createChartCell(0, 0, simpleGrammar);
        final BaseChartCell rightChild = (BaseChartCell) createChartCell(1, 1, simpleGrammar);

        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f));
        parent.addEdge(simpleGrammar.new Production("NN", "NN", "NN", -0.6f), -0.6f, leftChild, rightChild);
        assertEquals(2, parent.numEdgesAdded);
        assertEquals(2, parent.numEdgesConsidered);

        // Try adding another edge with lower probability. The count of edges considered should increment, but
        // not the count of edges added.
        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NN", "NP", -1.5f), leftChild, rightChild, -1.5f));
        parent.addEdge(simpleGrammar.new Production("NN", "NN", "NP", -1.6f), -1.6f, leftChild, rightChild);
        assertEquals(2, parent.numEdgesAdded);
        assertEquals(4, parent.numEdgesConsidered);
    }

    /**
     * Tests the {@link BaseChartCell#getBestEdge(int)} method.
     * 
     * @throws Exception if something bad happens
     */
    @Test
    public void testGetBestEdge() throws Exception {
        final G simpleGrammar = (G) GrammarTestCase.createSimpleGrammar(grammarClass());
        final BaseChartCell parent = (BaseChartCell) createChartCell(0, 1, simpleGrammar);
        final BaseChartCell leftChild = (BaseChartCell) createChartCell(0, 0, simpleGrammar);
        final BaseChartCell rightChild = (BaseChartCell) createChartCell(1, 1, simpleGrammar);

        // Add one edge to the parent cell and replace it with one of higher probability
        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NN", "NP", -1.5f), leftChild, rightChild, -1.5f));
        final ChartEdge bestNp = new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f);
        parent.addEdge(bestNp);

        // Add another edge to the parent cell and ensure it is _not_ replaced by one of lower probability
        final Production nnProduction = simpleGrammar.new Production("NN", "NN", "NN", -0.6f);
        final ChartEdge bestNn = new ChartEdge(nnProduction, leftChild, rightChild, -0.6f);
        parent.addEdge(nnProduction, -0.6f, leftChild, rightChild);
        parent.addEdge(simpleGrammar.new Production("NN", "NN", "NP", -1.6f), -1.6f, leftChild, rightChild);

        // And a unary production (TOP -> NP)
        final ChartEdge top = new ChartEdge(simpleGrammar.new Production("TOP", "NP", -0.2f, false), leftChild, null, -0.2f);
        parent.addEdge(top);

        assertEquals(bestNp, parent.getBestEdge(simpleGrammar.getNonTermIndex("NP")));
        assertEquals(bestNn, parent.getBestEdge(simpleGrammar.getNonTermIndex("NN")));
        assertEquals(top, parent.getBestEdge(simpleGrammar.getNonTermIndex("TOP")));
    }
}
