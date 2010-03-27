package edu.ohsu.cslu.util;

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
@Suite.SuiteClasses( { TestSort.TestRadixSort.class, TestSort.TestMergeSort.class, TestSort.TestBitonicSort.class, TestSort.TestFlashSort.class, TestSort.TestQuickSort.class,
        TestSerialCpuScanner.class, TestStrings.class })
public class AllUtilTests {
}
