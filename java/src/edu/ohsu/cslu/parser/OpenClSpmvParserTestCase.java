package edu.ohsu.cslu.parser;

import java.io.Reader;

import org.junit.Ignore;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.DefaultFunction;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;

/**
 * Tests for {@link OpenClSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class OpenClSpmvParserTestCase extends SparseMatrixVectorParserTestCase {

    @Override
    protected Class<? extends Grammar> grammarClass() {
        return CsrSparseMatrixGrammar.class;
    }

    @Override
    protected Grammar createGrammar(final Reader grammarReader, final Reader lexiconReader) throws Exception {
        return grammarClass().getConstructor(
            new Class[] { Reader.class, Reader.class, GrammarFormatType.class, Class.class }).newInstance(
            new Object[] { grammarReader, lexiconReader, GrammarFormatType.CSLU, DefaultFunction.class });
    }

    @Override
    @Ignore("OpenCL Parser does not currently implement filtering")
    public void testFilteredCrossProductVectorSimpleGrammar2() throws Exception {
    }

}
