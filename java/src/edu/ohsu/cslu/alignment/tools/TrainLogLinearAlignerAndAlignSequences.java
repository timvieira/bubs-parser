package edu.ohsu.cslu.alignment.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SimpleVocabulary;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.alignment.multiple.ReestimatingPssmMultipleSequenceAligner;
import edu.ohsu.cslu.alignment.pssm.ColumnSequenceAligner;
import edu.ohsu.cslu.alignment.pssm.FullColumnAligner;
import edu.ohsu.cslu.alignment.pssm.LogLinearAlignmentModel;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.tools.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.NumericVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

public class TrainLogLinearAlignerAndAlignSequences extends BaseCommandlineTool
{
    private String distanceMatrixFile;
    private String devSetFile;
    private String vocabularyFile;
    private String laplacePseudoCountVectorFile;
    private String scalingVectorFile;
    private String gapCostVectorFile;

    private final static String HEAD_VERB = "_head_verb";

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

        final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
        final LogLinearVocabulary vocabulary = LogLinearVocabulary.read(new FileReader(vocabularyFile));
        final int headFeature = vocabulary.map(HEAD_VERB);

        MappedSequence[] trainingSequences = mapSequences(reader, vocabulary);

        // Read in pairwise tree distance file
        Matrix distanceMatrix = Matrix.Factory.read(new File(distanceMatrixFile));

        // TODO: Sanity check to ensure distance matrix and training sequence count match

        final NumericVector laplacePseudoCountVector = (NumericVector) Vector.Factory.read(new FileReader(
            laplacePseudoCountVectorFile));
        final NumericVector gapCostVector = (NumericVector) Vector.Factory.read(new FileReader(gapCostVectorFile));
        final NumericVector scalingVector = (NumericVector) Vector.Factory.read(new FileReader(scalingVectorFile));

        // Align the sequences
        ReestimatingPssmMultipleSequenceAligner aligner = new ReestimatingPssmMultipleSequenceAligner(
            laplacePseudoCountVector, gapCostVector);
        MultipleSequenceAlignment trainingAlignment = aligner.align(trainingSequences, distanceMatrix);

        System.out.format("Training Alignment of length %d (produced in %d ms)\n\n", trainingAlignment.length(), System
            .currentTimeMillis()
            - startTime);
        // System.out.println(trainingAlignment.toString());
        // System.out.println();

        LogLinearAlignmentModel pssmAlignmentModel = trainingAlignment.induceLogLinearAlignmentModel(
            laplacePseudoCountVector, scalingVector, gapCostVector);

        // System.out.println(pssmAlignmentModel.toString());

        // Find the head column of the alignment model
        int trainHeadColumn = 0;
        float headP = Float.MAX_VALUE;
        for (int j = 0; j < pssmAlignmentModel.columns(); j++)
        {
            float negativeLogP = pssmAlignmentModel.cost(new SparseBitVector(new int[] {headFeature}), j);
            if (negativeLogP < headP)
            {
                headP = negativeLogP;
                trainHeadColumn = j;
            }
        }

        // Unset the head-column probabilities in the alignment model (so that feature will not be
        // used when aligning the development set)

        // TODO We could use another vector here to remove additional features
        // for (int j = 0; j < pssmAlignmentModel.columns(); j++)
        // {
        // pssmAlignmentModel.
        // }

        int trainHeadsCorrect = headVerbsInColumn(trainHeadColumn, trainingAlignment);
        System.out.format("Training: %4.2f%% identification accuracy of head verbs (%d out of %d )\n",
            trainHeadsCorrect * 100f / trainingAlignment.size(), trainHeadsCorrect, trainingAlignment.size());

        System.out.println("\nHead Column = " + trainHeadColumn + "\n");

        ColumnSequenceAligner pssmAligner = new FullColumnAligner();

        MappedSequence[] devSetSequences = mapSequences(new BufferedReader(new FileReader(devSetFile)), vocabulary);

        startTime = System.currentTimeMillis();
        int devHeadsCorrect = 0;
        for (MappedSequence sequence : devSetSequences)
        {
            int headIndex = 0;
            for (int j = 0; j < sequence.length(); j++)
            {
                if (sequence.elementAt(j).getBoolean(headFeature))
                {
                    // Record head indices and remove that feature from development-set
                    headIndex = j;
                    sequence.elementAt(j).set(headFeature, false);
                    break;
                }
            }

            MappedSequence alignedSequence = pssmAligner.align(sequence, pssmAlignmentModel).alignedSequence();

            // System.out.println(alignedSequence.toString());

            int tokens = -1;
            for (int j = 0; j <= trainHeadColumn; j++)
            {
                // Is this a gap?
                if (!alignedSequence.elementAt(j).getBoolean(0))
                {
                    tokens++;
                }
            }

            if (tokens == headIndex)
            {
                devHeadsCorrect++;
            }
        }

        System.out.format(
            "Analysis in %d ms\nDevelopment: %4.2f%% identification accuracy of head verbs (%d out of %d )\n", System
                .currentTimeMillis()
                - startTime, devHeadsCorrect * 100f / devSetSequences.length, devHeadsCorrect, devSetSequences.length);
    }

    private MappedSequence[] mapSequences(final BufferedReader reader, SimpleVocabulary vocabulary) throws IOException
    {
        final ArrayList<String> sentences = new ArrayList<String>();
        for (String line = reader.readLine(); line != null; line = reader.readLine())
        {
            sentences.add(line);
        }

        // Translate input into sequences
        MappedSequence[] sequences = new MappedSequence[sentences.size()];
        for (int i = 0; i < sequences.length; i++)
        {
            sequences[i] = new LogLinearMappedSequence(sentences.get(i), vocabulary);
        }
        return sequences;
    }

    private int headVerbsInColumn(int column, MultipleSequenceAlignment sequenceAlignment)
    {
        int correct = 0;
        for (int i = 0; i < sequenceAlignment.size(); i++)
        {
            if (headColumn((LogLinearMappedSequence) sequenceAlignment.get(i)) == column)
            {
                correct++;
            }
        }
        return correct;
    }

    private int headColumn(LogLinearMappedSequence sequence)
    {
        int headVerbFeature = sequence.vocabulary().map(HEAD_VERB);
        for (int j = 0; j < sequence.length(); j++)
        {
            if (sequence.elementAt(j).getBoolean(headVerbFeature))
            {
                return j;
            }
        }
        return -1;
    }

    @Override
    @SuppressWarnings("static-access")
    protected Options options() throws Exception
    {
        Options options = basicOptions();

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("file").withLongOpt("vocabulary-file")
            .withDescription("Vocabulary File").create("v"));

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("devset").withLongOpt("development-set")
            .withDescription("Development Set").create("ds"));

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("matrix").withLongOpt("distance-matrix")
            .withDescription("Distance Matrix File").create("dm"));

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("file").withLongOpt("laplace-vector")
            .withDescription("Laplace Pseudo-count Vector File").create("lpc"));

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("file").withLongOpt("scaling-vector")
            .withDescription("Scaling Vector File").create("s"));

        options.addOption(OptionBuilder.isRequired().hasArg().withArgName("cost vector").withLongOpt("gap-cost-file")
            .withDescription("Gap Cost Vector File").create("gc"));

        return options;
    }

    @Override
    public void setToolOptions(CommandLine commandLine)
    {
        distanceMatrixFile = commandLine.getOptionValue("dm");
        devSetFile = commandLine.getOptionValue("ds");
        vocabularyFile = commandLine.getOptionValue('v');
        laplacePseudoCountVectorFile = commandLine.getOptionValue("lpc");
        scalingVectorFile = commandLine.getOptionValue("s");
        gapCostVectorFile = commandLine.getOptionValue("gc");
    }

    @Override
    protected String usageArguments() throws Exception
    {
        return "[training-set filenames]";
    }
}
