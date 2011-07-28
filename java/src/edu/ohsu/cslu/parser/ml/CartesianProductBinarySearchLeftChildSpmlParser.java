/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */
package edu.ohsu.cslu.parser.ml;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.chart.PackedArrayChart;

/**
 * Exhaustive matrix-loop parser which performs grammar intersection by iterating over grammar rules matching the
 * observed child pairs in the cartesian product of non-terminals observed in child cells. Queries grammar using a
 * binary search.
 * 
 * @author Aaron Dunlop
 */
public class CartesianProductBinarySearchLeftChildSpmlParser extends CartesianProductBinarySearchSpmlParser {

    public CartesianProductBinarySearchLeftChildSpmlParser(final ParserDriver opts,
            final LeftCscSparseMatrixGrammar grammar) {
        super(opts, grammar);
    }

    @Override
    protected final int binarySearchStart(final int leftChild) {
        return grammar.cscBinaryLeftChildStartIndices[leftChild];
    }

    @Override
    protected final int binarySearchEnd(final int leftChild) {
        return grammar.cscBinaryLeftChildEndIndices[leftChild] + 1;
    }

    // Override default implementation, since this is a sub-subclass
    @Override
    protected void initSentence(final int[] tokens) {
        final int sentLength = tokens.length;
        if (chart != null && chart.size() >= sentLength) {
            chart.clear(sentLength);
        } else {
            chart = new PackedArrayChart(tokens, grammar, beamWidth, lexicalRowBeamWidth);
        }
        super.initSentence(tokens);
    }

}
