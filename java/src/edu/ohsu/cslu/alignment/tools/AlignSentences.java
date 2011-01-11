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
import edu.ohsu.cslu.alignment.multiple.IterativePairwiseAligner;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.datastructs.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.util.Strings;

public class AlignSentences extends BaseCommandlineTool {

    @Option(name = "-dm", aliases = { "--distance-matrix" }, metaVar = "filename", usage = "Distance matrix file")
    private String distanceMatrixFilename;

    @Option(name = "-vm", aliases = { "--vocabulary-files" }, metaVar = "vocabulary=matrix,...", usage = "Vocabularies and Substitution Matrices separated by '='. sub-cost,gap-cost"
            + " defines matrix, 'auto' induces vocabulary automatically")
    private String vocabularyMatrixParam;

    private final List<String> substitutionMatrixOptions = new ArrayList<String>();
    private final List<String> vocabularyFiles = new ArrayList<String>();

    private final static String AUTO = "auto";

    public static void main(final String[] args) {
        run(args);
    }

    @Override
    public void run() throws Exception {
        final HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

        final ArrayList<String> sentences = new ArrayList<String>();
        final StringBuilder sb = new StringBuilder(8192);

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
            sentences.add(line);
            sb.append(Strings.extractPos(line));
            sb.append('\n');
        }

        final String entireFile = sb.toString();

        // Induce the vocabularies
        // TODO: induceVocabularies should handle tree-format
        final SimpleVocabulary[] inducedVocabularies = SimpleVocabulary.induceVocabularies(entireFile);
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
            // TODO: extract pos from sentence
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
        final MultipleSequenceAlignment alignment = aligner.align(sequences, distanceMatrix, alignmentModel);

        System.out.println(alignment.toString());
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
