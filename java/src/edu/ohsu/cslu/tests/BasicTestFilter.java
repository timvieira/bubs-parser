package edu.ohsu.cslu.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * Basic test filter; includes normal unit and integration tests, but excludes performance tests (@link PerformanceTest}) and detailed tests ({@link DetailedTest}.
 * 
 * @author aarond
 * @since Dec 30, 2008
 * 
 * @version $Revision$
 */
public class BasicTestFilter extends Filter {

    @Override
    public String describe() {
        return "Excludes performance and detailed tests";
    }

    @Override
    public boolean shouldRun(final Description description) {
        final Description methodDescription = description.getChildren().get(0);

        // Run everything not annotated as a performance or detailed test
        if (description.getAnnotation(PerformanceTest.class) != null || methodDescription.getAnnotation(PerformanceTest.class) != null
                || description.getAnnotation(DetailedTest.class) != null || methodDescription.getAnnotation(DetailedTest.class) != null) {
            return false;
        }

        return true;
    }
}
