package edu.ohsu.cslu.tests;

import org.junit.runner.Description;
import org.junit.runner.manipulation.Filter;

public class AllTestFilter extends Filter
{
    @Override
    public String describe()
    {
        return "Includes all tests, regardless of their annotations";
    }

    @Override
    public boolean shouldRun(Description description)
    {
        return true;
    }
}
