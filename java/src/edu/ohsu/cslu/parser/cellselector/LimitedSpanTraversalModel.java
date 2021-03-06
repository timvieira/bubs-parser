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

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Performs a standard left-to-right, bottom-up traversal, but limited to a specific height (span). After reaching that
 * maximum span, the traversal only considers cells on the periphery of the chart up to the top cell (the left periphery
 * for left binarized grammars, and the right for right-binarized).
 * 
 * By default, in cells above the maximum span and below the top cell, only factored categories are permitted, but
 * populating complete categories can be enabled with the configuration option
 * {@link ParserDriver#OPT_ALLOW_COMPLETE_ABOVE_SPAN_LIMIT}.
 * 
 * @author Aaron Dunlop
 * @since Nov 19, 2012
 */
public class LimitedSpanTraversalModel extends ChainableCellSelectorModel implements CellSelectorModel {

    private final static long serialVersionUID = 1L;

    private final static boolean ALLOW_COMPLETE_ABOVE_LIMIT = GlobalConfigProperties.singleton().getBooleanProperty(
            ParserDriver.OPT_ALLOW_COMPLETE_ABOVE_SPAN_LIMIT, false);

    private final int maxSubtreeSpan;
    private short sentenceLength;
    private Binarization binarization;

    public LimitedSpanTraversalModel(final int maxSubtreeSpan, final CellSelectorModel childModel) {
        super(childModel);
        this.maxSubtreeSpan = maxSubtreeSpan;
    }

    @Override
    public CellSelector createCellSelector() {
        return new LimitedSpanTraversal(childModel != null ? childModel.createCellSelector() : null);
    }

    public class LimitedSpanTraversal extends CellSelector {

        public LimitedSpanTraversal(final CellSelector child) {
            super(child);
        }

        @Override
        public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
            super.initSentence(p, task);

            sentenceLength = (short) p.chart.size();
            binarization = p.grammar.binarization();

            if (sentenceLength > maxSubtreeSpan) {
                final int excludedCells = ((sentenceLength - maxSubtreeSpan) * (sentenceLength - maxSubtreeSpan + 1))
                        / 2 - (sentenceLength - maxSubtreeSpan);
                openCells = sentenceLength * (sentenceLength + 1) / 2 - excludedCells;
            } else {
                openCells = sentenceLength * (sentenceLength + 1) / 2;
            }

            if (cellIndices == null || cellIndices.length < openCells * 2) {
                cellIndices = new short[openCells * 2];
            }

            if (sentenceLength <= maxSubtreeSpan) {
                // Normal left-right bottom-up traversal
                for (short span = 1, i = 0; span <= sentenceLength; span++) {
                    for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                        cellIndices[i++] = start;
                        cellIndices[i++] = (short) (start + span);
                    }
                }

            } else {

                short i = 0;
                // Normal left-right bottom-up traversal up to span length
                for (short span = 1; span <= maxSubtreeSpan; span++) {
                    for (short start = 0; start < sentenceLength - span + 1; start++) { // beginning
                        cellIndices[i++] = start;
                        cellIndices[i++] = (short) (start + span);
                    }
                }

                for (short span = (short) (maxSubtreeSpan + 1); span <= sentenceLength; span++) {
                    if (binarization == Binarization.LEFT) {
                        // Left periphery only
                        cellIndices[i++] = 0;
                        cellIndices[i++] = span;
                    } else {
                        // Right periphery only
                        cellIndices[i++] = (short) (sentenceLength - span);
                        cellIndices[i++] = sentenceLength;
                    }

                }
            }
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {

            if (childCellSelector != null && !childCellSelector.isCellOpen(start, end)) {
                return false;
            }

            return (end - start <= maxSubtreeSpan) // Allowed spans
                    || (binarization == Binarization.LEFT && start == 0) // Left periphery
                    || (binarization == Binarization.RIGHT && end == sentenceLength); // Right periphery
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            if (ALLOW_COMPLETE_ABOVE_LIMIT) {
                return false;
            }

            // All cells above the maximum span are factored-only, except for the top cell
            return (end - start > maxSubtreeSpan) && (end - start < sentenceLength);
        }
    }
}
