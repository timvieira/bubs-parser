package edu.ohsu.cslu.counters;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( { TestCoocurrenceCounters.class, TestUnigramFrequencyCounter.class })
public class AllCounterTests {
}
