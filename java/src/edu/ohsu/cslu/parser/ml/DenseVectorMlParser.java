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
package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.DenseVectorChart;

/**
 * Base class for all matrix-loop parsers which use a dense vector chart structure ({@link DenseVectorChart}).
 * 
 * @author Aaron Dunlop
 */
public abstract class DenseVectorMlParser<G extends SparseMatrixGrammar> extends
        SparseMatrixLoopParser<G, DenseVectorChart> {

    public DenseVectorMlParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }
}
