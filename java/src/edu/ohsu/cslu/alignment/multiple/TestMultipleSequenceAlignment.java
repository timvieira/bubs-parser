package edu.ohsu.cslu.alignment.multiple;

import java.io.IOException;
import java.io.StringReader;

import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.LogLinearAlignmentModel;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.tests.FilteredRunner;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNull;

/**
 * Unit tests for {@link MultipleSequenceAlignment}
 * 
 * @author Aaron Dunlop
 * @since Mar 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestMultipleSequenceAlignment
{
    private static SimpleVocabulary posVocabulary;
    private static SimpleVocabulary wordVocabulary;

    private static LogLinearVocabulary logLinearVocabulary;

    private final static String sampleSentence4 = "(_- _-) (NN Delivery) (AUX is) (TO to) (VB begin) (IN in)"
        + " (_- _-) (_- _-) (JJ early) (_- _-) (CD 1991) (. .)";
    private final static String sampleSentence8 = "(DT The) (NN venture) (MD will) (AUX be) (VBN based) (IN in)"
        + " (_- _-) (_- _-) (_- _-) (NNP Indianapolis) (_- _-) (. .)";
    private final static String sampleSentence16 = "(_- _-) (JJS Most) (_- _-) (AUX are) (VBN expected) (TO to) (VB fall) (IN below)"
        + " (JJ previous-month) (NNS levels) (_- _-) (. .)";

    private static MappedSequence linguisticSequence1;
    private static MappedSequence linguisticSequence2;
    private static MappedSequence linguisticSequence3;

    private final static String logLinearSentence4 = "(_-) (_pos_NN Delivery) (_pos_AUX is) (_pos_TO to) (_pos_VB begin _head_verb) (_pos_IN in)"
        + " (_-) (_-) (_pos_JJ early) (_-) (_pos_CD 1991) (_pos_. .)";
    private final static String logLinearSentence8 = "(_pos_DT The) (_pos_NN venture) (_pos_MD will) (_pos_AUX be) (_pos_VBN based _head_verb) (_pos_IN in)"
        + " (_-) (_-) (_-) (_pos_NNP Indianapolis) (_-) (_pos_. .)";
    private final static String logLinearSentence16 = "(_-) (_pos_JJS Most) (_-) (_pos_AUX are) (_pos_VBN expected) (_pos_TO to) (_pos_VB fall _head_verb) (_pos_IN below)"
        + " (_pos_JJ previous-month) (_pos_NNS levels) (_-) (_pos_. .)";

    private static MappedSequence logLinearSequence1;
    private static MappedSequence logLinearSequence2;

    @BeforeClass
    public static void suiteSetUp() throws Exception
    {
        // Create Vocabularies
        SimpleVocabulary[] vocabularies = SimpleVocabulary.induceVocabularies(sampleSentence4 + '\n' + sampleSentence8
            + '\n' + sampleSentence16);
        posVocabulary = vocabularies[0];
        wordVocabulary = vocabularies[1];

        linguisticSequence1 = new MultipleVocabularyMappedSequence(sampleSentence4, vocabularies);
        linguisticSequence2 = new MultipleVocabularyMappedSequence(sampleSentence16, vocabularies);
        linguisticSequence3 = new MultipleVocabularyMappedSequence(sampleSentence8, vocabularies);

        logLinearVocabulary = LogLinearVocabulary.induce(logLinearSentence4 + '\n' + logLinearSentence8 + '\n'
            + logLinearSentence16);
        logLinearSequence1 = new LogLinearMappedSequence(logLinearSentence4, logLinearVocabulary);
        logLinearSequence2 = new LogLinearMappedSequence(logLinearSentence16, logLinearVocabulary);
    }

    @Test
    public void testGet()
    {
        MultipleSequenceAlignment linguisticAlignment = new MultipleSequenceAlignment(
            new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        assertEquals(linguisticSequence2.toString(), linguisticAlignment.get(1).toString());
    }

    @Test
    public void testAddSequence()
    {
        MultipleSequenceAlignment linguisticAlignment = new MultipleSequenceAlignment(
            new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        // Add a sequence in a position other than the 'next' position
        linguisticAlignment.addSequence(linguisticSequence3, 3);

        assertNull(linguisticAlignment.get(2));
        assertEquals(linguisticSequence3.toString(), linguisticAlignment.get(3).toString());
    }

    @Test
    public void testInsertGaps()
    {
        MultipleSequenceAlignment linguisticAlignment = new MultipleSequenceAlignment(
            new MappedSequence[] {linguisticSequence1, linguisticSequence2});
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

        MultipleSequenceAlignment alignment = MultipleSequenceAlignment.readCharAlignment(new StringReader(sb
            .toString()), dnaVocabulary);

        // First test a maximum-likelihood model
        ColumnAlignmentModel pssmModel = alignment.inducePssmAlignmentModel(0);
        assertEquals(-Math.log(3f / 8), pssmModel.cost(new IntVector(DNA_A), 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.cost(new IntVector(DNA_T), 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.cost(new IntVector(DNA_C), 5), .01f);
        assertEquals(0f, pssmModel.cost(new IntVector(DNA_GAP), 11), .01f);
        assertEquals(-Math.log(1f / 8), pssmModel.cost(new IntVector(DNA_T), 20), .01f);
        assertEquals(-Math.log(5f / 8), pssmModel.cost(new IntVector(DNA_G), 25), .01f);

        // Now test a Laplace-smoothed model
        pssmModel = alignment.inducePssmAlignmentModel(1);
        assertEquals(-Math.log(4f / 14), pssmModel.cost(new IntVector(DNA_A), 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.cost(new IntVector(DNA_T), 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.cost(new IntVector(DNA_C), 5), .01f);
        assertEquals(-Math.log(9f / 14), pssmModel.cost(new IntVector(DNA_GAP), 11), .01f);
        assertEquals(-Math.log(2f / 14), pssmModel.cost(new IntVector(DNA_T), 20), .01f);
        assertEquals(-Math.log(6f / 14), pssmModel.cost(new IntVector(DNA_G), 25), .01f);

        // And a linguistic alignment
        alignment = new MultipleSequenceAlignment();
        alignment.addSequence(linguisticSequence1);
        alignment.addSequence(linguisticSequence2);
        alignment.addSequence(linguisticSequence3);
        pssmModel = alignment.inducePssmAlignmentModel(0);

        // 1 DT/The
        assertEquals(-Math.log(1f / 9), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                wordVocabulary.map("The")}), 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                      wordVocabulary.map("fall")}), 0),
            .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("NN"),
                                                                                      wordVocabulary.map("The")}), 0),
            .01f);

        // We want 0 probability of POS/- and -/word. Even if gaps and the specified POS or word
        // occurs in that column, the PSSM should not allow combining a gap with another feature.
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                      wordVocabulary.map("_-")}), 0),
            .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("_-"),
                                                                                      wordVocabulary.map("The")}), 0),
            .01f);

        // Column 3 has: 1 TO/to, 1 AUX/be, 1 AUX/are. So all probabilities are calculated out of
        // the 9 permutations thereof.
        assertEquals(-Math.log(2f / 9), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("AUX"),
                                                                                wordVocabulary.map("be")}), 3), .01f);
        assertEquals(-Math.log(2f / 9), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("AUX"),
                                                                                wordVocabulary.map("are")}), 3), .01f);
        assertEquals(-Math.log(1f / 9), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("TO"),
                                                                                wordVocabulary.map("to")}), 3), .01f);
        assertEquals(-Math.log(1f / 9), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("TO"),
                                                                                wordVocabulary.map("are")}), 3), .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                      wordVocabulary.map("are")}), 3),
            .01f);

        // A Laplace-smoothed linguistic alignment. Our POS vocabulary consists of 15 tokens and our
        // word vocabulary 22 (including gaps). And we have 3 aligned sequences to count, so POS
        // counts are out of 18 and word counts out of 25.
        pssmModel = alignment.inducePssmAlignmentModel(1);

        // 1 DT/The
        assertEquals(-Math.log(4f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                  wordVocabulary.map("The")}), 0), .01f);
        assertEquals(-Math.log(2f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                  wordVocabulary.map("Most")}), 0),
            .01f);
        assertEquals(-Math.log(2f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("NN"),
                                                                                  wordVocabulary.map("The")}), 0), .01f);

        // We still want 0 probability of POS/- and -/word, even though a purely probabilistic model
        // would allow such combinations.
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                      wordVocabulary.map("_-")}), 0),
            .01f);
        assertEquals(Float.POSITIVE_INFINITY, pssmModel.cost(new IntVector(new int[] {posVocabulary.map("_-"),
                                                                                      wordVocabulary.map("The")}), 0),
            .01f);

        // Column 3 has: 1 TO/to, 1 AUX/be, 1 AUX/are.
        assertEquals(-Math.log(6f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("AUX"),
                                                                                  wordVocabulary.map("be")}), 3), .01f);
        assertEquals(-Math.log(6f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("AUX"),
                                                                                  wordVocabulary.map("are")}), 3), .01f);
        assertEquals(-Math.log(4f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("TO"),
                                                                                  wordVocabulary.map("to")}), 3), .01f);
        assertEquals(-Math.log(4f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("TO"),
                                                                                  wordVocabulary.map("are")}), 3), .01f);

        // Smoothing gives some probability mass to an unobserved feature vector
        assertEquals(-Math.log(1f / 450), pssmModel.cost(new IntVector(new int[] {posVocabulary.map("DT"),
                                                                                  wordVocabulary.map("in")}), 3), .01f);
    }

    @Test
    public void testInduceLogLinearAlignmentModel() throws IOException
    {
        LogLinearVocabulary dnaVocabulary = LogLinearVocabulary.induce("(A) (C) (G) (T) (-)", "-");
        SparseBitVector DNA_A = new SparseBitVector(new int[] {dnaVocabulary.map("A")});
        SparseBitVector DNA_C = new SparseBitVector(new int[] {dnaVocabulary.map("C")});
        SparseBitVector DNA_G = new SparseBitVector(new int[] {dnaVocabulary.map("G")});
        SparseBitVector DNA_T = new SparseBitVector(new int[] {dnaVocabulary.map("T")});
        SparseBitVector DNA_GAP = new SparseBitVector(new int[] {dnaVocabulary.map("-")});

        StringBuilder sb = new StringBuilder(256);
        sb.append("CGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("TAA--T-CT-A--C-C-TCC--GA-A\n");
        sb.append("A-A--C-GT-G--C-C-CAG--TC-G\n");
        sb.append("TGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("A-A--T-CT-G--C-C-TGG--TA-G\n");
        sb.append("A-A--C-AT----A-C-CTTT-TG-G\n");
        sb.append("--A--AACT-G--C-C-TGA--TG-G\n");
        sb.append("--------T-A--A-C-CAA--AG-G\n");

        MultipleSequenceAlignment alignment = MultipleSequenceAlignment.readCharAlignment(new StringReader(sb
            .toString()), dnaVocabulary);
        FloatVector dnaColumnInsertionCostVector = new FloatVector(dnaVocabulary.size(), 10);

        // First test a maximum-likelihood model
        ColumnAlignmentModel pssmModel = alignment.induceLogLinearAlignmentModel(new FloatVector(dnaVocabulary.size(),
            0), null, dnaColumnInsertionCostVector);
        assertEquals(-Math.log(3f / 8), pssmModel.cost(DNA_A, 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.cost(DNA_T, 0), .01f);
        assertEquals(-Math.log(2f / 8), pssmModel.cost(DNA_C, 5), .01f);
        assertEquals(0f, pssmModel.cost(DNA_GAP, 11), .01f);
        assertEquals(-Math.log(1f / 8), pssmModel.cost(DNA_T, 20), .01f);
        assertEquals(-Math.log(5f / 8), pssmModel.cost(DNA_G, 25), .01f);

        // Now test a Laplace-smoothed model
        pssmModel = alignment.induceLogLinearAlignmentModel(new FloatVector(dnaVocabulary.size(), 1), null,
            dnaColumnInsertionCostVector);
        assertEquals(-Math.log(4f / 14), pssmModel.cost(DNA_A, 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.cost(DNA_T, 0), .01f);
        assertEquals(-Math.log(3f / 14), pssmModel.cost(DNA_C, 5), .01f);
        assertEquals(-Math.log(9f / 14), pssmModel.cost(DNA_GAP, 11), .01f);
        assertEquals(-Math.log(2f / 14), pssmModel.cost(DNA_T, 20), .01f);
        assertEquals(-Math.log(6f / 14), pssmModel.cost(DNA_G, 25), .01f);

        // And a linguistic alignment
        String sentence = "(the _pos_DT) (_-) (cat _pos_NN) (ran _pos_VBN _head_verb)";
        LogLinearVocabulary vocabulary = LogLinearVocabulary.induce(sentence);
        alignment = new MultipleSequenceAlignment();
        LogLinearMappedSequence sequence = new LogLinearMappedSequence(sentence, vocabulary);
        alignment.addSequence(sequence);
        // We'll add 1 to gap count, 1/4 to each word count (including -unk-), 1 to each POS count,
        // and 0 to _head_verb counts
        FloatVector laplacePseudoCounts = new FloatVector(new float[] {1f, 1f / 4, 1f / 4, 1f / 4, 1f / 4, 1, 1, 1, 0});
        LogLinearAlignmentModel model = alignment.induceLogLinearAlignmentModel(laplacePseudoCounts, null,
            new FloatVector(new float[] {10, 10, 10, 10, 10, 10, 10, 10, Float.POSITIVE_INFINITY}));

        // Gap
        SparseBitVector gap = new SparseBitVector(new int[] {0});
        assertEquals(-Math.log(1f / 3), model.cost(gap, 0), .01f);
        assertEquals(-Math.log(2f / 3), model.cost(gap, 1), .01f);
        assertEquals(-Math.log(1f / 3), model.cost(gap, 2), .01f);
        // TODO: We should probably have an infinite cost of placing a gap in the _head_verb column
        assertEquals(-Math.log(1f / 3), model.cost(gap, 3), .01f);

        SparseBitVector the = new SparseBitVector(new int[] {vocabulary.map("the")});
        assertEquals(-Math.log(5f / 12), model.cost(the, 0), .01f);
        assertEquals(-Math.log(1f / 12), model.cost(the, 1), .01f);
        assertEquals(-Math.log(1f / 12), model.cost(the, 2), .01f);
        // TODO: We should probably have an infinite cost of aligning 'the' in the _head_verb column
        assertEquals(-Math.log(1f / 12), model.cost(the, 3), .01f);

        SparseBitVector theDT = new SparseBitVector(new int[] {vocabulary.map("the"), vocabulary.map("_pos_DT")});
        assertEquals(-(Math.log(5f / 12) + Math.log(1f / 2)), model.cost(theDT, 0), .01f);
        assertEquals(-(Math.log(1f / 12) + Math.log(1f / 4)), model.cost(theDT, 1), .01f);
        assertEquals(-(Math.log(1f / 12) + Math.log(1f / 4)), model.cost(theDT, 2), .01f);
        // TODO: We should probably have an infinite cost of aligning the/DT in the _head_verb
        // column
        assertEquals(-(Math.log(1f / 12) + Math.log(1f / 4)), model.cost(theDT, 3), .01f);

        SparseBitVector ranVBN = new SparseBitVector(new int[] {vocabulary.map("ran"), vocabulary.map("_pos_VBN"),
                                                                vocabulary.map("_head_verb")});
        assertEquals(Float.POSITIVE_INFINITY, model.cost(ranVBN, 0), .01f);
        assertEquals(Float.POSITIVE_INFINITY, model.cost(ranVBN, 1), .01f);
        assertEquals(Float.POSITIVE_INFINITY, model.cost(ranVBN, 2), .01f);
        assertEquals(-(Math.log(5f / 12) + Math.log(1f / 2) + Math.log(1)), model.cost(ranVBN, 3), .01f);
    }

    @Test
    public void testToString()
    {
        MultipleSequenceAlignment linguisticAlignment = new MultipleSequenceAlignment(
            new MappedSequence[] {linguisticSequence1, linguisticSequence2});
        StringBuilder sb = new StringBuilder(4096);
        sb.append(" _- |       NN | AUX |  TO |       VB | IN |   _- |    _- |             JJ |     _- |   CD | . |\n");
        sb.append(" _- | Delivery |  is |  to |    begin | in |   _- |    _- |          early |     _- | 1991 | . |\n");
        sb.append("------------------------------------------------------------------------------------------------\n");
        sb.append(" _- |      JJS |  _- | AUX |      VBN | TO |   VB |    IN |             JJ |    NNS |   _- | . |\n");
        sb.append(" _- |     Most |  _- | are | expected | to | fall | below | previous-month | levels |   _- | . |\n");
        sb.append("------------------------------------------------------------------------------------------------\n");

        assertEquals(sb.toString(), linguisticAlignment.toString());

        linguisticAlignment = new MultipleSequenceAlignment(new MappedSequence[] {logLinearSequence1,
                                                                                  logLinearSequence2});

        sb = new StringBuilder(4096);
        sb
            .append(" _- | Delivery |       is |       to |      begin |      in |         _- |      _- |          early |       _- |    1991 |      . |\n");
        sb
            .append("    |  _pos_NN | _pos_AUX |  _pos_TO |    _pos_VB | _pos_IN |            |         |        _pos_JJ |          | _pos_CD | _pos_. |\n");
        sb
            .append("    |          |          |          | _head_verb |         |            |         |                |          |         |        |\n");
        sb
            .append("-----------------------------------------------------------------------------------------------------------------------------------\n");
        sb
            .append(" _- |     Most |       _- |      are |   expected |      to |       fall |   below | previous-month |   levels |      _- |      . |\n");
        sb
            .append("    | _pos_JJS |          | _pos_AUX |   _pos_VBN | _pos_TO |    _pos_VB | _pos_IN |        _pos_JJ | _pos_NNS |         | _pos_. |\n");
        sb
            .append("    |          |          |          |            |         | _head_verb |         |                |          |         |        |\n");
        sb
            .append("-----------------------------------------------------------------------------------------------------------------------------------\n");

        assertEquals(sb.toString(), linguisticAlignment.toString());
    }
}
