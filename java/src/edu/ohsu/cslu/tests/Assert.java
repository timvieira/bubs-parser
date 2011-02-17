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

    /**
     * Returns a string representation of a rational approximation of the exponentiation of a real.
     * 
     * Performs brute-force search for numerators from 1 to 20. If no approximation is found within 0.01 of the
     * specified value, returns the closest approximation with a numerator of 1.
     * 
     * This hackish approximation is useful for small fractions (3/16, 5/12, etc.) and for small probabilities, which
     * can often be well-represented by a numerator of 1 and an appropriate denominator (e.g. 1/12385)
     * 
     * @param logProbability
     * @return
     */
    private static String fraction(final double logProbability) {
        if (logProbability == 0f) {
            return "1";
        }
        if (logProbability == Double.NEGATIVE_INFINITY) {
            return "0";
        }

        final double exp = java.lang.Math.exp(-1.0 * logProbability);
        for (int numerator = 1; numerator <= 20; numerator++) {
            final double denominator = exp * numerator;
            if (java.lang.Math.abs(denominator - java.lang.Math.round(denominator)) <= 0.01) {
                return String.format("%d/%d", numerator, java.lang.Math.round(denominator));
            }
        }

        return String.format("1/%d", java.lang.Math.round(exp));
    }
}
