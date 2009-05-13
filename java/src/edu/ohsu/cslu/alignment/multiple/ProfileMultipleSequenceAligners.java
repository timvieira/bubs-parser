package edu.ohsu.cslu.alignment.multiple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.bio.EvaluateAlignment;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.LaplaceModel;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.PerformanceTest;
import edu.ohsu.cslu.tests.SharedNlpTests;

import static junit.framework.Assert.assertEquals;

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

    }

    @Test
    @PerformanceTest( {"d820", "52344"})
    public void profileIterativePairwiseAligner()
    {
        SubstitutionAlignmentModel subModel = new MatrixSubstitutionAlignmentModel(10, 8,
            new AlignmentVocabulary[] {DNA_VOCABULARY});

        new IterativePairwiseAligner().align(DNA_VOCABULARY.mapSequences(unalignedSequences), distanceMatrix, subModel);
    }

    @Test
    @PerformanceTest( {"d820", "67359"})
    public void profilePssmAligner() throws IOException
    {
        ColumnAlignmentModel model = new LaplaceModel(
            new InputStreamReader(SharedNlpTests.unitTestDataAsStream(CORPUS)), new DnaVocabulary(), 6, true);
        MultipleSequenceAlignment sequenceAlignment = new PssmAligner().align(DNA_VOCABULARY
            .mapSequences(unalignedSequences), distanceMatrix, model);
        long[] eval = EvaluateAlignment.evaluate(DNA_VOCABULARY.mapSequences(sequenceAlignment.sequences()), corpus);
        float accuracy = eval[0] * 100f / eval[1];
        assertEquals(94.78f, accuracy, .1);
    }

    @Test
    @PerformanceTest( {"d820", "26173"})
    public void profileReestimatingPssmAligner() throws IOException
    {
        BufferedReader br = new BufferedReader(new InputStreamReader(SharedNlpTests
            .unitTestDataAsStream("alignment/multiple/wsj_nps.2000.txt.gz")));
        br.mark(1024 * 1024);
        LogLinearVocabulary vocabulary = LogLinearVocabulary.induce(br);
        br.reset();

        LinkedList<MappedSequence> trainingSequences = new LinkedList<MappedSequence>();
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            trainingSequences.add(new LogLinearMappedSequence(line, vocabulary));
        }

        ReestimatingPssmMultipleSequenceAligner aligner = new ReestimatingPssmMultipleSequenceAligner(new FloatVector(
            vocabulary.size(), .2f), new FloatVector(vocabulary.size(), 1));

        aligner.alignInOrder(trainingSequences.toArray(new MappedSequence[trainingSequences.size()]));
    }
}
