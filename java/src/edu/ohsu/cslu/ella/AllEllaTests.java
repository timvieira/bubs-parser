package edu.ohsu.cslu.ella;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ TestStringCountGrammar.class, TestMappedCountGrammar.class, TestProductionListGrammar.class })
public class AllEllaTests {

}
