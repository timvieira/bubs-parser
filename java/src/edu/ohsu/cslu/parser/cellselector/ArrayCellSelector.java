package edu.ohsu.cslu.parser.cellselector;

import java.util.Iterator;

import edu.ohsu.cslu.parser.ChartParser;

/**
 * Base class for {@link CellSelector} implementations which store the list of cells in a primitive array
 * 
 * @author Aaron Dunlop
 */
public abstract class ArrayCellSelector extends CellSelector {

    protected boolean constraintsEnabled = true;

    protected short[][] cellIndices;
    private int nextCell = 0;
    protected int openCells;

    // TODO: If we really need a parser instance to call parser.waitForActiveTasks(), then
    // this should be passed in when the model is created, not at each sentence init.
    protected ChartParser<?, ?> parser;

    protected ArrayCellSelector() {
    }

    @Override
    public short[] next() {
        return cellIndices[nextCell++];
    }

    @Override
    public void initSentence(final ChartParser<?, ?> p) {
        this.parser = p;
        nextCell = 0;
    }

    @Override
    public boolean hasNext() {
        // In left-to-right and bottom-to-top traversal, each row depends on the row below. Wait for active
        // tasks (if any) before proceeding on to the next row and before returning false when parsing is complete.
        if (nextCell >= 1) {
            if (nextCell >= openCells) {
                parser.waitForActiveTasks();
                return false;
            }
            final int nextSpan = cellIndices[nextCell][1] - cellIndices[nextCell][0];
            final int currentSpan = cellIndices[nextCell - 1][1] - cellIndices[nextCell - 1][0];
            if (nextSpan > currentSpan) {
                parser.waitForActiveTasks();
            }
        }

        return nextCell < openCells;
    }

    @Override
    public void reset(final boolean enableConstraints) {
        this.constraintsEnabled = enableConstraints;
        nextCell = 0;
    }

    @Override
    public final Iterator<short[]> reverseIterator() {
        return new Iterator<short[]>() {

            private int nextCell = openCells;

            @Override
            public boolean hasNext() {
                return nextCell > 0;
            }

            @Override
            public short[] next() {
                return cellIndices[--nextCell];
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
}
