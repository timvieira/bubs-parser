package edu.ohsu.cslu.tools;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * Util test suite.
 * 
 * @author Aaron Dunlop
 * @since Sep 25, 2008
 * 
 *        $Id$
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( {TestCalculateDistances.class, TestSelectFeatures.class})
public class AllToolTests
{}
