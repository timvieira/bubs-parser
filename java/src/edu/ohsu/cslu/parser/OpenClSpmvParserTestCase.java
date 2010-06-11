package edu.ohsu.cslu.parser;

import java.io.Reader;

import org.junit.BeforeClass;
import org.junit.Ignore;

import edu.ohsu.cslu.grammar.CsrSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.DefaultFunction;
import edu.ohsu.cslu.parser.ParserOptions.GrammarFormatType;
import static com.nativelibs4java.opencl.JavaCL.createBestContext;

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

    @BeforeClass
    public static void checkOpenCL() throws Exception {

        // Verify that we can load the JavaCL library; ignore tests of OpenCL parsers on platforms that do not
        // support OpenCL
        try {
            createBestContext();
        } catch (final Throwable t) {
            org.junit.Assume.assumeNoException(t);
        }
    }

    @Override
    @Ignore("OpenCL Parsers do not currently implement filtering")
    public void testFilteredCartesianProductVectorSimpleGrammar2() throws Exception {
    }

}
