package edu.ohsu.cslu.tests;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows test methods or classes to be annotated as 'detailed' tests, allowing shorter, cursory test runs and more advanced 'detailed' runs. This is useful for long-running test
 * suites; running a subset of such a suite can alert you quickly to basic problems late in the suite that otherwise might take several minutes to encounter. The assumption is that
 * the user will first run the suite with a filter that will only execute normal test methods and, if successful, will follow up that run with a full test run.
 * 
 * These annotations will only take effect when a test is run with {@link FilteredRunner}.
 * 
 * @author Aaron Dunlop
 * @since Jan 7, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@Retention(RetentionPolicy.RUNTIME)
@Target( { ElementType.METHOD, ElementType.TYPE })
public @interface DetailedTest {

}
