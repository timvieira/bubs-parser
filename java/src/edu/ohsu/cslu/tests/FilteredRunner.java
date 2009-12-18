package edu.ohsu.cslu.tests;

import java.util.Arrays;

import junit.framework.AssertionFailedError;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.RunnerBuilder;

/**
 * A custom JUnit runner that filters tests based on annotations. ({@link PerformanceTest}, etc.) An
 * implementation of the {@link Filter} interface is created based on the system property 'junit.filter.class'
 * and filters individual test methods at runtime. If the system property is not specified,
 * {@link DefaultFilter} is created, which runs all tests except for those labeled as performance tests.
 * 
 * Tests which are filtered out are marked as ignored. Similar functionality could be achieved using JUnit
 * assumptions, but a failed assumption is marked as a successful test, which seems odd, and hides the fact
 * that the test did not actually execute.
 * 
 * @author aarond
 * @since Dec 30, 2008
 * 
 * @version $Revision$
 */
public class FilteredRunner extends BlockJUnit4ClassRunner {

    public final static String FILTER_CLASS_PROPERTY = "junit.filter.class";
    private final Filter filter;

    public FilteredRunner(Class<?> testClass, RunnerBuilder builder) throws InitializationError {
        super(testClass);

        // Create a filter based on the class specified in a system property
        String filterClass = System.getProperty(FILTER_CLASS_PROPERTY);
        if (filterClass != null) {
            try {
                filter = (Filter) Class.forName(filterClass).newInstance();
            } catch (Exception e) {
                throw new InitializationError(Arrays.asList(new Throwable[] { e }));
            }
        } else {
            filter = new DefaultFilter();
        }
    }

    /**
     * This implementation should work, but there's a bug in the JUnit test runner that causes ignored classes
     * to be marked as 'Unrooted'. So we'll just work around it in runChild()
     */
    // @Override
    // public void run(RunNotifier notifier)
    // {
    // Class<?> javaClass = getTestClass().getJavaClass();
    // Description.createSuiteDescription(javaClass.getName(), javaClass.getAnnotations());
    // if (filter.shouldRun(getDescription()))
    // {
    // super.run(notifier);
    // }
    // else
    // {
    // notifier.fireTestIgnored(Description.createSuiteDescription(javaClass.getName()));
    // }
    // }
    @Override
    protected void runChild(FrameworkMethod method, RunNotifier notifier) {
        /*
         * TODO: This checks the class on each test method, and marks the methods as ignored instead of the
         * entire class.
         * 
         * Overriding run() should allow us to mark the entire class as ignored when appropriate, but this
         * seems to result in classes labeled as 'Unrooted Tests' instead of marking them the same as classes
         * annotated with @Ignore. It appears this is a bug in the Eclipse JUnit runner, but that's an
         * important one, so we'll work around it this way.
         */

        Description classDescription = Description.createSuiteDescription(getTestClass().getJavaClass()
            .getName(), getTestClass().getJavaClass().getAnnotations());
        Description methodDescription = describeChild(method);
        classDescription.addChild(methodDescription);

        if (!filter.shouldRun(classDescription)) {
            notifier.fireTestIgnored(methodDescription);
            return;
        }
        PerformanceTest performanceTestAnnotation = methodDescription.getAnnotation(PerformanceTest.class);

        // Tests may be annotated as performance tests without an expected runtime, in which
        // case we'll run the test without timing it.
        if (performanceTestAnnotation == null || performanceTestAnnotation.value().length == 0) {
            super.runChild(method, notifier);
        } else {
            // Find expected time
            String testHardware = System.getProperty(PerformanceTest.TEST_HARDWARE);
            String[] annotationParams = performanceTestAnnotation.value();

            long expectedTime = 0;
            if (testHardware == null) {
                // Default to the first annotated value
                expectedTime = Long.parseLong(annotationParams[1]);
            } else {
                for (int i = 0; i < annotationParams.length; i = i + 2) {
                    if (annotationParams[i].equals(testHardware)) {
                        expectedTime = Long.parseLong(performanceTestAnnotation.value()[i + 1]);
                        break;
                    }
                }
                if (expectedTime == 0) {
                    notifier.fireTestFailure(new Failure(methodDescription, new AssertionFailedError(String
                        .format("No time annotated for test hardware '%s'", testHardware))));
                }
            }

            // Time the test run
            long startTime = System.currentTimeMillis();
            super.runChild(method, notifier);
            long elapsedTime = System.currentTimeMillis() - startTime;

            // If actual time differs from expected by more than 20%, fail the test.
            long difference = elapsedTime - expectedTime;
            if (Math.abs(((float) difference) / expectedTime) > .2f) {
                notifier.fireTestFailure(new Failure(methodDescription, new AssertionFailedError(String
                    .format("Expected elapsed time within 20%% of %d but was %d instead", expectedTime,
                        elapsedTime))));
            }

            // TODO: Output test name and time?
        }
    }
}
