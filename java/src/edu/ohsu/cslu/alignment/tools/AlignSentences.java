package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;


import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.alignment.multiple.VariableLengthIterativePairwiseAligner;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.narytree.HeadPercolationRuleset;
import edu.ohsu.cslu.narytree.MsaHeadPercolationRuleset;
import edu.ohsu.cslu.util.Strings;

public class AlignSentences extends BaseCommandlineTool
{
    private String distanceMatrixFilename;
    private final List<String> substitutionMatrixOptions = new ArrayList<String>();
    private final List<String> vocabularyFiles = new ArrayList<String>();

    private final static String AUTO = "auto";

    /**
     * @param args
     */
    public static void main(String[] args)
    {
        run(args);
    }

    @Override
    public void execute() throws Exception
    {
        HeadPercolationRuleset ruleset = new MsaHeadPercolationRuleset();

        final ArrayList<String> sentences = new ArrayList<String>();
        StringBuilder sb = new StringBuilder(8192);
        // TODO: Handle STDIN?
        for (String dataFile : dataFiles)
        {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(fileAsInputStream(dataFile)));
            for (String line = reader.readLine(); line != null; line = reader.readLine())
            {
                sentences.add(line);
                sb.append(Strings.extractPos(line));
                sb.append('\n');
            }
        }
        String entireFile = sb.toString();

        // Induce the vocabularies
        // TODO: induceVocabularies should handle tree-format
        SimpleVocabulary[] inducedVocabularies = SimpleVocabulary.induceVocabularies(entireFile);
        SimpleVocabulary[] vocabularies = new SimpleVocabulary[inducedVocabularies.length];

        if (vocabularyFiles != null)
        {
            if (vocabularyFiles.size() > vocabularies.length)
            {
                vocabularies = new SimpleVocabulary[vocabularyFiles.size()];
            }

            for (int i = 0; i < vocabularyFiles.size(); i++)
            {
                if (!vocabularyFiles.get(i).equalsIgnoreCase(AUTO))
                {
                    vocabularies[i] = SimpleVocabulary.read(new FileReader(vocabularyFiles.get(i)));
                }
                else
                {
                    vocabularies[i] = inducedVocabularies[i];
                }
            }
        }

        // Translate into sequences
        MappedSequence[] sequences = new MappedSequence[sentences.size()];
        for (int i = 0; i < sequences.length; i++)
        {
            // TODO: extract pos from sentence
            sequences[i] = new SimpleMappedSequence(Strings.extractPosAndHead(sentences.get(i), ruleset), vocabularies);
        }

        // Construct and/or read in substitution matrices
        Matrix[] substitutionMatrices = new Matrix[substitutionMatrixOptions.size()];

        for (int i = 0; i < substitutionMatrices.length; i++)
        {
            if (substitutionMatrixOptions.get(i).indexOf(',') >= 0)
            {
                // Construct from 'definition' (substitution-cost, gap-cost)
                String[] costs = substitutionMatrixOptions.get(i).split(",");
                float substitutionCost = Float.parseFloat(costs[0]);
                float gapCost = costs.length > 1 ? Float.parseFloat(costs[1]) : substitutionCost;

                substitutionMatrices[i] = Matrix.Factory.newSymmetricIdentityFloatMatrix(vocabularies[i].size(),
                    substitutionCost, 0f);

                // Specific (generally lower) cost for gaps
                substitutionMatrices[i].setRow(0, gapCost);
                substitutionMatrices[i].setColumn(0, gapCost);
                substitutionMatrices[i].set(0, 0, 0f);
            }
            else
            {
                // Read in matrix file
                substitutionMatrices[i] = Matrix.Factory.read(new File(substitutionMatrixOptions.get(i)));
            }
        }

        MatrixSubstitutionAlignmentModel alignmentModel = new MatrixSubstitutionAlignmentModel(substitutionMatrices,
            vocabularies);

        // Read in pairwise tree distance file
        // TODO: Option to calculate the distance matrix on-the-fly
        Matrix distanceMatrix = Matrix.Factory.read(new File(distanceMatrixFilename));

        // Align the sequences
        VariableLengthIterativePairwiseAligner aligner = new VariableLengthIterativePairwiseAligner();
        MultipleSequenceAlignment alignment = aligner.align(sequences, distanceMatrix, alignmentModel);

        System.out.println(alignment.toString());
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.hasArg().withArgName("matrix").withLongOpt("distance-matrix").withDescription(
            "Distance Matrix File").create("dm"));

        options.addOption(OptionBuilder.hasArgs().withArgName("vocabulary=matrix")
            .withLongOpt("filenames/vocabularies").withDescription(
                "Vocabularies and Substitution Matrices separated by '='. sub-cost,gap-cost"
                    + " defines matrix, 'auto' induces vocabulary automatically").create("vm"));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        distanceMatrixFilename = commandLine.getOptionValue("dm");

        String[] vmOptions = commandLine.getOptionValues("vm");

        for (int i = 0; i < vmOptions.length; i++)
        {
            String[] split = vmOptions[i].split("=");
            if (split.length > 1)
            {
                vocabularyFiles.add(split[0]);
                substitutionMatrixOptions.add(split[1]);
            }
            else
            {
                dataFiles.add(split[0]);
            }
        }
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filenames]";
    }

}
