package edu.ohsu.cslu.parser.spmv;

import java.io.IOException;
import java.io.Reader;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
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
public class TestPrunedBeamCsrSpmvParser extends PrunedSpmvParserTestCase<CsrSparseMatrixGrammar> {

    @Override
    protected CsrSparseMatrixGrammar createGrammar(final Reader grammarReader,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        return new CsrSparseMatrixGrammar(grammarReader, packingFunctionClass);
    }

    @Override
    protected PackedArraySpmvParser<CsrSparseMatrixGrammar> createParser(final ParserDriver opts,
            final CsrSparseMatrixGrammar grammar) {
        return new CsrSpmvParser(opts, grammar);
    }
}
