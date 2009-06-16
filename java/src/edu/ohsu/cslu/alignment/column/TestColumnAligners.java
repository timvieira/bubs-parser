package edu.ohsu.cslu.alignment.column;

import static junit.framework.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.bio.DnaVocabulary;
import edu.ohsu.cslu.alignment.bio.LogLinearDnaVocabulary;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.tests.FilteredRunner;
import edu.ohsu.cslu.tests.SharedNlpTests;

/**
 * Unit tests for various {@link ColumnSequenceAligner} implementations.
 * 
 * @author Aaron Dunlop
 * @since Jan 22, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
@RunWith(FilteredRunner.class)
public class TestColumnAligners
{
    protected final static DnaVocabulary DNA_VOCABULARY = new DnaVocabulary();

    private String trainingData;
    // TODO: Combine 'simple' and 'columnInsertion' training data?
    private String simpleTrainingData;
    private String columnInsertionTrainingData;

    private ColumnAlignmentModel pssmModel;
    private ColumnAlignmentModel columnInsertionModel;

    @Before
    public void setUp() throws IOException
    {
        StringBuilder sb = new StringBuilder();
        sb.append("Label 1\n");
        sb.append("CGA--T\n");
        sb.append("Label 2\n");
        sb.append("TAA--T\n");
        sb.append("Label 3\n");
        sb.append("A-A--C\n");
        sb.append("Label 4\n");
        sb.append("TGA--T\n");
        sb.append("Label 5\n");
        sb.append("A-A--T\n");
        sb.append("Label 6\n");
        sb.append("A-AC-C\n");
        sb.append("Label 7\n");
        sb.append("--A-AA\n");
        sb.append("Label 8\n");
        sb.append("------\n");
        simpleTrainingData = sb.toString();

        sb = new StringBuilder();
        sb.append("Label\n");
        sb.append("CGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("Label\n");
        sb.append("TAA--T-CT-A--C-C-TCC--GA-A\n");
        sb.append("Label\n");
        sb.append("A-A--C-GT-G--C-C-CAG--TC-G\n");
        sb.append("Label\n");
        sb.append("TGA--T-CT-G--C-C-CTG--CA-C\n");
        sb.append("Label\n");
        sb.append("A-A--T-CT-G--C-C-TGG--TA-G\n");
        sb.append("Label\n");
        sb.append("A-A--C-AT----A-C-CTTT-TG-G\n");
        sb.append("Label\n");
        sb.append("--A--AACT-G--C-C-TGA--TG-G\n");
        sb.append("Label\n");
        sb.append("--------T-A--A-C-CAA--AG-G\n");
        trainingData = sb.toString();

        sb = new StringBuilder();
        sb.append("AACGA\n");
        sb.append("ACCCT\n");
        sb.append("A-CGT\n");
        sb.append("AAGG-\n");
        sb.append("AA-GT\n");
        sb.append("-A-GT\n");
        sb.append("A-CG-\n");
        sb.append("-----\n");
        columnInsertionTrainingData = sb.toString();

        pssmModel = new MaximumLikelihoodModel(new StringReader(columnInsertionTrainingData), DNA_VOCABULARY, false);
        ((MaximumLikelihoodModel) pssmModel).setColumnInsertionCost(Float.POSITIVE_INFINITY);

        columnInsertionModel = new MaximumLikelihoodModel(new StringReader(columnInsertionTrainingData),
            DNA_VOCABULARY, false);
        ((MaximumLikelihoodModel) columnInsertionModel).setColumnInsertionCost(2);
    }

    @Test
    public void testFullColumnAligner() throws Exception
    {
        FullColumnAligner aligner = new FullColumnAligner();

        MaximumLikelihoodModel simpleMlModel = new MaximumLikelihoodModel(new StringReader(simpleTrainingData),
            DNA_VOCABULARY, true);
        assertEquals("Wrong ML alignment", "CAAC-T", alignString(aligner, "CAACT", simpleMlModel));

        MaximumLikelihoodModel mlModel = new MaximumLikelihoodModel(new StringReader(trainingData), DNA_VOCABULARY,
            true);
        LaplaceModel laplaceModel0 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 0, true);
        checkUnsmoothedModel(mlModel, laplaceModel0);

        LaplaceModel laplaceModel2 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 2, true);
        LaplaceModel laplaceModel6 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 6, true);

        sanityTest(aligner, mlModel);
        sanityTest(aligner, laplaceModel2);

        maximumLikelihoodTest(aligner, mlModel);
        maximumLikelihoodTest(aligner, laplaceModel0);

        laplaceTest(aligner, laplaceModel2);
        laplaceTest(aligner, laplaceModel6);

        assertEquals(longAlignedSequence(), alignString(aligner, longUnalignedSequence(), new LaplaceModel(
            new InputStreamReader(SharedNlpTests
                .unitTestDataAsStream("alignment/current_prokMSA_aligned.fasta.train.set.100.gz")), DNA_VOCABULARY, 6,
            true)));

        columnInsertionTest(aligner, pssmModel, columnInsertionModel);
    }

    @Test
    public void testLinearColumnAligner() throws Exception
    {
        LinearColumnAligner aligner = new LinearColumnAligner();

        ColumnAlignmentModel simpleMlModel = new MaximumLikelihoodModel(new StringReader(simpleTrainingData),
            DNA_VOCABULARY, true);
        assertEquals("Wrong ML alignment", "CAAC-T", alignString(aligner, "CAACT", simpleMlModel));

        ColumnAlignmentModel mlModel = new MaximumLikelihoodModel(new StringReader(trainingData), DNA_VOCABULARY, true);
        LaplaceModel laplaceModel0 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 0, true);
        checkUnsmoothedModel(mlModel, laplaceModel0);

        LaplaceModel laplaceModel2 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 2, true);
        LaplaceModel laplaceModel6 = new LaplaceModel(new StringReader(trainingData), DNA_VOCABULARY, 6, true);

        sanityTest(aligner, mlModel);
        sanityTest(aligner, laplaceModel2);

        maximumLikelihoodTest(aligner, mlModel);
        maximumLikelihoodTest(aligner, laplaceModel0);

        laplaceTest(aligner, laplaceModel2);
        laplaceTest(aligner, laplaceModel6);

        assertEquals(longAlignedSequence(), alignString(aligner, longUnalignedSequence(), new LaplaceModel(
            new InputStreamReader(SharedNlpTests
                .unitTestDataAsStream("alignment/current_prokMSA_aligned.fasta.train.set.100.gz")), DNA_VOCABULARY, 6,
            true)));

        columnInsertionTest(aligner, pssmModel, columnInsertionModel);
    }

    @Test
    public void testLogLinearAlignmentModel() throws Exception
    {
        FullColumnAligner aligner = new FullColumnAligner();
        DnaVocabulary dnaVocabulary = new LogLinearDnaVocabulary();
        FloatVector dnaColumnInsertionCostVector = new FloatVector(6, Float.POSITIVE_INFINITY);
        LogLinearAlignmentModel simpleMlModel = new LogLinearAlignmentModel(new StringReader(simpleTrainingData),
            dnaVocabulary, true, dnaColumnInsertionCostVector);
        assertEquals("Wrong ML alignment", "CAAC-T", alignString(aligner, "CAACT", simpleMlModel));

        LogLinearAlignmentModel mlModel = new LogLinearAlignmentModel(new StringReader(trainingData), dnaVocabulary,
            true, dnaColumnInsertionCostVector);
        LogLinearAlignmentModel laplaceModel0 = new LogLinearAlignmentModel(new StringReader(trainingData),
            dnaVocabulary, new FloatVector(6, 0), true, dnaColumnInsertionCostVector);
        checkUnsmoothedModel(mlModel, laplaceModel0);

        LogLinearAlignmentModel laplaceModel2 = new LogLinearAlignmentModel(new StringReader(trainingData),
            dnaVocabulary, new FloatVector(6, .333f), true, dnaColumnInsertionCostVector);
        LogLinearAlignmentModel laplaceModel6 = new LogLinearAlignmentModel(new StringReader(trainingData),
            dnaVocabulary, new FloatVector(6, 1f), true, dnaColumnInsertionCostVector);

        sanityTest(aligner, mlModel);
        sanityTest(aligner, laplaceModel2);

        maximumLikelihoodTest(aligner, mlModel);
        maximumLikelihoodTest(aligner, laplaceModel0);

        laplaceTest(aligner, laplaceModel2);
        laplaceTest(aligner, laplaceModel6);

        assertEquals(longAlignedSequence(), alignString(aligner, longUnalignedSequence(), new LogLinearAlignmentModel(
            new InputStreamReader(SharedNlpTests
                .unitTestDataAsStream("alignment/current_prokMSA_aligned.fasta.train.set.100.gz")), dnaVocabulary,
            new FloatVector(6, 1f), true, dnaColumnInsertionCostVector)));

        columnInsertionModel = new LogLinearAlignmentModel(new StringReader(columnInsertionTrainingData),
            dnaVocabulary, true, new FloatVector(6, 2f));
        columnInsertionTest(aligner, pssmModel, columnInsertionModel);

        String logLinearSentence1 = "(_-) (Delivery _pos_NN) (_-) (_-) (began _pos_AUX _head_verb) (in _pos_IN)"
            + " (early _pos_JJ) (1991 _pos_CD) (. _pos_.)";
        String logLinearSentence2 = "(The _pos_DT) (venture _pos_NN) (will _pos_MD) (be _pos_AUX) (based _pos_VBN _head_verb) (in _pos_IN)"
            + " (_-) (Indianapolis _pos_NNP) (. _pos_.)";
        final String logLinearSentence3 = "(Most _pos_JJS) (will _pos_AUX) (fall _pos_VB _head_verb) (below _pos_IN)"
            + " (previous-month _pos_JJ) (levels _pos_NNS) (. _pos_.)";
        final String logLinearSentence4 = "(mr. _pos_NNP) (rosen _pos_NNP) (and _pos_CC) (mr. _pos_NNP) (smith _pos_NNP)"
            + " (are _pos_AUX) (also _pos_RB) (pushing _pos_VBG _head_verb) (retail _pos_JJ) (sales _pos_NNS) (. _pos_.)";
        LogLinearVocabulary linguisticVocabulary = LogLinearVocabulary.induce(logLinearSentence1 + '\n'
            + logLinearSentence2 + '\n' + logLinearSentence3 + '\n' + logLinearSentence4);

        MultipleSequenceAlignment msa = new MultipleSequenceAlignment();
        msa.addSequence(new LogLinearMappedSequence(logLinearSentence1, linguisticVocabulary));
        msa.addSequence(new LogLinearMappedSequence(logLinearSentence2, linguisticVocabulary));

        FloatVector columnInsertionCostVector = new FloatVector(linguisticVocabulary.size(), 10);
        columnInsertionCostVector.set(linguisticVocabulary.map("_head_verb"), Float.POSITIVE_INFINITY);
        LogLinearAlignmentModel linguisticModel = msa.induceLogLinearAlignmentModel(new FloatVector(
            new float[] {2f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f, 1f,
                         1f, 1f, 1f, 1f, 1f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f, .2f,
                         .2f, 0}), null, columnInsertionCostVector);

        MappedSequence unalignedSequence = new LogLinearMappedSequence(logLinearSentence3, linguisticVocabulary);
        SequenceAlignment alignment = aligner.align(unalignedSequence, linguisticModel);
        assertEquals("(_-) (Most _pos_JJS) (_-) (will _pos_AUX) (fall _pos_VB _head_verb) (below _pos_IN)"
            + " (previous-month _pos_JJ) (levels _pos_NNS) (. _pos_.)", alignment.alignedSequence().toBracketedString());
        msa.insertGaps(alignment.gapIndices());
        msa.addSequence(alignment.alignedSequence());

        unalignedSequence = new LogLinearMappedSequence(logLinearSentence4, linguisticVocabulary);
        alignment = aligner.align(unalignedSequence, linguisticModel);
        assertEquals("(mr. _pos_NNP) (rosen _pos_NNP) (and _pos_CC) (mr. _pos_NNP) (smith _pos_NNP)"
            + " (are _pos_AUX) (also _pos_RB) (pushing _pos_VBG _head_verb) (_-) (retail _pos_JJ)"
            + " (sales _pos_NNS) (. _pos_.)", alignment.alignedSequence().toBracketedString());
        msa.insertGaps(alignment.gapIndices());
        msa.addSequence(alignment.alignedSequence());
    }

    private void checkUnsmoothedModel(ColumnAlignmentModel mlModel, ColumnAlignmentModel unsmoothedModel)
    {
        // A quick sanity test to ensure that an un-smoothed model and the Maximum Likelihood model
        // agree.
        for (int j = 0; j < mlModel.columnCount(); j++)
        {
            for (int i = 0; i < mlModel.featureCount(); i++)
            {
                IntVector sampleVector = new IntVector(new int[] {i});
                assertEquals(mlModel.cost(sampleVector, j), unsmoothedModel.cost(sampleVector, j));
            }
        }
    }

    private void checkUnsmoothedModel(ColumnAlignmentModel mlModel, LogLinearAlignmentModel unsmoothedModel)
    {
        // TODO Combine two forms of this method (the only difference being the type of sample
        // vector we create)

        // A quick sanity test to ensure that an un-smoothed model and the Maximum Likelihood model
        // agree.
        for (int j = 0; j < mlModel.columnCount(); j++)
        {
            for (int i = 0; i < mlModel.featureCount(); i++)
            {
                SparseBitVector sampleVector = new SparseBitVector(new int[] {i});
                assertEquals(mlModel.cost(sampleVector, j), unsmoothedModel.cost(sampleVector, j));
            }
        }
    }

    private String longAlignedSequence()
    {
        StringBuilder sb = new StringBuilder(5000);
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("----------------------------------CT--G--AG-T-GG-C-GG-A--C-------------GGG-TGAGT");
        sb.append("-A--AC-GC-G-T-G-GG---TAA--C-CT-G--C-C-TCA--TA-C---------------------------------");
        sb.append("---------------------------------A-GG----GGG-AT-AA-CAG-------------------------T");
        sb.append("-T-A-----------------------GAA-A---TGG-CTG-CTAA-TA---CC-G--C-AT-A----------A----");
        sb.append("----------------G-C--G-C-A--C--G-----------------GT---AC-C----------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("-----------------G-CA-T---------------------------------------------------------");
        sb.append("-----------------------------------------------------------------------------G-G");
        sb.append("-T--A-C---------------A--G-T-G-T-G----------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("-----------------------------------------------AAAA--A-C------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------TCC-G-------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("-------G---T-G--------------G----T-A---T-GA-G---AT---G-G-----A-CCC-GCG--T-TGG--A");
        sb.append("------TT--A--G-CT-A----G---TTGG-C-A-GG-G-T----AAC-GG-C-C-T-ACCA--A-GG-C-G--A-CG-");
        sb.append("A------------TCC-A-T------AG-CC-G-G-CCT-G-AG----A--GG-TT--G-AA-C-GG-CCAC-A-TTGGG");
        sb.append("--A-C-TG-A-GA-C-AC-G-G-CCCAGA-CTCC-TAC-G--G-G-A-G-GC-A-GC-A-G-TG---GG-G-G-ATA-TT");
        sb.append("GCA-C-AA-T-GG--GG-GA-A----A-C-CC-T-GA-TG-CA-GCGA-CGCC-G-CG-T---G-G-A--G--GA-A-G-");
        sb.append("-A--A-G-G-TT-----TT-CG---------G-A-T-T-G-T--A---AA-C-TCC--------TG-TC-G-T--T-AGG");
        sb.append("----GA-C--G---A-----------------------T--AA------------------------------T-GA-CG");
        sb.append("-GT-A-C-CT-A-AC-A---------AG-----------AAAGC-ACC-GG-C-TAA---C--T-ACGT--GCCA--G-C");
        sb.append("---A--GCCG---C-GG--TA-AA--AC---GT-AG-GGT-GCA-A-G-CG-TTGT-C-CGG-AA-TT-A--C-T--GGG");
        sb.append("T-GTA----AA-GGGA-GC--G-CA-G-G-C-G------------G--G-AA-G-A-C-AA----G-T-T-G---G-AAG");
        sb.append("-TG-A-AA-AC--CA-TGG-G-----------------------------------------------------------");
        sb.append("---------CT-C-AA----------------------------------------------------------------");
        sb.append("---------CC-C-A-TG-AA-T----T-G-C-T-T-T--------C--AA-A-A-C-T-G-TTT--T-T-C--------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("-----------------------------------------T-T-G-A-G-T-A-----G-TG--CA-G-A---------");
        sb.append("---G-GT-A-GA-T----GG--AATT-CCC-G-GT--GT-A-GCG-GTGGAA-TG-CGT-AGAT-A-TC-G-GGA--GG-");
        sb.append("A-AC-A-CC-AG--T--G--GC-GAA-G--G-C---G----G--T-C-TACTG------G-GC-AC--------------");
        sb.append("------------------------------------------------CA-A-C-T--GA--CG-----CT-GA-GG--C");
        sb.append("-T-CGA--AA-G-C--------------A-TGGG-TAG-C-A-AACA--GG-ATTA-G-ATA-C-----CC-T-G-GTA-");
        sb.append("G-T----C-CA--T-G-CCG-T-AAA--C-GATG-AT--TA-CT---------A-GG--T--G-T-TG-G-GG-G-----");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("----ATT-GA-C--------------------------------------------------------------------");
        sb.append("----------------------------------------------------------------------------C-C-");
        sb.append("C-CT--C-A-G-T-GC-C------GC--A----GT-TAA--CA-C-A-A--T--AA-GT--A----A-TCC-ACC-T-G-");
        sb.append("GG-GAG-TA---CGA-----C-C--G-C-A-A-GGT-T--GAA-ACTC-AAA---------GGAA-TTG-ACGGG-G-G-");
        sb.append("CCCG----C-A--C-A-A-GCA-GT-G--G--AG-TA-T--GT-GGT-TT-AATT-C-G-AAG-CAAC-G-CG-A-AG-A");
        sb.append("-A-CC-TT-A-CC-AGGTC-TT-G-AC-A-T-C--------------CGA-T-G-------------C-AT-A-G-C--A");
        sb.append("C--A-GA-G-A-T--G-T-G--T-G-A-AA-T-------CC-------------------------------------T-");
        sb.append("-TC-G------------------------------------------GG---------A---CA-TCG---A--GA----");
        sb.append("-----------------------------------------------C-A-G-G-T-GGTG-CA-TGG-TT--GTC-GTC");
        sb.append("-A-GC-TC---G-TG-TC-G--TGA-GA-TGT-T-GG-G-TT-AA-GT-CCCGC-AA--------C-GAG-CGC-A-ACC");
        sb.append("-C-T-TA--TT--G-CCAG--T-T-A-C-T---A--C----G--------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("------------------------------------------------------------------TTAA----------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("-----------------------------------------G------------A----G---G-A--CT----------");
        sb.append("-----C-T-G-G-C-GA-G--AC-T-G-CCG--T-T------------------------------------G-A---CA");
        sb.append("A----------------------------------A-A-C-G--G-A-GG-A--AGG-T--GGGG-A-TGAC-GTC--AA");
        sb.append("AT-C---ATC-A-T-G-C-C-C-TTT----AT-G--AC-C-T-GG-GC-TA-CAC-ACGTA-C--TA--CAATG---G-C");
        sb.append("GTT-A-A-AC-AAA-GA-GA------------------------------------------------------------");
        sb.append("--------------------------------------A-G-C-A-A--G-ACCG-C--G--------------------");
        sb.append("-------------------A-GG-T-G-----------G--A-G-CA---A----------A--ACT-C------A-A-A");
        sb.append("AACA-AC-G-T-C-T-CAG-TTC--------AGA-T-TGCAG-GC--T-GCAA-CT-C----------------------");
        sb.append("---------------------------------------------------------------------------G-CCT");
        sb.append("GC-A-T-G-AA-G-TC-GGAAT-TG-C-TA--G-TA-AT-C-G-C----GGA-TC-A-G-C-------AT--GCC-GC-G");
        sb.append("-GT-G-AAT-ACGT-T-CCCGGGCCT-TGCA----CACACCG-CCC-GTC-----A------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--------------------------------------------------------------------------------");
        sb.append("--");
        return sb.toString();
    }

    public String longUnalignedSequence()
    {
        return longAlignedSequence().replaceAll("-", "");
    }

    /**
     * Returns the supplied sequence with gaps inserted to align it according to the model.
     * 
     * @param aligner
     * @param sequence
     * @param model
     * 
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    protected String alignString(ColumnSequenceAligner aligner, String sequence, ColumnAlignmentModel model)
    {
        CharVocabulary charVocabulary = (CharVocabulary) model.vocabularies()[0];

        MappedSequence s = charVocabulary.mapSequence(sequence);
        MappedSequence sequence1 = aligner.align(s, model).alignedSequence();
        return charVocabulary.mapSequence(sequence1);
    }

    /**
     * Returns an alignment of the supplied sequence with a PSSM model, possibly inserting columns
     * into the profile.
     * 
     * @param aligner
     * @param sequence
     * @param model
     * 
     * @return The supplied sequence, with gaps inserted to align it according to the model.
     */
    protected SequenceAlignment align(ColumnSequenceAligner aligner, String sequence, ColumnAlignmentModel model)
    {
        MappedSequence s = ((CharVocabulary) model.vocabularies()[0]).mapSequence(sequence);
        return aligner.align(s, model);
    }

    protected void sanityTest(BaseColumnAligner aligner, ColumnAlignmentModel model)
    {
        // The alignment length is fixed, so sequences of the same length should be unchanged,
        // regardless of the input characters
        assertEquals("Wrong sanity check alignment", "CCCCCCCCCCCCCCCCCCCCCCCCCC", alignString(aligner,
            "CCCCCCCCCCCCCCCCCCCCCCCCCC", model));
        assertEquals("Wrong sanity check alignment", "AAAAAAAAAAAAAAAAAAAAAAAAAA", alignString(aligner,
            "AAAAAAAAAAAAAAAAAAAAAAAAAA", model));
        assertEquals("Wrong sanity check alignment", "GGGGGGGGGGGGGGGGGGGGGGGGGG", alignString(aligner,
            "GGGGGGGGGGGGGGGGGGGGGGGGGG", model));
        assertEquals("Wrong sanity check alignment", "TTTTTTTTTTTTTTTTTTTTTTTTTT", alignString(aligner,
            "TTTTTTTTTTTTTTTTTTTTTTTTTT", model));
        assertEquals("Wrong sanity check alignment", "ACGTACGTACGTACGTACGTACGTAC", alignString(aligner,
            "ACGTACGTACGTACGTACGTACGTAC", model));

        // An empty sequence should map to all deletions
        assertEquals("Wrong sanity check alignment", "--------------------------", alignString(aligner, "", model));
    }

    protected void maximumLikelihoodTest(BaseColumnAligner aligner, ColumnAlignmentModel model)
    {
        assertEquals("Wrong ML alignment", "-------------------------A", alignString(aligner, "A", model));

        assertEquals("Wrong ML alignment", "TGA--T-CT-G--C-C-CTG--CA-C", alignString(aligner, "TGATCTGCCCTGCAC", model));
        assertEquals("Wrong ML alignment", "TGA--T-CT-G--C-C-CTC--CA-C", alignString(aligner, "TGATCTGCCCTCCAC", model));
        assertEquals("Wrong ML alignment", "TAA--C-CT-A--C-C-TGT--TA-G", alignString(aligner, "TAACCTACCTGTTAG", model));
    }

    protected void laplaceTest(BaseColumnAligner aligner, ColumnAlignmentModel model)
    {
        assertEquals("Wrong Laplace alignment", "-----------------------A--", alignString(aligner, "A", model));

        assertEquals("Wrong Laplace alignment", "TGA--T-CT-G--C-C-CTG--CA-C", alignString(aligner, "TGATCTGCCCTGCAC",
            model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CT-T", alignString(aligner, "CAACTTACCCTTCTT",
            model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CA-G", alignString(aligner, "CAACTTACCCTTCAG",
            model));
        assertEquals("Wrong Laplace alignment", "C-A--A-CTTA--C-C-CTT--CA-G", alignString(aligner, "CAACTTACCCTTCAG",
            model));

        // TODO: Add a test with more training and test data...
    }

    protected void columnInsertionTest(FullColumnAligner aligner, ColumnAlignmentModel fixedColumnModel,
        ColumnAlignmentModel variableColumnModel)
    {
        // Aligning with a model which does not allow column insertion should return the sequence
        // as-is
        SequenceAlignment sequenceAlignment = align(aligner, "AACCG", fixedColumnModel);
        assertEquals("Wrong PSSM alignment", "AACCG", ((CharVocabulary) fixedColumnModel.vocabularies()[0])
            .mapSequence(sequenceAlignment.alignedSequence()));

        // But the aligner should insert a column if we allow it.
        sequenceAlignment = align(aligner, "AACCG", variableColumnModel);
        assertEquals("Wrong Column-insertion alignment", "AACCG-",
            ((CharVocabulary) variableColumnModel.vocabularies()[0]).mapSequence(sequenceAlignment.alignedSequence()));
        SharedNlpTests.assertEquals(new int[] {2}, sequenceAlignment.gapIndices());
    }
}
