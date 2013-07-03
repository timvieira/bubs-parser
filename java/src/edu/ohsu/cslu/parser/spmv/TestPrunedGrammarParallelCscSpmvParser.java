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
package edu.ohsu.cslu.parser.spmv;

import java.io.IOException;
import java.io.Reader;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.TokenClassifier.TokenClassifierType;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Tests FOM-pruned parsing using both cell and grammar-level threading.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 */
public class TestPrunedGrammarParallelCscSpmvParser extends
        PrunedSparseMatrixParserTestCase<LeftCscSparseMatrixGrammar> {

    @Override
    protected LeftCscSparseMatrixGrammar createGrammar(final Reader grammarReader,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        return new LeftCscSparseMatrixGrammar(grammarReader, TokenClassifierType.DecisionTree, packingFunctionClass);
    }

    @Override
    protected PackedArraySpmvParser<LeftCscSparseMatrixGrammar> createParser(final ParserDriver opts,
            final LeftCscSparseMatrixGrammar grammar) {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CELL_THREAD_COUNT, "2");
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_GRAMMAR_THREAD_COUNT, "4");
        return new GrammarParallelCscSpmvParser(opts, grammar);
    }

}
