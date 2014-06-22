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

import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ParseTask;

/**
 * Classifies cells as 'open' or 'closed' (i.e., for consideration during inference).
 * 
 * @author Aaron Dunlop
 * @since Jun 28, 2013
 */
public interface CellClosureClassifier {

    /**
     * Initializes classifier data structures for a new sentence. {@link CellClosureClassifier} instances are not
     * expected to be thread-safe, but will generally be re-initialized and reused to process multiple sentences.
     * 
     * @param p
     * @param task
     */
    public void initSentence(final ChartParser<?, ?> p, final ParseTask task);

    /**
     * Returns true if the specified cell is 'open'.
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open'
     */
    public boolean isCellOpen(final short start, final short end);

    /**
     * Returns true if the specified cell is 'open' only to factored parents (i.e., will never be populated with a
     * complete constituent).
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' only to factored parents
     */
    public boolean isCellOnlyFactored(final short start, final short end);

    /**
     * Returns true if the specified cell is 'open' to unary productions.
     * 
     * @param start
     * @param end
     * @return true if the specified cell is 'open' to unary productions.
     */
    public boolean isUnaryOpen(final short start, final short end);

}
