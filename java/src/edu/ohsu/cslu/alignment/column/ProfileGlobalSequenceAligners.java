package edu.ohsu.cslu.alignment.column;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;

@RunWith(FilteredRunner.class)
@PerformanceTest
public class ProfileGlobalSequenceAligners
{
    private final static String TEST_SET_FILE = "alignment/current_prokMSA_aligned.fasta.test10.set.gz";
    private final static String SMALL_TRAINING_SET_FILE = "alignment/current_prokMSA_aligned.fasta.train.set.small.gz";

    private static String TEST_SET;
    private static String SMALL_TRAINING_SET;

    @BeforeClass
    public static void suiteSetUp() throws IOException
    {
        TEST_SET = new String(SharedNlpTests.readUnitTestData(TEST_SET_FILE));
        SMALL_TRAINING_SET = new String(SharedNlpTests.readUnitTestData(SMALL_TRAINING_SET_FILE));
    }

    @Test
    @PerformanceTest( {"d820", "13970"})
    public void profileFullDynamicAligner() throws IOException
    {
        ColumnAlignmentModel model = new LaplaceModel(new StringReader(SMALL_TRAINING_SET), new DnaVocabulary(), 6,
            true);
        profileAligner(new FullColumnAligner(), model, "Full Dynamic", TEST_SET);
    }

    @Test
    @PerformanceTest( {"d820", "11392"})
    public void profileLinearDynamicAligner() throws IOException
    {
        ColumnAlignmentModel model = new LaplaceModel(new StringReader(SMALL_TRAINING_SET), new DnaVocabulary(), 6,
            true);
        profileAligner(new LinearColumnAligner(), model, "Linear Dynamic", TEST_SET);
    }

    private void profileAligner(BaseColumnAligner aligner, ColumnAlignmentModel model, String name, String testSet)
        throws IOException
    {
        int totalMatches = 0;
        int totalLength = 0;
        DnaVocabulary dnaVocabulary = new DnaVocabulary();

        BufferedReader testSetReader = new BufferedReader(new StringReader(testSet));

        int testExamples = 0;
        while (testSetReader.readLine() != null) // Discard label line
        {
            String sequence = testSetReader.readLine();
            MappedSequence unalignedSequence = dnaVocabulary.mapSequence(sequence.replaceAll("-", ""));
            MappedSequence alignment = aligner.align(unalignedSequence, model).alignedSequence();
            totalLength += sequence.length();
            int matches = aligner.matches(sequence, dnaVocabulary.mapSequence(alignment));
            totalMatches += matches;
            testExamples++;
        }

        assertEquals(76164, totalMatches);
    }
}
