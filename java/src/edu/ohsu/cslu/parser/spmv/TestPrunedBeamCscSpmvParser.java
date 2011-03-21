package edu.ohsu.cslu.parser.spmv;

import java.io.IOException;
import java.io.Reader;

import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;

/**
 * Tests FOM-pruned parsing, using row-level threading.
 * 
 * @author Aaron Dunlop
 * @since Mar 9, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestPrunedBeamCscSpmvParser extends PrunedSpmvParserTestCase<LeftCscSparseMatrixGrammar> {

    @Override
    protected LeftCscSparseMatrixGrammar createGrammar(final Reader grammarReader,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        return new LeftCscSparseMatrixGrammar(grammarReader, packingFunctionClass);
    }

    @Override
    protected PackedArraySpmvParser<LeftCscSparseMatrixGrammar> createParser(final ParserDriver opts,
            final LeftCscSparseMatrixGrammar grammar) {
        return new CscSpmvParser(opts, grammar);
    }
}
