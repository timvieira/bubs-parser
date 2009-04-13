package edu.ohsu.cslu.alignment.multiple;

import static junit.framework.Assert.assertEquals;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.tests.SharedNlpTests;
import edu.ohsu.cslu.util.Strings;

/**
 * Test cases for {@link HmmMultipleSequenceAligner}.
 * 
 * TODO: Recombine with {@link TestMultipleSequenceAligners}
 * 
 * @author Aaron Dunlop
 * @since Jan 28, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestHmmMultipleSequenceAligner
{
    private static SimpleVocabulary[] linguisticVocabularies;
    private static SubstitutionAlignmentModel linguisticAlignmentModel;

    private final static HeadPercolationRuleset headPercolationRuleset = new MsaHeadPercolationRuleset();

    // Sentence 4
    private final static String sampleSentence4 = "(TOP (S (NP (NN Delivery)) (VP (AUX is) (S (VP (TO to)"
        + " (VP (VB begin) (PP (IN in) (NP (JJ early) (CD 1991))))))) (. .)))";
    // Sentence 8
    private final static String sampleSentence8 = "(TOP (S (NP (DT The) (NN venture)) (VP (MD will) (VP (AUX be) (VP (VBN based) (PP (IN in) (NP (NNP Indianapolis)))))) (. .)))";
    // Sentence 16
    private final static String sampleSentence16 = "(TOP (S (NP (JJS Most)) (VP (AUX are) (VP (VBN expected)"
        + " (S (VP (TO to) (VP (VB fall) (PP (IN below) (NP (JJ previous-month) (NNS levels)))))))) (. .)))";

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        // Create Vocabularies
        StringBuilder sb = new StringBuilder(1024);
        sb.append(Strings.extractPosAndHead(sampleSentence4, headPercolationRuleset));
        sb.append('\n');
        sb.append(Strings.extractPosAndHead(sampleSentence8, headPercolationRuleset));
        sb.append('\n');
        sb.append(Strings.extractPosAndHead(sampleSentence16, headPercolationRuleset));
        linguisticVocabularies = SimpleVocabulary.induceVocabularies(sb.toString());

        // Create an alignment model based on the vocabularies - POS substitutions cost 10, word
        // substitutions 1, and HEAD substitutions 100. Gap insertions always cost 4, matches are 0.
        Matrix matrix0 = Matrix.Factory.newIdentityIntMatrix(linguisticVocabularies[0].size(), 10, 0);
        matrix0.setRow(0, 4);
        matrix0.setColumn(0, 4);

        Matrix matrix1 = Matrix.Factory.newIdentityIntMatrix(linguisticVocabularies[1].size(), 1, 0);
        matrix1.setRow(0, 4);
        matrix1.setColumn(0, 4);

        Matrix matrix2 = Matrix.Factory.newIdentityIntMatrix(linguisticVocabularies[2].size(), 1000, 0);
        matrix2.setRow(0, 4);
        matrix2.setColumn(0, 4);

        linguisticAlignmentModel = new MatrixSubstitutionAlignmentModel(new Matrix[] {matrix0, matrix1, matrix2},
            linguisticVocabularies);
    }

    // @Test
    // public void testDnaAlignment() throws Exception
    // {
    // DnaVocabulary dnaVocabulary = new DnaVocabulary();
    // Matrix levenshteinDistances =
    // CalculateDistances.LevenshteinDistanceCalculator.distances(SAMPLE_SEQUENCES);
    // Sequence[] sampleSequences2 = dnaVocabulary.mapSequences(SAMPLE_SEQUENCES);
    //
    // // Full dynamic aligner
    // MultipleSequenceAligner aligner = new IterativePairwiseAligner();
    //
    // // Hand-tuned matrix designed to force more gap insertion
    // Matrix matrix2 = new IntMatrix(new int[][] { {3, 99, 99, 98, 99, 200}, {93, 26, 95, 90, 94,
    // 500},
    // {93, 94, 29, 92, 89, 500}, {93, 91, 94, 25, 94, 500},
    // {92, 92, 87, 91, 34, 500}, {92, 75, 80, 69, 81, 500}});
    // AlignmentModel model2 = new MatrixSubstitutionAlignmentModel(matrix2, dnaVocabulary);
    // MultipleSequenceAlignment alignedSequences = aligner.align(sampleSequences2,
    // levenshteinDistances, model2);
    //
    // StringBuilder sb = new StringBuilder(512);
    // sb.append(" A | C | T | - | G | A | C | - |\n");
    // sb.append("--------------------------------\n");
    // sb.append(" A | C | - | X | G | A | C | - |\n");
    // sb.append("--------------------------------\n");
    // sb.append(" A | C | T | - | G | - | - | - |\n");
    // sb.append("--------------------------------\n");
    // sb.append(" - | C | T | G | G | A | C | T |\n");
    // sb.append("--------------------------------\n");
    // assertEquals(sb.toString(), alignedSequences.toString());
    // }

    @Test
    public void testLinguisticAlignment() throws Exception
    {
        MappedSequence sequence1 = new MultipleVocabularyMappedSequence(Strings.extractPosAndHead(sampleSentence4,
            headPercolationRuleset), linguisticVocabularies);
        MappedSequence sequence2 = new MultipleVocabularyMappedSequence(Strings.extractPosAndHead(sampleSentence16,
            headPercolationRuleset), linguisticVocabularies);
        MappedSequence sequence3 = new MultipleVocabularyMappedSequence(Strings.extractPosAndHead(sampleSentence8,
            headPercolationRuleset), linguisticVocabularies);

        MultipleSequenceAligner aligner = new HmmMultipleSequenceAligner(new int[] {1, 1, 0}, 10);
        Matrix distanceMatrix = new IntMatrix(new int[][] { {0, 1}, {1, 0}});
        MultipleSequenceAlignment sequenceAlignment = aligner.align(new MappedSequence[] {sequence1, sequence2},
            distanceMatrix, linguisticAlignmentModel);

        StringBuilder sb = new StringBuilder(512);

        sb
            .append("       - |       NN |      AUX |      TO |      VB |      IN |             JJ |      CD |       . |\n");
        sb
            .append("       - | Delivery |       is |      to |   begin |      in |          early |    1991 |       . |\n");
        sb
            .append("       - |  NONHEAD |     HEAD | NONHEAD | NONHEAD | NONHEAD |        NONHEAD | NONHEAD | NONHEAD |\n");
        sb
            .append("---------------------------------------------------------------------------------------------------\n");
        sb
            .append("     JJS |      AUX |      VBN |      TO |      VB |      IN |             JJ |     NNS |       . |\n");
        sb
            .append("    Most |      are | expected |      to |    fall |   below | previous-month |  levels |       . |\n");
        sb
            .append(" NONHEAD |  NONHEAD |     HEAD | NONHEAD | NONHEAD | NONHEAD |        NONHEAD | NONHEAD | NONHEAD |\n");
        sb
            .append("---------------------------------------------------------------------------------------------------\n");
        assertEquals(sb.toString(), sequenceAlignment.toString());

        distanceMatrix = new IntMatrix(new int[][] { {0, 1, 2}, {1, 0, 2}, {2, 2, 0}});
        sequenceAlignment = aligner.align(new MappedSequence[] {sequence1, sequence2, sequence3}, distanceMatrix,
            linguisticAlignmentModel);

        String expected = new String(SharedNlpTests
            .readUnitTestData("alignment/multiple/TestHmmMultipleSequenceAlignerSample.txt"));
        assertEquals(expected, sequenceAlignment.toString());

    }

}
