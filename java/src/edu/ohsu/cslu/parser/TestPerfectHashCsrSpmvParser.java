package edu.ohsu.cslu.parser;

import static org.junit.Assert.fail;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.PerfectHashCsrSparseMatrixGrammar;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

public class TestPerfectHashCsrSpmvParser extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return PerfectHashCsrSparseMatrixGrammar.class;
    }

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new PerfectHashCsrSpmvParser((PerfectHashCsrSparseMatrixGrammar) grammar);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "35243", "d820", "75657" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

    @Override
    public void testUnfilteredCrossProductVectorSimpleGrammar2() throws Exception {
        // Not applicable to PerfectHashCsrSpmvParser
    }

    @Override
    public void testBinarySpMVMultiplySimpleGrammar2() throws Exception {
        fail("Not implemented correctly for PerfectHashCsrSpmvParser");
    }
}
