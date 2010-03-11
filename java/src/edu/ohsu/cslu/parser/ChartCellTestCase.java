package edu.ohsu.cslu.parser;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.ParameterizedType;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;

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
        final Chart chart = new Chart(2, ArrayChartCell.class, simpleGrammar);
        final ChartCell parent = chart.getCell(0, 2);
        final ChartCell leftChild = chart.getCell(0, 1);
        final ChartCell rightChild = chart.getCell(1, 2);

        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f));
        assertEquals(1, parent.numEdgesAdded);
        assertEquals(1, parent.numEdgesConsidered);

        // Try adding another edge with lower probability. The count of edges considered should increment, but
        // not the count of edges added.
        parent.addEdge(simpleGrammar.new Production("NP", "NP", "NN", -0.6f), leftChild, rightChild, -0.6f);
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
        final Chart chart = new Chart(2, ArrayChartCell.class, simpleGrammar);
        final ChartCell parent = chart.getCell(0, 2);
        final ChartCell leftChild = chart.getCell(0, 1);
        final ChartCell rightChild = chart.getCell(1, 2);

        // Add one edge to the parent cell and replace it with one of higher probability
        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -1.5f), leftChild, rightChild, -1.5f));
        final ChartEdge bestNp = new ChartEdge(simpleGrammar.new Production("NP", "NN", "NN", -0.5f), leftChild, rightChild, -0.5f);
        parent.addEdge(bestNp);

        // And ensure it is _not_ replaced by one of lower probability
        parent.addEdge(new ChartEdge(simpleGrammar.new Production("NP", "NP", "NN", -0.6f), leftChild, rightChild, -0.6f));

        // And a unary production (TOP -> NP)
        final ChartEdge top = new ChartEdge(simpleGrammar.new Production("TOP", "NP", -0.2f, false), leftChild, null, -0.2f);
        parent.addEdge(top);

        assertEquals(bestNp, parent.getBestEdge(simpleGrammar.mapNonterminal("NP")));
        assertEquals(top, parent.getBestEdge(simpleGrammar.mapNonterminal("TOP")));
    }
}
