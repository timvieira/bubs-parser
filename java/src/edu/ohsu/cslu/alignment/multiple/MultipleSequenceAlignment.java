package edu.ohsu.cslu.alignment.multiple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;


import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.pssm.MatrixPssmAlignmentModel;
import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.math.linear.FloatMatrix;
import edu.ohsu.cslu.math.linear.IntMatrix;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.util.Strings;

/**
 * Represents a multiple-sequence-alignment; that is, a bag of sequences with gaps inserted as
 * needed in order to align them with one another.
 * 
 * This implementation uses a simplistic ArrayList representation. When aligning large sets of long
 * sequences, it can be more efficient to represent the sequence bag with more sophisticated data
 * structures, such as a Linked List of arrays, allowing for efficient gap insertion.
 * 
 * @author Aaron Dunlop
 * @since Oct 11, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class MultipleSequenceAlignment implements Serializable
{
    /** Sequences currently in the alignment */
    private final ArrayList<MappedSequence> sequences;

    /** Length of the sequences in the alignment */
    private int length;

    /** Number of features of each sequence in the alignment */
    private int features;

    private Vocabulary[] vocabularies;

    public MultipleSequenceAlignment()
    {
        this.sequences = new ArrayList<MappedSequence>();
        length = 0;
    }

    public MultipleSequenceAlignment(final MappedSequence[] sequences)
    {
        this.sequences = new ArrayList<MappedSequence>();

        for (int i = 0; i < sequences.length; i++)
        {
            addSequence(sequences[i]);
        }
    }

    /**
     * Adds a sequence to this alignment
     * 
     * @param newSequence
     */
    public final void addSequence(MappedSequence newSequence)
    {
        if (sequences.size() == 0)
        {
            length = newSequence.length();
            features = newSequence.features();
            vocabularies = newSequence.vocabularies();
        }

        checkSequence(newSequence);

        sequences.add(newSequence);
    }

    /**
     * Adds a sequence to this alignment at a specified location (useful when we want the final
     * alignment in the same order as the original list of sequences)
     * 
     * @param newSequence
     * @param index
     */
    public final void addSequence(MappedSequence newSequence, int index)
    {
        if (sequences.size() == 0)
        {
            length = newSequence.length();
            features = newSequence.features();
            vocabularies = newSequence.vocabularies();
        }

        checkSequence(newSequence);

        while (sequences.size() <= index)
        {
            sequences.add(null);
        }
        sequences.set(index, newSequence);
    }

    private void checkSequence(MappedSequence newSequence)
    {
        // Assign the length when the first sequence is added
        if (length == 0)
        {
            length = newSequence.length();
        }

        // All sequence lengths must match
        if (newSequence.length() != length)
        {
            throw new IllegalArgumentException(String.format(
                "Sequence length (%d) does not match alignment length (%d)", newSequence.length(), length));
        }

        // All sequence feature counts must match
        if (newSequence.features() != features)
        {
            throw new IllegalArgumentException(String.format(
                "Sequence feature count (%d) does not match alignment feature count (%d)", newSequence.features(),
                features));
        }

        // And all sequences must use the same vocabularies
        Vocabulary[] newVocabularies = newSequence.vocabularies();
        for (int i = 0; i < vocabularies.length; i++)
        {
            if (vocabularies[i] != newVocabularies[i])
            {
                throw new IllegalArgumentException("Sequence vocabularies must match");
            }
        }
    }

    /**
     * Returns the specified sequence
     * 
     * @param index
     * @return the specified sequence
     */
    public MappedSequence get(int index)
    {
        return sequences.get(index);
    }

    /**
     * @return the length of the sequences currently in the alignment
     */
    public final int length()
    {
        return length;
    }

    /**
     * @return the number of sequences currently in the alignment
     */
    public final int size()
    {
        return sequences.size();
    }

    /**
     * @return The sequences in this alignment
     */
    public final MappedSequence[] sequences()
    {
        return sequences.toArray(new MappedSequence[sequences.size()]);
    }

    /**
     * Inserts gaps at the specified indices into each of the sequences already in the alignment.
     * 
     * @param gapIndices
     */
    public void insertGaps(final int[] gapIndices)
    {
        for (int i = 0; i < sequences.size(); i++)
        {
            MappedSequence s = sequences.get(i);
            if (s != null)
            {
                sequences.set(i, s.insertGaps(gapIndices));
            }
        }
        length += gapIndices.length;
    }

    public PssmAlignmentModel inducePssmAlignmentModel(int pseudoCountsPerToken)
    {
        final int[] featureIndices = new int[features];
        final int[] pseudoCounts = new int[features];
        for (int i = 0; i < features; i++)
        {
            featureIndices[i] = i;
            pseudoCounts[i] = pseudoCountsPerToken;
        }
        return inducePssmAlignmentModel(pseudoCounts, featureIndices, 0, 0, new boolean[features],
            Float.POSITIVE_INFINITY);
    }

    public PssmAlignmentModel inducePssmAlignmentModel(int pseudoCountsPerToken, int[] featureIndices)
    {
        int[] pseudoCounts = new int[features];
        for (int i = 0; i < features; i++)
        {
            pseudoCounts[i] = pseudoCountsPerToken;
        }
        return inducePssmAlignmentModel(pseudoCounts, featureIndices, 0, 0, new boolean[features],
            Float.POSITIVE_INFINITY);
    }

    public PssmAlignmentModel inducePssmAlignmentModel(int[] pseudoCountsPerToken, int[] featureIndices,
        int emphasizedSequence, int additionalCounts, boolean[] binaryFeatures, float binaryFeatureGapCost)
    {
        final int featureCount = featureIndices.length;
        final int columns = length;
        final Vocabulary[] newVocabularies = new Vocabulary[featureIndices.length];
        for (int f = 0; f < featureCount; f++)
        {
            newVocabularies[f] = vocabularies[featureIndices[f]];
        }

        int[] totalCounts = new int[featureCount];
        final IntMatrix[] counts = new IntMatrix[featureCount];
        for (int f = 0; f < featureCount; f++)
        {
            counts[f] = Matrix.Factory.newIntMatrix(newVocabularies[featureIndices[f]].size(), length,
                pseudoCountsPerToken[f]);
            totalCounts[f] = pseudoCountsPerToken[f] * newVocabularies[featureIndices[f]].size() + size()
                + additionalCounts;
        }

        for (int i = 0; i < sequences.size(); i++)
        {
            MappedSequence sequence = sequences.get(i);
            if (sequence != null)
            {
                for (int j = 0; j < columns; j++)
                {
                    final int addend = (i == emphasizedSequence) ? additionalCounts + 1 : 1;
                    for (int f = 0; f < featureCount; f++)
                    {
                        counts[f].add(sequence.feature(j, featureIndices[f]), j, addend);
                    }
                }
            }
        }

        Matrix[] pssmMatrices = new Matrix[featureCount];
        for (int f = 0; f < featureCount; f++)
        {
            final Matrix matrix = pssmMatrices[f] = new FloatMatrix(counts[f].rows(), counts[f].columns());
            for (int i = 0; i < matrix.rows(); i++)
            {
                if (binaryFeatures[f] && i == AlignmentModel.GAP_INDEX)
                {
                    // Set gap cost specific to a binary feature
                    for (int j = 0; j < matrix.columns(); j++)
                    {
                        matrix.set(i, j, binaryFeatureGapCost);
                    }
                }
                else
                {
                    for (int j = 0; j < matrix.columns(); j++)
                    {
                        matrix.set(i, j, (float) -Math.log(counts[f].getFloat(i, j) / totalCounts[f]));
                    }
                }
            }
        }

        return new MatrixPssmAlignmentModel(pssmMatrices, newVocabularies);
    }

    /**
     * Reads a character-based sequence alignment, such as a DNA, RNA, or protein alignment in the
     * format:
     * 
     * <pre>
     * CGA--T-CT-G--C-C-CTG--CA-C
     * A-A--C-AT----A-C-CTTT-TG-G
     * </pre>
     * 
     * One sequence per line, no delimiters between elements
     * 
     * @param reader
     * @return sequence alignment
     * @throws IOException if the read fails
     */
    public static MultipleSequenceAlignment readCharAlignment(Reader reader, CharVocabulary vocabulary)
        throws IOException
    {
        MultipleSequenceAlignment alignment = new MultipleSequenceAlignment();
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            alignment.addSequence(vocabulary.mapSequence(line));
        }
        return alignment;
    }

    /**
     * Reads an alignment of bracketed sequences, one sequence per line.
     * 
     * <pre>
     * (feature1 feature2...featureN) (feature1 feature2 featureN)...
     * (feature1 feature2...featureN) (feature1 feature2 featureN)...
     * </pre>
     * 
     * One sequence per line, no delimiters between elements
     * 
     * @param reader
     * @return sequence alignment
     * @throws IOException if the read fails
     */
    public static MultipleSequenceAlignment readAlignment(Reader reader, AlignmentVocabulary[] vocabularies)
        throws IOException
    {
        MultipleSequenceAlignment alignment = new MultipleSequenceAlignment();
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            alignment.addSequence(new SimpleMappedSequence(line, vocabularies));
        }
        return alignment;
    }

    @Override
    public String toString()
    {
        // Find the maximum token length in each column
        int lineLength = 0;
        String[] formats = new String[length];
        for (int j = 0; j < length; j++)
        {
            int columnLength = 0;
            for (MappedSequence sequence : sequences)
            {
                for (int i = 0; i < features; i++)
                {
                    // Skip un-populated slots
                    // TODO: Should we include a marker for these positions?
                    if (sequence != null)
                    {
                        int featureLength = sequence.stringFeature(j, i).length();
                        if (featureLength > columnLength)
                        {
                            columnLength = featureLength;
                        }
                    }
                }
            }
            formats[j] = " %" + columnLength + "s |";
            lineLength += columnLength + 3;
        }

        StringBuilder sb = new StringBuilder(2048);

        for (MappedSequence sequence : sequences)
        {
            if (sequence != null)
            {
                sb.append(sequence.toColumnString(formats));
                sb.append('\n');

                // Append a divider
                sb.append(Strings.fill('-', lineLength));
                sb.append('\n');
            }
        }

        return sb.toString();
    }
}
