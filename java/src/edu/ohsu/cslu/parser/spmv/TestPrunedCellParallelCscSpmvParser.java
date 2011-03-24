package edu.ohsu.cslu.parser.spmv;

import java.io.IOException;
import java.io.Reader;

import cltool4j.GlobalConfigProperties;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.parser.ParserDriver;

public class TestPrunedCellParallelCscSpmvParser extends PrunedSpmvParserTestCase<LeftCscSparseMatrixGrammar> {

    @Override
    protected LeftCscSparseMatrixGrammar createGrammar(final Reader grammarReader,
            final Class<? extends PackingFunction> packingFunctionClass) throws IOException {
        return new LeftCscSparseMatrixGrammar(grammarReader, packingFunctionClass);
    }

    @Override
    protected PackedArraySpmvParser<LeftCscSparseMatrixGrammar> createParser(final ParserDriver opts,
            final LeftCscSparseMatrixGrammar grammar) {
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_ROW_THREAD_COUNT, "2");
        GlobalConfigProperties.singleton().setProperty(ParserDriver.OPT_CELL_THREAD_COUNT, "4");
        return new CellParallelCscSpmvParser(opts, grammar);
    }

}
