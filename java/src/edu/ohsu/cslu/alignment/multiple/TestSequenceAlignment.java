package edu.ohsu.cslu.alignment.multiple;

import java.io.IOException;
import java.io.StringReader;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;


import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

public class TestSequenceAlignment
{
    MultipleSequenceAlignment linguisticAlignment;

    private static SimpleVocabulary posVocabulary;
    private static SimpleVocabulary wordVocabulary;

    private final static String sampleSentence4 = "(- -) (NN Delivery) (AUX is) (TO to) (VB begin) (IN in)"
        + " (- -) (- -) (JJ early) (- -) (CD 1991) (. .)";
    private final static String sampleSentence8 = "(DT The) (NN venture) (MD will) (AUX be) (VBN based) (IN in)"
        + " (- -) (- -) (- -) (NNP Indianapolis) (- -) (. .)";
    private final static String sampleSentence16 = "(- -) (JJS Most) (- -) (AUX are) (VBN expected) (TO to) (VB fall) (IN below)"
        + " (JJ previous-month) (NNS levels) (- -) (. .)";

    private static MappedSequence linguisticSequence1;
    private static MappedSequence linguisticSequence2;
    private static MappedSequence linguisticSequence3;

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        // Create Vocabularies
        StringBuilder sb = new StringBuilder(1024);
        sb.append(sampleSentence4);
        sb.append('\n');
        sb.append(sampleSentence8);
        sb.append('\n');
        sb.append(sampleSentence16);

        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(sb.toString());
        posVocabulary = vocabularies[0];
        wordVocabulary = vocabularies[1];

        linguisticSequence1 = new SimpleMappedSequence(sampleSentence4, vocabularies);
        linguisticSequence2 = new SimpleMappedSequence(sampleSentence16, vocabularies);
        linguisticSequence3 = new SimpleMappedSequence(sampleSentence8, vocabularies);
    }

    @Test
    public void testGet()
    {
        linguisticAlignment = new MultipleSequenceAlignment(new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        assertEquals(linguisticSequence2.toString(), linguisticAlignment.get(1).toString());
    }

    @Test
    public void testAddSequence()
    {
        linguisticAlignment = new MultipleSequenceAlignment(new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        // Add a sequence in a position other than the 'next' position
        linguisticAlignment.addSequence(linguisticSequence3, 3);

        assertNull(linguisticAlignment.get(2));
        assertEquals(linguisticSequence3.toString(), linguisticAlignment.get(3).toString());
    }

    @Test
    public void testInsertGaps()
    {
        linguisticAlignment = new MultipleSequenceAlignment(new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        int[] gapIndices = new int[] {3, 4, 6};
        linguisticAlignment.insertGaps(gapIndices);
        assertEquals(linguisticSequence1.insertGaps(gapIndices).toString(), linguisticAlignment.get(0).toString());
        assertEquals(linguisticSequence2.insertGaps(gapIndices).toString(), linguisticAlignment.get(1).toString());
    }

    @Test
    public void testInducePssmAlignmentModel() throws IOException
    {
        DnaVocabulary dnaVocabulary = new DnaVocabulary();
        int[] DNA_A = new int[] {dnaVocabulary.mapCharacter('A')};
        int[] DNA_C = new int[] {dnaVocabulary.mapCharacter('C')};
        int[] DNA_G = new int[] {dnaVocabulary.mapCharacter('G')};
        int[] DNA_T = new int[] {dnaVocabulary.mapCharacter('T')};
        int[] DNA_GAP = new int[] {dnaVocabulary.mapCharacter('-')};

        StringBuilder sb = new StringBuilder(256);
        sb.append("CGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("TAA--T-CT-A--C-C-TCC--GA-A\n");
        sb.append("A-A--C-GT-G--C-C-CAG--TC-G\n");
        sb.append("TGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("A-A--T-CT-G--C-C-TGG--TA-G\n");
        sb.append("A-A--C-AT----A-C-CTTT-TG-G\n");
        sb.append("--A--AACT-G--C-C-TGA--TG-G\n");
        sb.append("--------T-A--A-C-CAA--AG-G\n");

        MultipleSequenceAlignment alignment = MultipleSequenceAlignment.readCharAlignment(new StringReader(sb.toString()),
            dnaVocabulary);

        // First test a maximum-likelihood model
        PssmAlignmentModel pssmModel = alignment.inducePssmAlignmentModel(0);
        assertEquals(-Math.log(3f / 8), pssmModel.negativeLogP(DNA_A, 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.negativeLogP(DNA_T, 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.negativeLogP(DNA_C, 5), .01f);
        assertEquals(0f, pssmModel.negativeLogP(DNA_GAP, 11), .01f);
        assertEquals(-Math.log(1f / 8), pssmModel.negativeLogP(DNA_T, 20), .01f);
        assertEquals(-Math.log(5f / 8), pssmModel.negativeLogP(DNA_G, 25), .01f);

        // Now test a Laplace-smoothed model
        pssmModel = alignment.inducePssmAlignmentModel(1);
        assertEquals(-Math.log(4f / 14), pssmModel.negativeLogP(DNA_A, 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.negativeLogP(DNA_T, 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.negativeLogP(DNA_C, 5), .01f);
        assertEquals(-Math.log(9f / 14), pssmModel.negativeLogP(DNA_GAP, 11), .01f);
        assertEquals(-Math.log(2f / 14), pssmModel.negativeLogP(DNA_T, 20), .01f);
        assertEquals(-Math.log(6f / 14), pssmModel.negativeLogP(DNA_G, 25), .01f);

        // And a linguistic alignment
        alignment = new MultipleSequenceAlignment();
        alignment.addSequence(linguisticSequence1);
        alignment.addSequence(linguisticSequence2);
        alignment.addSequence(linguisticSequence3);
        pssmModel = alignment.inducePssmAlignmentModel(0);

        // 1 DT/The
        assertEquals(-Math.log(1f / 9), pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                          wordVocabulary.map("The")}, 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                                wordVocabulary.map("fall")}, 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("NN"),
                                                                                wordVocabulary.map("The")}, 0), .01f);

        // We want 0 probability of POS/- and -/word. Even if gaps and the specified POS or word
        // occurs in that column, the PSSM should not allow combining a gap with another feature.
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                                wordVocabulary.map("-")}, 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("-"),
                                                                                wordVocabulary.map("The")}, 0), .01f);

        // Column 3 has: 1 TO/to, 1 AUX/be, 1 AUX/are. So all probabilities are calculated out of
        // the 9 permutations thereof.
        assertEquals(-Math.log(2f / 9), pssmModel.negativeLogP(new int[] {posVocabulary.map("AUX"),
                                                                          wordVocabulary.map("be")}, 3), .01f);
        assertEquals(-Math.log(2f / 9), pssmModel.negativeLogP(new int[] {posVocabulary.map("AUX"),
                                                                          wordVocabulary.map("are")}, 3), .01f);
        assertEquals(-Math.log(1f / 9), pssmModel.negativeLogP(new int[] {posVocabulary.map("TO"),
                                                                          wordVocabulary.map("to")}, 3), .01f);
        assertEquals(-Math.log(1f / 9), pssmModel.negativeLogP(new int[] {posVocabulary.map("TO"),
                                                                          wordVocabulary.map("are")}, 3), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                                wordVocabulary.map("are")}, 3), .01f);

        // A Laplace-smoothed linguistic alignment. Our POS vocabulary consists of 15 tokens and our
        // word vocabulary 22 (including gaps). And we have 3 aligned sequences to count, so POS
        // counts are out of 18 and word counts out of 25.
        pssmModel = alignment.inducePssmAlignmentModel(1);

        // 1 DT/The
        assertEquals(-Math.log(4f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                            wordVocabulary.map("The")}, 0), .01f);
        assertEquals(-Math.log(2f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                            wordVocabulary.map("Most")}, 0), .01f);
        assertEquals(-Math.log(2f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("NN"),
                                                                            wordVocabulary.map("The")}, 0), .01f);

        // We still want 0 probability of POS/- and -/word, even though a purely probabilistic model
        // would allow such combinations.
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                                wordVocabulary.map("-")}, 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.negativeLogP(new int[] {posVocabulary.map("-"),
                                                                                wordVocabulary.map("The")}, 0), .01f);

        // Column 3 has: 1 TO/to, 1 AUX/be, 1 AUX/are.
        assertEquals(-Math.log(6f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("AUX"),
                                                                            wordVocabulary.map("be")}, 3), .01f);
        assertEquals(-Math.log(6f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("AUX"),
                                                                            wordVocabulary.map("are")}, 3), .01f);
        assertEquals(-Math.log(4f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("TO"),
                                                                            wordVocabulary.map("to")}, 3), .01f);
        assertEquals(-Math.log(4f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("TO"),
                                                                            wordVocabulary.map("are")}, 3), .01f);

        // Smoothing gives some probability mass to an unobserved feature vector
        assertEquals(-Math.log(1f / 450), pssmModel.negativeLogP(new int[] {posVocabulary.map("DT"),
                                                                            wordVocabulary.map("in")}, 3), .01f);

    }
}
