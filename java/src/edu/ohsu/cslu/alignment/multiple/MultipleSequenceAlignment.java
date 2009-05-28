package edu.ohsu.cslu.alignment.multiple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.util.ArrayList;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.LogLinearAlignmentModel;
import edu.ohsu.cslu.alignment.column.MatrixColumnAlignmentModel;
import edu.ohsu.cslu.alignment.slotBased.MatrixSlotAlignmentModel;
import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.NumericVector;
import edu.ohsu.cslu.util.Arrays;
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
    private int columns;

    /** Number of features of each sequence in the alignment */
    private int featureCount;

    private Vocabulary[] vocabularies;

    /** Used by {@link #induceLogLinearAlignmentModel(NumericVector, NumericVector, NumericVector)} */
    private NumericVector[] countVectors;

    /** Used by {@link #induceLogLinearAlignmentModel(NumericVector, NumericVector, NumericVector)} */
    private FloatVector totalCountVector;

    /** Used by {@link #induceLogLinearAlignmentModel(NumericVector, NumericVector, NumericVector)} */
    private NumericVector cachedLaplacePseudoCounts;

    public MultipleSequenceAlignment()
    {
        this.sequences = new ArrayList<MappedSequence>();
        columns = 0;
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
            columns = newSequence.length();
            featureCount = newSequence.featureCount();
            vocabularies = newSequence.vocabularies();
        }

        checkSequence(newSequence);

        sequences.add(newSequence);

        if (totalCountVector != null)
        {
            // Count the tokens occurring in the sequence
            countSequence((LogLinearMappedSequence) newSequence);

            // And add 1 to the total count
            totalCountVector.inPlaceScalarAdd(1);
        }
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
            columns = newSequence.length();
            featureCount = newSequence.featureCount();
            vocabularies = newSequence.vocabularies();
        }

        checkSequence(newSequence);

        while (sequences.size() <= index)
        {
            sequences.add(null);
        }
        sequences.set(index, newSequence);

        if (totalCountVector != null)
        {
            // Count the tokens occurring in the sequence
            countSequence((LogLinearMappedSequence) newSequence);

            // And add 1 to the total count
            totalCountVector.inPlaceScalarAdd(1);
        }
    }

    private void checkSequence(MappedSequence newSequence)
    {
        // Assign the length when the first sequence is added
        if (columns == 0)
        {
            columns = newSequence.length();
        }

        // All sequence lengths must match
        if (newSequence.length() != columns)
        {
            throw new IllegalArgumentException(String.format(
                "Sequence length (%d) does not match alignment length (%d)", newSequence.length(), columns));
        }

        // All sequence feature counts must match
        if (newSequence.featureCount() != featureCount)
        {
            throw new IllegalArgumentException(String.format(
                "Sequence feature count (%d) does not match alignment feature count (%d)", newSequence.featureCount(),
                featureCount));
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
        return columns;
    }

    /**
     * @return the number of sequences currently in the alignment
     */
    public final int numOfSequences()
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
        if (gapIndices.length == 0)
        {
            return;
        }

        for (int i = 0; i < sequences.size(); i++)
        {
            MappedSequence s = sequences.get(i);
            if (s != null)
            {
                sequences.set(i, s.insertGaps(gapIndices));
            }
        }
        columns += gapIndices.length;

        if (countVectors != null)
        {
            NumericVector[] gapVectors = new NumericVector[gapIndices.length];
            for (int j = 0; j < gapVectors.length; j++)
            {
                gapVectors[j] = (NumericVector) cachedLaplacePseudoCounts.clone();
                gapVectors[j].set(AlignmentModel.GAP_INDEX, gapVectors[j].getFloat(AlignmentModel.GAP_INDEX) + numOfSequences());
            }

            NumericVector[] newCountVectors = new NumericVector[countVectors.length + gapIndices.length];
            Arrays.insertGaps(countVectors, gapIndices, newCountVectors, gapVectors);
            countVectors = newCountVectors;
        }
    }

    public ColumnAlignmentModel inducePssmAlignmentModel(int pseudoCountsPerToken)
    {
        final int[] featureIndices = new int[featureCount];
        final int[] pseudoCounts = new int[featureCount];
        for (int i = 0; i < featureCount; i++)
        {
            featureIndices[i] = i;
            pseudoCounts[i] = pseudoCountsPerToken;
        }
        return inducePssmAlignmentModel(
            pseudoCounts, 
            featureIndices, 
            0, 
            0, 
            new boolean[featureCount],
            Float.POSITIVE_INFINITY);
    }

    public ColumnAlignmentModel inducePssmAlignmentModel(int pseudoCountsPerToken, int[] featureIndices)
    {
        int[] pseudoCounts = new int[featureCount];
        for (int i = 0; i < featureCount; i++)
        {
            pseudoCounts[i] = pseudoCountsPerToken;
        }
        return inducePssmAlignmentModel(
            pseudoCounts, 
            featureIndices, 
            0, 
            0, 
            new boolean[featureCount],
            Float.POSITIVE_INFINITY);
    }

    public ColumnAlignmentModel inducePssmAlignmentModel(int[] pseudoCountsPerToken, int[] featureIndices,
        int emphasizedSequence, int additionalCounts, boolean[] binaryFeatures, float binaryFeatureGapCost)
    {
        final int featureCount = featureIndices.length;
        final Vocabulary[] newVocabularies = new Vocabulary[featureIndices.length];
        for (int f = 0; f < featureCount; f++)
        {
            newVocabularies[f] = vocabularies[featureIndices[f]];
        }

        int[] totalCounts = new int[featureCount];
        final IntMatrix[] counts = new IntMatrix[featureCount];
        for (int f = 0; f < featureCount; f++)
        {
            counts[f] = Matrix.Factory.newIntMatrix(newVocabularies[featureIndices[f]].size(), columns,
                pseudoCountsPerToken[f]);
            totalCounts[f] = pseudoCountsPerToken[f] * newVocabularies[featureIndices[f]].size() + numOfSequences()
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
                        counts[f].add(sequence.elementAt(j).getInt(featureIndices[f]), j, addend);
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

        return new MatrixColumnAlignmentModel(pssmMatrices, newVocabularies);
    }

    /**
     * Induces a {@link LogLinearAlignmentModel} from the sequence. Alignment cost vectors for each
     * column are estimated using maximum likelihood with Laplace smoothing.
     * 
     * @param laplacePseudoCounts Laplace smoothing counts for each vocabulary entry
     * @param scalingVector Weights for each vocabulary entry. If not null, the alignment cost
     *            vectors are scaled by this vector (if null, the final alignment vectors are left
     *            un-scaled).
     * @param columnInsertionCostVector
     * @return alignment model
     */
    public LogLinearAlignmentModel induceLogLinearAlignmentModel(NumericVector laplacePseudoCounts,
        NumericVector scalingVector, NumericVector columnInsertionCostVector)
    {
        final LogLinearVocabulary vocabulary = (LogLinearVocabulary) vocabularies[0];

        if (laplacePseudoCounts.length() != vocabulary.size())
        {
            throw new IllegalArgumentException("Pseudo-count vector length must match vocabulary size");
        }

        if (columnInsertionCostVector.length() != vocabulary.size())
        {
            throw new IllegalArgumentException("Gap insertion-cost vector length must match vocabulary size");
        }

        // Do a full count calculation the first time or if the pseudo-count vector is changed
        if (totalCountVector == null || !laplacePseudoCounts.equals(cachedLaplacePseudoCounts))
        {
            cachedLaplacePseudoCounts = laplacePseudoCounts;

            int[] categoryBoundaries = vocabulary.categoryBoundaries();

            // Separate 'total' count for each category in the grammar
            totalCountVector = new FloatVector(vocabulary.size());
            int previousBoundary = 0;
            for (int i = 0; i <= categoryBoundaries.length + 1; i++)
            {
                final int nextBoundary = i < categoryBoundaries.length ? categoryBoundaries[i] : vocabulary.size();
                final float laplaceTotalCount = laplacePseudoCounts.subVector(previousBoundary, nextBoundary - 1).sum();
                for (int j = previousBoundary; j < nextBoundary; j++)
                {
                    // The divisor for each category should be the pseudo-counts for that category +
                    // the number of sequences in the MSA
                    totalCountVector.set(j, laplaceTotalCount + numOfSequences());
                }

                previousBoundary = nextBoundary;
            }

            // Initialize the count vectors with the specified pseudo-counts
            // NumericVector[] oldCountVectors = countVectors;
            countVectors = new NumericVector[columns];
            for (int j = 0; j < columns; j++)
            {
                countVectors[j] = (NumericVector) cachedLaplacePseudoCounts.clone();
            }

            // Count the features present in the alignment
            for (int i = 0; i < sequences.size(); i++)
            {
                LogLinearMappedSequence sequence = (LogLinearMappedSequence) sequences.get(i);
                if (sequence != null)
                {
                    countSequence(sequence);
                }
            }
        }

        return induceLogLinearAlignmentModel(columnInsertionCostVector, scalingVector, vocabulary);
    }

    private void countSequence(LogLinearMappedSequence sequence)
    {
        for (int j = 0; j < columns; j++)
        {
            ((FloatVector) countVectors[j]).inPlaceAdd(sequence.elementAt(j));
        }
    }

    private LogLinearAlignmentModel induceLogLinearAlignmentModel(NumericVector columnInsertionCostVector,
        NumericVector scalingVector, final LogLinearVocabulary vocabulary)
    {
        // Turn the count vectors into cost vectors
        final FloatVector[] costVectors = new FloatVector[columns];
        for (int j = 0; j < columns; j++)
        {
            costVectors[j] = countVectors[j].elementwiseDivide(totalCountVector).inPlaceElementwiseLog()
                .inPlaceScalarMultiply(-1f);

            if (scalingVector != null)
            {
                costVectors[j].inPlaceElementwiseMultiply(scalingVector);
            }
        }

        return new LogLinearAlignmentModel(costVectors, vocabulary, columnInsertionCostVector);
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
    public static MultipleSequenceAlignment readCharAlignment(Reader reader, AlignmentVocabulary vocabulary)
        throws IOException
    {
        MultipleSequenceAlignment alignment = new MultipleSequenceAlignment();
        BufferedReader br = new BufferedReader(reader);
        for (String line = br.readLine(); line != null; line = br.readLine())
        {
            int[] mappedSequence = new int[line.length()];
            for (int i = 0; i < line.length(); i++)
            {
                mappedSequence[i] = vocabulary.map(line.substring(i, i + 1));
            }

            if (vocabulary instanceof LogLinearVocabulary)
            {
                alignment.addSequence(new LogLinearMappedSequence(mappedSequence, vocabulary));
            }
            else
            {
                alignment.addSequence(new MultipleVocabularyMappedSequence(mappedSequence, vocabulary));
            }
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
            alignment.addSequence(new MultipleVocabularyMappedSequence(line, vocabularies));
        }
        return alignment;
    }

    @Override
    public String toString()
    {
        // TODO: Handle sequences mapped with a LogLinearVocabulary

        // Find the maximum token length in each column
        int lineLength = 0;
        String[] formats = new String[columns];
        for (int j = 0; j < columns; j++)
        {
            int columnLength = 0;
            for (MappedSequence sequence : sequences)
            {
                for (int i = 0; i < featureCount; i++)
                {
                    // Skip un-populated slots (we do not currently insert a marker for these slots)
                    if (sequence != null)
                    {
                        final int sequenceColumnLength = sequence.maxLabelLength(j);
                        if (sequenceColumnLength > columnLength)
                        {
                            columnLength = sequenceColumnLength;
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

    public Vocabulary[] getVocabularies()
    {
        return vocabularies;
    }
}
