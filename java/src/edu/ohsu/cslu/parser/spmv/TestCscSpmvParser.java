package edu.ohsu.cslu.parser.spmv;

import java.io.Reader;

import org.junit.Test;

import edu.ohsu.cslu.grammar.CscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.parser.Parser;
import edu.ohsu.cslu.parser.ParserOptions;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCscSpmvParser extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CscSparseMatrixGrammar.class;
    }

    @Override
    protected Grammar createGrammar(final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass().getConstructor(
            new Class[] { Reader.class, Reader.class, GrammarFormatType.class, Class.class }).newInstance(
            new Object[] { grammarReader, lexiconReader, GrammarFormatType.CSLU,
                    PerfectIntPairHashFilterFunction.class });
    }

    @Override
    protected Parser<?> createParser(final Grammar grammar, final CellSelector cellSelector) {
        return new CscSpmvParser((CscSparseMatrixGrammar) grammar);
    }

    @Override
    @Test
    @PerformanceTest( { "mbp", "23541", "d820", "100329" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}
