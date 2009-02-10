package edu.ohsu.cslu.common;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses( {TestSimpleSequence.class, TestSimpleMappedSequence.class, TestPorterStemmer.class})
public class AllCommonTests
{}
