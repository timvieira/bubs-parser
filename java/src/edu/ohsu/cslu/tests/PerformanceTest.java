package edu.ohsu.cslu.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * TODO: Document
 * 
 * @author Aaron Dunlop
 * @since Jan 7, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( {ElementType.METHOD, ElementType.TYPE})
public @interface PerformanceTest {
    /**
     * Different test machines will of course perform differently, so the specific machine running
     * the tests can be specified with a system property.
     * 
     * If, at runtime, no 'test.hardware' system property is specified, the first hardware
     * configuration in the annotation (@link {@link #value()} will be assumed.
     */
    public final static String TEST_HARDWARE = "test.hardware";

    /**
     * The configuration properties should be in pairs denoting the test hardware and the expected
     * runtime in milliseconds. e.g. {"macbook", "1250", "sun-x4150", "1028"}. If no configuration
     * properties are included in the annotation, the test will be classified and filtered as a
     * performance test, but no expectation about its runtime will be made.
     * 
     * @return performance test configuration properties
     */
    public String[] value() default {};
}
