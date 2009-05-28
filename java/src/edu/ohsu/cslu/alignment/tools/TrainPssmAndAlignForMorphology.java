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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.ohsu.cslu.alignment.MatrixSubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.multiple.HmmMultipleSequenceAlignerForMorphology;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

public class TrainPssmAndAlignForMorphology extends BaseCommandlineTool
{
    private String distanceMatrixFilename;
    private int pseudoCountsPerElement;
    
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
        long startTime = System.currentTimeMillis();

        StringBuilder sb = new StringBuilder(8192);

        final ArrayList<String> sentences = new ArrayList<String>();
        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
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

        if (vocabularyFiles != null)
        {
            // if (vocabularyFiles.size() > vocabularies.length)
            // {
            vocabularies = new SimpleVocabulary[vocabularyFiles.size()];
            // }

            for (int i = 0; i < vocabularyFiles.size(); i++)
            {
                if (!vocabularyFiles.get(i).equalsIgnoreCase(AUTO))
                {
                    vocabularies[i] = SimpleVocabulary.read(new FileReader(vocabularyFiles.get(i)));
                }
                else
                {
                    // vocabularies[i] = inducedVocabularies[i];
                }
            }
        }

        // Translate into sequences
        MappedSequence[] sequences = new MappedSequence[sentences.size()];
        for (int i = 0; i < sequences.length; i++)
        {
            sequences[i] = new MultipleVocabularyMappedSequence(sentences.get(i), vocabularies);
            // sequences[i] =
            // new SimpleMappedSequence(Strings.extractPosAndHead(sentences.get(i), ruleset),
            // vocabularies);
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

        
        MatrixSubstitutionAlignmentModel alignmentModel = new MatrixSubstitutionAlignmentModel(substitutionMatrices, vocabularies);
        
        
        // Read in pairwise  distance file
        // TODO: Option to calculate the distance matrix on-the-fly
        Matrix distanceMatrix = Matrix.Factory.read(new File(distanceMatrixFilename));

        // Align the sequences
//        IterativePairwiseAligner aligner = new IterativePairwiseAligner();
        HmmMultipleSequenceAlignerForMorphology aligner = 
            new HmmMultipleSequenceAlignerForMorphology(new int[] {pseudoCountsPerElement}, 0); // Zero for DO NOT UPWEIGHT THE MOST SIMILAR SEQUENCE
        MultipleSequenceAlignment trainingAlignment = aligner.align(sequences, distanceMatrix, alignmentModel);

        System.out.format("Training Alignment of length %d (produced in %d ms)\n\n", trainingAlignment.length(), System
            .currentTimeMillis()
            - startTime);
        System.out.println(trainingAlignment.toString());
        System.out.println();
        

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

        options.addOption(OptionBuilder.hasArg().withArgName("devset").withLongOpt("development-set").withDescription(
            "Development Set").create("ds"));
        options.addOption(OptionBuilder.hasArg().withArgName("pseudo-counts").withLongOpt("pseudo-counts")
            .withDescription("Pseudo-counts per vocabulary element").create("pc"));
        options.addOption(OptionBuilder.hasArgs().withArgName("features").withLongOpt("devset-features")
            .withDescription("Features to use when aligning development set (indices, comma-separated)")
            .withValueSeparator(',').create("df"));

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
        
        pseudoCountsPerElement = Integer.parseInt(commandLine.getOptionValue("pc", "1"));
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[filenames]";
    }
}
