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

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * @author Aaron Dunlop
 * @since Dec 3, 2012
 */
public class LimitedSpanOHSUCellConstraintsModel extends OHSUCellConstraintsModel {

    private static final long serialVersionUID = 1L;

    private final static boolean ALLOW_COMPLETE_ABOVE_LIMIT = GlobalConfigProperties.singleton().getBooleanProperty(
            ParserDriver.OPT_ALLOW_COMPLETE_ABOVE_SPAN_LIMIT, false);

    private final int maxSubtreeSpan;

    /**
     * @param modelStream
     */
    public LimitedSpanOHSUCellConstraintsModel(final BufferedReader modelStream, final int maxSubtreeSpan) {
        super(modelStream, null);
        this.maxSubtreeSpan = maxSubtreeSpan;
    }

    @Override
    public CellSelector createCellSelector() {
        return new LimitedSpanOHSUCellConstraints();
    }

    public class LimitedSpanOHSUCellConstraints extends OHSUCellConstraintsModel.OHSUCellConstraints {

        public LimitedSpanOHSUCellConstraints() {
            super(null);
        }

        private short sentenceLength;
        private Binarization binarization;

        @Override
        public void initSentence(final ChartParser<?, ?> p, final ParseTask task) {
            super.initSentence(p, task);

            sentenceLength = (short) p.chart.size();
            binarization = p.grammar.binarization();
        }

        @Override
        public boolean isCellOpen(final short start, final short end) {
            if (end - start > maxSubtreeSpan && end - start < sentenceLength) {
                // Left periphery only
                if (binarization == Binarization.LEFT && start != 0) {
                    return false;
                    // Right periphery only
                } else if (binarization == Binarization.RIGHT && end != sentenceLength) {
                    return false;
                }
            }

            return super.isCellOpen(start, end);
        }

        @Override
        public boolean isCellOnlyFactored(final short start, final short end) {
            if (!ALLOW_COMPLETE_ABOVE_LIMIT && (end - start) > maxSubtreeSpan && (end - start) < sentenceLength) {
                return true;
            }

            return super.isCellOnlyFactored(start, end);
        }
    }
}
