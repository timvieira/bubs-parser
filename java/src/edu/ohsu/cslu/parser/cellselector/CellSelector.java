/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.parser.cellselector;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.cellselector.DepGraphCellSelectorModel.DepGraphCellSelector;
import edu.ohsu.cslu.parser.chart.Chart;

/**
 * Iterates through open cells in a chart. Some implementations (e.g. {@link LeftRightBottomTopTraversal}) use a simple
 * and predictable iteration order; others implement cell constraints (see Roark and Hollingshead, 2008 and 2009) or
 * constrain iteration via a gold chart.
 */
public abstract class CellSelector implements CellClosureClassifier, Iterator<short[]> {

    /**
     * Enables constraints (if any) implemented by this {@link CellSelector}. If the parse fails, constraints may be
     * disabled on later reparsing passes.
     */
    protected boolean constraintsEnabled = true;

    protected short[] cellIndices;
    private int nextCell = 0;
    protected int openCells;

    // TODO: If we really need a parser instance to call parser.waitForActiveTasks(), then
    // this should be passed in when the model is created, not at each sentence init.
    protected ChartParser<?, ?> parser;
    protected ParseTask parseTask;

    protected DenseIntVector maxSpan = null;

    /**
     * Another cell selector with which this selector is intersected - i.e., a cell is open if and only if this selector
     * and the child (and any children of that child, recursively) open the cell
     */
    protected final CellSelector childCellSelector;

    protected CellSelector(final CellSelector child) {
        this.childCellSelector = child;
    }

    public short[] next() {
        short[] next = new short[] { cellIndices[nextCell << 1], cellIndices[(nextCell++ << 1) + 1] };
        if (childCellSelector != null) {
            while (!childCellSelector.isCellOpen(next[0], next[1])) {
                next = new short[] { cellIndices[nextCell << 1], cellIndices[(nextCell++ << 1) + 1] };
            }
        }
        return next;
    }

    @Override
    public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
        this.parser = p;
        this.parseTask = task;
        nextCell = 0;

        // Enable constraints at sentence initialization - if parsing fails, we may disable them for a later reparsing
        // pass
        this.constraintsEnabled = true;

        if (childCellSelector != null) {
            childCellSelector.initSentence(p, task);
        }
    }

    /**
     * Returns true if the cell selector has more cells available. The parser should call {@link #hasNext()} until it
     * returns <code>false</code> to ensure the sentence is fully parsed.
     * 
     * @return true if the cell selector has more cells.
     */
    @Override
    public boolean hasNext() {
        // In left-to-right and bottom-to-top traversal, each row depends on the row below. Wait for active
        // tasks (if any) before proceeding on to the next row and before returning false when parsing is complete.
        if (nextCell >= 1) {
            if (nextCell >= openCells) {
                parser.waitForActiveTasks();
                return false;
            }
            final int nextSpan = cellIndices[(nextCell << 1) + 1] - cellIndices[nextCell << 1];
            final int currentSpan = cellIndices[((nextCell - 1) << 1) + 1] - cellIndices[(nextCell - 1) << 1];
            if (nextSpan > currentSpan) {
                parser.waitForActiveTasks();
            }
        }

        return nextCell < openCells;
    }

    /**
     * This {@link Iterator} implementation does not support removal.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reset the {@link Iterator} to the first cell. Used for reparsing and in grammar estimation.
     */
    public void reset() {
        reset(true);
    }

    /**
     * Reset the {@link Iterator} to the first cell, optionally disabling cell constraints (e.g., for reparsing).
     * 
     * @param enableConstraints
     */
    public void reset(final boolean enableConstraints) {
        this.constraintsEnabled = enableConstraints;
        nextCell = 0;
    }

    /**
     * @return an iterator which supplies cells in the reverse order of this {@link CellSelector} (e.g. for populating
     *         outside probabilities in inside-outside parsing after a normal inside pass).
     */
    public final Iterator<short[]> reverseIterator() {
        return new Iterator<short[]>() {

            private int nextCell = openCells;

            @Override
            public boolean hasNext() {
                return nextCell > 0;
            }

            @Override
            public short[] next() {
                --nextCell;
                return new short[] { cellIndices[nextCell << 1], cellIndices[(nextCell << 1) + 1] };
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @throws IOException if the write fails
     */
    public void train(final BufferedReader inStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * @throws IOException if the write fails
     */
    public void writeModel(final BufferedWriter outStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    public boolean hasCellConstraints() {
        return constraintsEnabled;
    }

    @Override
    public boolean isCellOnlyFactored(final short start, final short end) {
        return false;
    }

    @Override
    public boolean isUnaryOpen(final short start, final short end) {
        return true;
    }

    public int getMidStart(final short start, final short end) {
        if ((end - start) < 2 || !isCellOnlyFactored(start, end) || isGrammarLeftBinarized())
            return start + 1;
        return end - 1; // only allow one midpoint
    }

    public int getMidEnd(final short start, final short end) {
        if ((end - start) < 2 || !isCellOnlyFactored(start, end) || !isGrammarLeftBinarized())
            return end - 1;
        return start + 1; // only allow one midpoint
    }

    /**
     * Returns the maximum span in which the specified cell can participate. Certain {@link CellSelector}
     * implementations (e.g. {@link DepGraphCellSelector}) identify subsequence spans and constrain the final chart to
     * be consistent with those bracketings. E.g., if an NP-chunker has identified noun phrases, we must build a
     * complete NP covering the identified span, but we need not consider larger spans that include part (and not all)
     * of that NP.
     * 
     * @param start
     * @param end
     * @return The maximum span in which the specified cell can participate.
     */
    public short getMaxSpan(final short start, final short end) {
        return maxSpan == null ? Short.MAX_VALUE : (short) maxSpan.getInt(parser.chart.cellIndex(start, end));
    }

    /**
     * Returns the beam width for the current cell. Consumers generally set the cell beam width to
     * java.lang.Math.min(getCelValue(), beamWidth), so they will not attempt to search a range larger than the maximum
     * beam width of the parser.
     * 
     * TODO The naming and interface still aren't great.
     */
    public int getBeamWidth(final int cellIndex) {
        return Integer.MAX_VALUE;
    }

    /**
     * Returns the beam width for the current cell. Consumers generally set the cell beam width to
     * java.lang.Math.min(getCelValue(), beamWidth), so they will not attempt to search a range larger than the maximum
     * beam width of the parser.
     * 
     * TODO The naming and interface still aren't great.
     */
    public int getBeamWidth(final short start, final short end) {
        return getBeamWidth(Chart.cellIndex(start, end, parseTask.sentenceLength()));
    }

    protected final boolean isGrammarLeftBinarized() {
        return parser.grammar.binarization() == Binarization.LEFT;
    }

    protected abstract static class ChainableCellSelector extends CellSelector {

        protected ChainableCellSelector(final CellSelector child) {
            super(child);
        }

        @Override
        public void reset(final boolean enableConstraints) {
            super.reset(enableConstraints);

            if (!enableConstraints) {
                // Replace cellIndices with all chart cells.
                final int sentenceLength = parser.chart.size();
                openCells = sentenceLength * (sentenceLength + 1) / 2;
                if (cellIndices == null || cellIndices.length < openCells * 2) {
                    cellIndices = new short[openCells * 2];
                }

                int i = 0;
                for (short span = 1; span <= sentenceLength; span++) {
                    for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                        cellIndices[i++] = start;
                        cellIndices[i++] = (short) (start + span);
                    }
                }
            }
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            if (childCellSelector != null && !childCellSelector.isCellOpen(start, end)) {
                return false;
            }

            // For now, just iterate through cells. We might decide to improve on this later, but it's only used when
            // combining multiple cell selectors, so it's not a high priority.
            final int span = end - start;
            for (int i = 0; i < cellIndices.length; i += 2) {
                final short start2 = cellIndices[i];
                final short end2 = cellIndices[i + 1];

                if (start2 == start && end2 == end) {
                    return true;
                } else if (end2 - end2 > span) {
                    return false;
                }
            }
            return false;
        }
    }
}
