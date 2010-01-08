package edu.ohsu.cslu.parser;


/**
 * Represents a cell in a CYK chart
 * 
 * @author Aaron Dunlop
 * @since Jan 7, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface ChartCell {

    /**
     * @return The start of the span represented by this cell
     */
    public int start();

    /**
     * @return The end of the span represented by this cell
     */
    public int end();

    /**
     * Adds an edge to the cell if the edge's probability is greater than an existing edge with the same non-terminal. Optional operation (some {@link ChartCell} implementations
     * may be immutable).
     * 
     * @param edge Edge to be added
     * @return True if the edge was added, false if another edge with greater probability was already present.
     */
    public boolean addEdge(final ChartEdge edge);

    /**
     * @param nonterminal integer index of a non-terminal
     * @return the best edge in this cell for the specified non-terminal
     */
    public ChartEdge getBestEdge(final int nonterminal);

}
