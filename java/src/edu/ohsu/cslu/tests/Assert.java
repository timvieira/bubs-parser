package edu.ohsu.cslu.tests;

import static org.junit.Assert.fail;

/**
 * JUnit assertion utility methods.
 * 
 * @author Aaron Dunlop
 * @since Feb 7, 2011
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class Assert {

    /**
     * Asserts that two doubles or floats are equal to within a positive delta. If they are not, an
     * {@link AssertionError} is thrown. The values are expected to be natural logs of probabilities, and the failure
     * message is formulated as a fraction. e.g. assertLogFractionEquals(Math.log(.5), Math.log(.25), .01) should return
     * "expected 1/2 but was 1/4".
     * 
     * If the expected value is infinity then the delta value is ignored. NaNs are considered equal:
     * <code>assertEquals(Double.NaN, Double.NaN, *)</code> passes
     * 
     * @param expected expected value
     * @param actual the value to check against <code>expected</code>
     * @param delta the maximum delta between <code>expected</code> and <code>actual</code> for which both numbers are
     *            still considered equal.
     */
    public static void assertLogFractionEquals(final double expected, final double actual, final double delta) {
        if (Double.compare(expected, actual) == 0) {
            return;
        }

        if (!(Math.abs(expected - actual) <= delta)) {
            fail("expected log(" + fraction(expected) + ") but was log(" + fraction(actual) + ")");
        }
    }

    private static String fraction(final double logProbability) {
        if (logProbability == 0f) {
            return "1";
        }
        if (logProbability == Double.NEGATIVE_INFINITY) {
            return "0";
        }
        return "1/" + (int) java.lang.Math.round(java.lang.Math.exp(-1.0 * logProbability));
    }
}
