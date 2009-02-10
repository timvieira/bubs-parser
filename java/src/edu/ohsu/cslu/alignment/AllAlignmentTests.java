package edu.ohsu.cslu.alignment;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import edu.ohsu.cslu.alignment.multiple.ProfileMultipleSequenceAligners;
import edu.ohsu.cslu.alignment.multiple.TestMultipleSequenceAligners;
import edu.ohsu.cslu.alignment.multiple.TestSequenceAlignment;
import edu.ohsu.cslu.alignment.pairwise.TestPairwiseAligners;
import edu.ohsu.cslu.alignment.pairwise.TestPairwiseLinguisticAlignment;
import edu.ohsu.cslu.alignment.pssm.ProfileGlobalSequenceAligners;
import edu.ohsu.cslu.alignment.pssm.TestPssmAligners;


/**
 * Alignment test suite.
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
@RunWith(Suite.class)
@Suite.SuiteClasses( {TestSimpleVocabulary.class, TestPssmAligners.class,
                      ProfileGlobalSequenceAligners.class, TestSequenceAlignment.class,
                      TestMultipleSequenceAligners.class, ProfileMultipleSequenceAligners.class,
                      TestPairwiseAligners.class, TestPairwiseLinguisticAlignment.class})
public class AllAlignmentTests
{}
