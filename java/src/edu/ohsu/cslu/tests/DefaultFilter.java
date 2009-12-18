package edu.ohsu.cslu.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * The default test filter; includes normal unit and integration tests, but excludes tests specifically
 * annotated as performance tests.
 * 
 * @author aarond
 * @since Dec 30, 2008
 * 
 * @version $Revision$
 */
public class DefaultFilter extends Filter {

    @Override
    public String describe() {
        return "Excludes performance tests";
    }

    @Override
    public boolean shouldRun(Description description) {
        Description methodDescription = description.getChildren().get(0);

        // Run everything not annotated as a performance test
        if ((description.getAnnotation(PerformanceTest.class) != null)
                || methodDescription.getAnnotation(PerformanceTest.class) != null) {
            return false;
        }

        return true;
    }

}
