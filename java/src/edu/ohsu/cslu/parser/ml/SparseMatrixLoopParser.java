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

import java.lang.reflect.ParameterizedType;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.parser.ParseTask;
import edu.ohsu.cslu.parser.ParserDriver;
import edu.ohsu.cslu.parser.SparseMatrixParser;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

public abstract class SparseMatrixLoopParser<G extends SparseMatrixGrammar, C extends ParallelArrayChart> extends
        SparseMatrixParser<G, C> {

    public SparseMatrixLoopParser(final ParserDriver opts, final G grammar) {
        super(opts, grammar);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void initSentence(final ParseTask parseTask) {
        final int sentLength = parseTask.sentenceLength();
        if (chart != null && chart.size() >= sentLength) {
            chart.reset(parseTask);
        } else {
            // Construct a chart of the appropriate type
            try {
                final Class<C> chartClass = ((Class<C>) ((ParameterizedType) getClass().getGenericSuperclass())
                        .getActualTypeArguments()[1]);
                try {
                    // First, try for a constructor that takes tokens, grammar, beamWidth, and lexicalRowBeamWidth
                    chart = chartClass.getConstructor(
                            new Class[] { ParseTask.class, SparseMatrixGrammar.class, int.class, int.class })
                            .newInstance(new Object[] { parseTask, grammar, beamWidth, lexicalRowBeamWidth });

                } catch (final NoSuchMethodException e) {
                    // If not found, use a constructor that takes only tokens and grammar
                    chart = chartClass.getConstructor(new Class[] { ParseTask.class, SparseMatrixGrammar.class })
                            .newInstance(new Object[] { parseTask, grammar });
                }
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

}
