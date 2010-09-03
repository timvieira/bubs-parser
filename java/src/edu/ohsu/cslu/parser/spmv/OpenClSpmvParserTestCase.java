package edu.ohsu.cslu.parser.spmv;

import static com.nativelibs4java.opencl.JavaCL.createBestContext;

import org.junit.BeforeClass;
import org.junit.Ignore;

import edu.ohsu.cslu.parser.chart.ParallelArrayChart;

/**
 * Tests for {@link OpenClSpmvParser}.
 * 
 * @author Aaron Dunlop
 * @since Feb 11, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class OpenClSpmvParserTestCase<P extends OpenClSpmvParser<? extends ParallelArrayChart>> extends
        SparseMatrixVectorParserTestCase<P> {

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
