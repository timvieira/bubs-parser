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

import java.io.Reader;

import org.cjunit.PerformanceTest;
import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.TokenClassifier.TokenClassifierType;
import edu.ohsu.cslu.parser.ecp.ExhaustiveChartParserTestCase;

public class TestCartesianProductHashSpmlParser extends ExhaustiveChartParserTestCase<CartesianProductHashSpmlParser> {

    private final static Class<? extends PackingFunction> PACKING_FUNCTION_CLASS = SparseMatrixGrammar.PerfectIntPairHashPackingFunction.class;

    @Override
    @Test
    @PerformanceTest({ "mbp", "7974", "mbp2012", "3914" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    public Grammar createGrammar(final Reader grammarReader) throws Exception {
        return grammarClass().getConstructor(new Class[] { Reader.class, TokenClassifierType.class, Class.class })
                .newInstance(new Object[] { grammarReader, TokenClassifierType.DecisionTree, PACKING_FUNCTION_CLASS });
    }

    /**
     * Ensure the grammar is constructed with the Constructs the grammar (if necessary) and a new parser instance. Run
     * prior to each test method.
     * 
     * @throws Exception if unable to construct grammar or parser.
     */
    @Override
    @Before
    public void setUp() throws Exception {
        if (f2_21_grammar != null
                && (f2_21_grammar.getClass() != grammarClass() || ((SparseMatrixGrammar) f2_21_grammar)
                        .packingFunction().getClass() != PACKING_FUNCTION_CLASS)) {
            f2_21_grammar = null;
        }

        if (simpleGrammar1 != null
                && (simpleGrammar1.getClass() != grammarClass() || ((SparseMatrixGrammar) simpleGrammar1)
                        .packingFunction().getClass() != PACKING_FUNCTION_CLASS)) {
            simpleGrammar1 = null;
        }

        if (simpleGrammar2 != null
                && (simpleGrammar2.getClass() != grammarClass() || ((SparseMatrixGrammar) simpleGrammar2)
                        .packingFunction().getClass() != PACKING_FUNCTION_CLASS)) {
            simpleGrammar2 = null;
        }

        super.setUp();
    }
}
