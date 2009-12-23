package edu.ohsu.cslu.tests;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.zip.GZIPInputStream;

import org.junit.Assert;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.alignment.AllAlignmentTests;
import edu.ohsu.cslu.common.AllCommonTests;
import edu.ohsu.cslu.counters.AllCounterTests;
import edu.ohsu.cslu.datastructs.AllDataStructureTests;
import edu.ohsu.cslu.matching.ProfileMatchers;
import edu.ohsu.cslu.matching.approximate.TestApproximateMatchers;
import edu.ohsu.cslu.matching.exact.TestExactMatchers;
import edu.ohsu.cslu.parser.AllParserTests;
import edu.ohsu.cslu.tools.AllToolTests;
import edu.ohsu.cslu.util.AllUtilTests;

/**
 * The entire regression suite for shared NLP code. This class also contains a number of static helper methods
 * used by other unit test classes.
 * 
 * @author Aaron Dunlop
 * @since Sep 22, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( { AllCommonTests.class, TestExactMatchers.class, TestApproximateMatchers.class,
        ProfileMatchers.class, AllAlignmentTests.class, AllCounterTests.class, AllDataStructureTests.class,
        AllToolTests.class, AllUtilTests.class, AllParserTests.class })
public class SharedNlpTests {

    public final static String UNIT_TEST_DIR = "unit-test-data/";
    public final static String SHARED_UNIT_TEST_DIR = "../shared-nlp-code/" + UNIT_TEST_DIR;

    /**
     * Returns a {@link Reader} reading the specified unit test file (from the shared unit test data
     * directory). Uncompresses gzip-compressed files transparently.
     * 
     * @param filename
     *            Unit test file
     * @return a Reader reading the specified unit test file
     * @throws IOException
     *             If unable to find or open the file
     */
    public static Reader unitTestDataAsReader(final String filename) throws IOException {
        return new InputStreamReader(unitTestDataAsStream(filename));
    }

    /**
     * Returns an {@link InputStream} reading the specified unit test file (from the shared unit test data
     * directory). Uncompresses gzip-compressed files transparently.
     * 
     * @param filename
     *            Unit test file
     * @return a InputStream reading the specified unit test file
     * @throws IOException
     *             If unable to find or open the file
     */
    public static InputStream unitTestDataAsStream(final String filename) throws IOException {
        try {
            InputStream is = new FileInputStream(SharedNlpTests.UNIT_TEST_DIR + filename);
            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return is;
        } catch (final FileNotFoundException e) {
            // A hack to read files in the shared unit test data directory from tests run within
            // other projects
            InputStream is = new FileInputStream(SharedNlpTests.SHARED_UNIT_TEST_DIR + filename);
            if (filename.endsWith(".gz")) {
                is = new GZIPInputStream(is);
            }
            return is;
        }
    }

    /**
     * Returns a {@link String} containing the contents of the specified unit test file (from the shared unit
     * test data directory). Uncompresses gzip-compressed files transparently.
     * 
     * @param filename
     *            Unit test file
     * @return a String containing the contents of the specified unit test file
     * @throws IOException
     *             If unable to find or open the file
     */
    public static String unitTestDataAsString(final String filename) throws IOException {
        return new String(readUnitTestData(filename));
    }

    // TODO Document, rename
    public static byte[] readUnitTestData(final String filename) throws IOException {
        return readUnitTestData(unitTestDataAsStream(filename));
    }

    private static byte[] readUnitTestData(final InputStream is) throws IOException {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream(1024);
        final byte[] buf = new byte[1024];
        for (int i = is.read(buf); i >= 0; i = is.read(buf)) {
            bos.write(buf, 0, i);
        }
        is.close();
        return bos.toByteArray();
    }

    public static void assertEquals(final String message, final float[][] expected, final float[][] actual,
            final float delta) {
        for (int i = 0; i < actual.length; i++) {
            for (int j = 0; j < actual[0].length; j++) {
                Assert.assertEquals(message, expected[i][j], actual[i][j], delta);
            }
        }
    }

    public static void assertEquals(final float[][] expected, final float[][] actual, final float delta) {
        assertEquals(null, expected, actual, delta);
    }
}
