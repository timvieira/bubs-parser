package edu.ohsu.cslu.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

/**
 * An alternate test filter, which runs all tests regardless of their annotations.
 * 
 * @author Aaron Dunlop
 * @since Jan 8, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class AllTestFilter extends Filter {

    @Override
    public String describe() {
        return "Includes all tests, regardless of their annotations";
    }

    @Override
    public boolean shouldRun(Description description) {
        return true;
    }
}
