package edu.ohsu.cslu.alignment.multiple;

import static junit.framework.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.bio.EvaluateAlignment;
import edu.ohsu.cslu.alignment.pssm.LaplaceModel;
import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

@RunWith(FilteredRunner.class)
@PerformanceTest
public class ProfileMultipleSequenceAligners
{
    public final static String CORPUS = "alignment/multiple/train.set.125.txt.gz";
    public final static String DISTANCE_MATRIX = "alignment/multiple/matrix.125.txt.gz";

    private final static DnaVocabulary DNA_VOCABULARY = new DnaVocabulary();

    private String[] corpus;
    private String[] unalignedSequences;
    private Matrix distanceMatrix;

    private PssmAlignmentModel model;

    @Before
    public void setUp() throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(SharedNlpTests.unitTestDataAsStream(CORPUS)));
        int sequenceIndex = 0;

        List<String> corpusSequenceList = new LinkedList<String>();
        List<String> unalignedSequenceList = new LinkedList<String>();

        while (br.readLine() != null) // Discard the label line
        {
            String corpusSequence = br.readLine();
            corpusSequenceList.add(corpusSequence);

            String unalignedSequence = corpusSequence.replaceAll("-", "");

            sequenceIndex++;
            unalignedSequenceList.add(unalignedSequence);
        }
        br.close();

        // Read distance matrix
        distanceMatrix = Matrix.Factory.read(SharedNlpTests.unitTestDataAsStream(DISTANCE_MATRIX));

        corpus = corpusSequenceList.toArray(new String[0]);
        unalignedSequences = unalignedSequenceList.toArray(new String[0]);

        model = new LaplaceModel(new InputStreamReader(SharedNlpTests.unitTestDataAsStream(CORPUS)),
            new DnaVocabulary(), 6, true);
    }

    @Test
    @PerformanceTest
    public void profileVariableLengthIterativePairwiseAligner() throws IOException
    {
        System.out.println("Variable Length Iterative Pairwise Aligner"
            + profileAligner(new IterativePairwiseAligner()));
    }

    @Test
    @PerformanceTest
    public void profileModelAligner() throws IOException
    {
        float accuracy = profileAligner(new PssmAligner());
        assertEquals(94.78f, accuracy, .1);
    }

    private float profileAligner(MultipleSequenceAligner aligner) throws IOException
    {
        MultipleSequenceAlignment sequenceAlignment = aligner.align(DNA_VOCABULARY.mapSequences(unalignedSequences),
            distanceMatrix, model);

        long[] eval = EvaluateAlignment.evaluate(DNA_VOCABULARY.mapSequences(sequenceAlignment.sequences()), corpus);
        return eval[0] * 100f / eval[1];
    }
}
