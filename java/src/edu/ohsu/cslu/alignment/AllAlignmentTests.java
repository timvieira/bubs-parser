package edu.ohsu.cslu.alignment;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.alignment.column.ProfileGlobalSequenceAligners;
import edu.ohsu.cslu.alignment.column.TestColumnAligners;
import edu.ohsu.cslu.alignment.multiple.ProfileMultipleSequenceAligners;
import edu.ohsu.cslu.alignment.multiple.TestMultipleSequenceAligners;
import edu.ohsu.cslu.alignment.multiple.TestMultipleSequenceAlignment;
import edu.ohsu.cslu.alignment.pairwise.TestPairwiseAligners;
import edu.ohsu.cslu.alignment.pairwise.TestPairwiseLinguisticAlignment;
import edu.ohsu.cslu.alignment.tools.TestInduceMappedVocabularies;

/**
 * Alignment test suite.
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( {TestSimpleVocabulary.class, TestLogLinearVocabulary.class, TestColumnAligners.class,
                      ProfileGlobalSequenceAligners.class, TestMultipleSequenceAlignment.class,
                      TestMultipleSequenceAligners.class, ProfileMultipleSequenceAligners.class,
                      TestPairwiseAligners.class, TestPairwiseLinguisticAlignment.class,
                      TestInduceMappedVocabularies.class})
public class AllAlignmentTests
{}
