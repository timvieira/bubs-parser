package edu.ohsu.cslu.parser.spmv;

import java.io.Reader;

import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.Grammar.GrammarFormatType;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashFilterFunction;
import edu.ohsu.cslu.tests.PerformanceTest;

/**
 * Tests for {@link CscSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestCscSpmvParser extends SparseMatrixVectorParserTestCase<CscSpmvParser> {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return LeftCscSparseMatrixGrammar.class;
    }

    @Override
    protected Grammar createGrammar(final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass().getConstructor(
                new Class[] { Reader.class, Reader.class, GrammarFormatType.class, Class.class }).newInstance(
                new Object[] { grammarReader, lexiconReader, GrammarFormatType.CSLU,
                        PerfectIntPairHashFilterFunction.class });
    }

    @Override
    @Test
    @PerformanceTest({ "mbp", "23541", "d820", "48282" })
    public void profileSentences11Through20() throws Exception {
        internalProfileSentences11Through20();
    }

}
