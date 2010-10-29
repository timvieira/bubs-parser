/*
 * Author: Christian Monson
 * Date:   March 4, 2009
 * 
 * This class was built by copying TrainPssmAndAlignSentences. The purpose of this class is to see
 * if performing the style of multiple sequence alignment that Aaron Dunlop has worked on produces
 * anything meaningful when applied to aligning the characters of words.
 * 
 */

package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.CmdLineParser;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.multiple.HmmMultipleSequenceAlignerForMorphology;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

public class TrainPssmAndAlignForMorphology extends BaseCommandlineTool {

    @Option(name = "-dm", aliases = { "--distance-matrix" }, metaVar = "filename", usage = "Distance matrix file")
    private String distanceMatrixFilename;

    @Option(name = "-pc", required = true, aliases = { "--pseudo-counts" }, metaVar = "counts", usage = "Pseudo-counts per vocabulary element")
    private int pseudoCountsPerElement;

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

        final StringBuilder sb = new StringBuilder(8192);

        final ArrayList<String> sentences = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            sentences.add(line);
            sb.append(line);
            sb.append('\n');
        }

        // String entireInput = sb.toString();
        //
        // Induce the vocabularies
        // TODO: induceVocabularies should handle tree-format
        // SimpleVocabulary[] inducedVocabularies =
        // SimpleVocabulary.induceVocabularies(entireInput);
        // SimpleVocabulary[] vocabularies = new SimpleVocabulary[inducedVocabularies.length];

        SimpleVocabulary[] vocabularies = new SimpleVocabulary[1];

        if (vocabularyFiles != null) {
            // if (vocabularyFiles.size() > vocabularies.length)
            // {
            vocabularies = new SimpleVocabulary[vocabularyFiles.size()];
            // }

            for (int i = 0; i < vocabularyFiles.size(); i++) {
                if (!vocabularyFiles.get(i).equalsIgnoreCase(AUTO)) {
                    vocabularies[i] = SimpleVocabulary.read(new FileReader(vocabularyFiles.get(i)));
                } else {
                    // vocabularies[i] = inducedVocabularies[i];
                }
            }
        }

        // Translate into sequences
        final MappedSequence[] sequences = new MappedSequence[sentences.size()];
        for (int i = 0; i < sequences.length; i++) {
            sequences[i] = new MultipleVocabularyMappedSequence(sentences.get(i), vocabularies);
            // sequences[i] =
            // new SimpleMappedSequence(Strings.extractPosAndHead(sentences.get(i), ruleset),
            // vocabularies);
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

        // Read in pairwise distance file
        // TODO: Option to calculate the distance matrix on-the-fly
        final Matrix distanceMatrix = Matrix.Factory.read(new File(distanceMatrixFilename));

        // Align the sequences
        // IterativePairwiseAligner aligner = new IterativePairwiseAligner();
        final HmmMultipleSequenceAlignerForMorphology aligner = new HmmMultipleSequenceAlignerForMorphology(
            new int[] { pseudoCountsPerElement }, 0); // Zero for DO NOT UPWEIGHT THE MOST SIMILAR
        // SEQUENCE
        final MultipleSequenceAlignment trainingAlignment = aligner.align(sequences, distanceMatrix,
            alignmentModel);

        System.out.format("Training Alignment of length %d (produced in %d ms)\n\n", trainingAlignment
            .length(), System.currentTimeMillis() - startTime);
        System.out.println(trainingAlignment.toString());
        System.out.println();

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
