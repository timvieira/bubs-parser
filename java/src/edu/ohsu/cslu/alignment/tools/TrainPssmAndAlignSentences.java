package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.LinearColumnAligner;
import edu.ohsu.cslu.alignment.multiple.IterativePairwiseAligner;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.util.Strings;

public class TrainPssmAndAlignSentences extends BaseCommandlineTool {

    @Option(name = "-dm", aliases = { "--distance-matrix" }, metaVar = "filename", usage = "Distance matrix file")
    private String distanceMatrixFilename;

    @Option(name = "-ds", aliases = { "--development-set" }, metaVar = "filename", usage = "Development set file")
    private String devSetFilename;

    @Option(name = "-pc", required = true, aliases = { "--pseudo-counts" }, metaVar = "counts", usage = "Pseudo-counts per vocabulary element")
    private int pseudoCountsPerElement;

    @Option(name = "-df", multiValued = true, aliases = { "--devset-features" }, metaVar = "indices", usage = "Features to use when aligning development set")
    private int[] devSetFeatures;

    @Option(name = "-vm", aliases = { "--vocabulary-files" }, metaVar = "vocabulary=matrix,...", usage = "Vocabularies and Substitution Matrices separated by '='. sub-cost,gap-cost"
            + " defines matrix, 'auto' induces vocabulary automatically")
    private String vocabularyMatrixParam;

    private final List<String> substitutionMatrixOptions = new ArrayList<String>();
    private final List<String> vocabularyFiles = new ArrayList<String>();
    private final static String AUTO = "auto";

    /**
     * @param args
     */
    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        final long startTime = System.currentTimeMillis();
        final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

        final ArrayList<String> sentences = new ArrayList<String>();
        final StringBuilder sb = new StringBuilder(8192);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            sentences.add(line);
            sb.append(Strings.extractPos(line));
            sb.append('\n');
        }
        final String entireInput = sb.toString();

        // Induce the vocabularies
        // TODO: induceVocabularies should handle tree-format
        final SimpleVocabulary[] inducedVocabularies = SimpleVocabulary.induceVocabularies(entireInput);
        SimpleVocabulary[] vocabularies = new SimpleVocabulary[inducedVocabularies.length];

        if (vocabularyFiles != null) {
            if (vocabularyFiles.size() > vocabularies.length) {
                vocabularies = new SimpleVocabulary[vocabularyFiles.size()];
            }

            for (int i = 0; i < vocabularyFiles.size(); i++) {
                if (!vocabularyFiles.get(i).equalsIgnoreCase(AUTO)) {
                    vocabularies[i] = SimpleVocabulary.read(new FileReader(vocabularyFiles.get(i)));
                } else {
                    vocabularies[i] = inducedVocabularies[i];
                }
            }
        }

        // Translate into sequences
        final MappedSequence[] sequences = new MappedSequence[sentences.size()];
        for (int i = 0; i < sequences.length; i++) {
            sequences[i] = new MultipleVocabularyMappedSequence(Strings.extractPosAndHead(sentences.get(i),
                ruleset), vocabularies);
        }

        // Construct and/or read in substitution matrices
        final DenseMatrix[] substitutionMatrices = new DenseMatrix[substitutionMatrixOptions.size()];

        for (int i = 0; i < substitutionMatrices.length; i++) {
            if (substitutionMatrixOptions.get(i).indexOf(',') >= 0) {
                // Construct from 'definition' (substitution-cost, gap-cost)
                final String[] costs = substitutionMatrixOptions.get(i).split(",");
                final float substitutionCost = Float.parseFloat(costs[0]);
                final float gapCost = costs.length > 1 ? Float.parseFloat(costs[1]) : substitutionCost;

                substitutionMatrices[i] = Matrix.Factory.newSymmetricIdentityFloatMatrix(vocabularies[i]
                    .size(), substitutionCost, 0f);

                // Specific (generally lower) cost for gaps
                substitutionMatrices[i].setRow(0, gapCost);
                substitutionMatrices[i].setColumn(0, gapCost);
                substitutionMatrices[i].set(0, 0, 0f);
            } else {
                // Read in matrix file
                substitutionMatrices[i] = (DenseMatrix) Matrix.Factory.read(new File(
                    substitutionMatrixOptions.get(i)));
            }
        }

        final MatrixSubstitutionAlignmentModel alignmentModel = new MatrixSubstitutionAlignmentModel(
            substitutionMatrices, vocabularies);

        // Read in pairwise tree distance file
        // TODO: Option to calculate the distance matrix on-the-fly
        final Matrix distanceMatrix = Matrix.Factory.read(new File(distanceMatrixFilename));

        // Align the sequences
        final IterativePairwiseAligner aligner = new IterativePairwiseAligner();
        final MultipleSequenceAlignment trainingAlignment = aligner.align(sequences, distanceMatrix,
            alignmentModel);

        System.out.format("Training Alignment of length %d (produced in %d ms)\n\n", trainingAlignment
            .length(), System.currentTimeMillis() - startTime);
        // System.out.println(trainingAlignment.toString());
        // System.out.println();

        final ColumnAlignmentModel pssmAlignmentModel = trainingAlignment
            .inducePssmAlignmentModel(pseudoCountsPerElement);

        int trainHeadColumn = 0;
        float headP = Float.MAX_VALUE;
        for (int j = 0; j < pssmAlignmentModel.columnCount(); j++) {
            final float negativeLogP = pssmAlignmentModel.cost(new IntVector(new int[] { 1, 1, 1 }), j,
                new int[] { 2 });
            if (negativeLogP < headP) {
                headP = negativeLogP;
                trainHeadColumn = j;
            }
        }

        int correct = headVerbsInColumn(trainHeadColumn, trainingAlignment);
        System.out.format("Training: %4.2f%% identification accuracy of head verbs (%d out of %d )\n",
            correct * 100f / trainingAlignment.numOfSequences(), correct, trainingAlignment.numOfSequences());

        System.out.println("\nHead Column = " + trainHeadColumn + "\n");

        final BufferedReader devSetReader = new BufferedReader(new FileReader(devSetFilename));
        final LinearColumnAligner pssmAligner = new LinearColumnAligner();
        final MultipleSequenceAlignment devAlignment = new MultipleSequenceAlignment();

        for (String line = devSetReader.readLine(); line != null; line = devSetReader.readLine()) {
            final MappedSequence unalignedSequence = new MultipleVocabularyMappedSequence(Strings
                .extractPosAndHead(line, ruleset), vocabularies);
            final MappedSequence alignedSequence = pssmAligner.align(unalignedSequence, pssmAlignmentModel,
                devSetFeatures).alignedSequence();
            devAlignment.addSequence(alignedSequence);
        }

        // System.out.println("Development Alignment");
        // System.out.println(devAlignment.toString());

        correct = headVerbsInColumn(trainHeadColumn, devAlignment);
        System.out.format("Development: %4.2f%% identification accuracy of head verbs (%d out of %d )\n",
            correct * 100f / devAlignment.numOfSequences(), correct, devAlignment.numOfSequences());
    }

    private int headVerbsInColumn(final int column, final MultipleSequenceAlignment sequenceAlignment) {
        int correct = 0;
        for (int i = 0; i < sequenceAlignment.numOfSequences(); i++) {
            if (headColumn((MultipleVocabularyMappedSequence) sequenceAlignment.get(i)) == column) {
                correct++;
            }
        }
        return correct;
    }

    private int headColumn(final MultipleVocabularyMappedSequence sequence) {
        for (int j = 0; j < sequence.length(); j++) {
            if (sequence.feature(j, 2) == 1) {
                return j;
            }
        }
        return -1;
    }

    @Override
    public void setup(final CmdLineParser parser) {
        final String[] vmOptions = vocabularyMatrixParam.split(",");

        for (int i = 0; i < vmOptions.length; i++) {
            final String[] split = vmOptions[i].split("=");
            vocabularyFiles.add(split[0]);
            substitutionMatrixOptions.add(split[1]);
        }
    }
}
