package edu.ohsu.cslu.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * This filter includes only tests annotated as performance tests and ignores all others.
 * 
 * @author aarond
 * @since Dec 30, 2008
 * 
 * @version $Revision$
 */
public class PerformanceTestFilter extends Filter {

    @Override
    public String describe() {
        return "Includes only performance tests";
    }

    @Override
    public boolean shouldRun(Description description) {
        Description methodDescription = description.getChildren().get(0);

        // Only run performance tests
        if ((description.getAnnotation(PerformanceTest.class) != null)
                || methodDescription.getAnnotation(PerformanceTest.class) != null) {
            return true;
        }

        return false;
    }
}
