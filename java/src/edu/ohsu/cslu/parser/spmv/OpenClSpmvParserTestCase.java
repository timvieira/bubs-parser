package edu.ohsu.cslu.parser.spmv;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import edu.ohsu.cslu.grammar.SparseMatrixGrammar.CartesianProductFunction;
import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Tests for {@link OpenClSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class OpenClSpmvParserTestCase<P extends OpenClSpmvParser<? extends ParallelArrayChart>, C extends CartesianProductFunction>
        extends SparseMatrixVectorParserTestCase<P, C> {

    @BeforeClass
    public static void checkOpenCL() throws Exception {

        // Verify that we can load the JavaCL library; ignore tests of OpenCL parsers on platforms that do not
        // support OpenCL
        try {
            createBestContext();
        } catch (final Throwable t) {
            org.junit.Assume.assumeNoException(t);
        }
        f2_21_grammar = null;
        simpleGrammar1 = null;
        simpleGrammar2 = null;
    }

    /**
     * OpenCL parsers must use a simple shift function, since we don't (yet) implement the more complex hashing in
     * OpenCL code. So we override setUp() and tearDown() to null out the grammar and force re-creation, and override
     * createGrammar() to create a grammar implementation using a simpler CPF.
     */
    @AfterClass
    public static void suiteTearDown() throws Exception {
        f2_21_grammar = null;
        simpleGrammar1 = null;
        simpleGrammar2 = null;
    }

    // @Override
    // @Ignore("OpenCL Parsers do not currently implement filtering")
    // public void testFilteredCartesianProductVectorSimpleGrammar2() throws Exception {
    // }

}
