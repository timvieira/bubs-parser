package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.ColumnSequenceAligner;
import edu.ohsu.cslu.alignment.column.LinearColumnAligner;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.NumericVector;

/**
 * Aligns sequences using a PSSM model re-estimated at each iteration.
 * 
 * TODO: Share more code with {@link BaseMultipleSequenceAligner} and
 * {@link HmmMultipleSequenceAligner}.
 * 
 * @author Aaron Dunlop
 * @since Mar 31, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ReestimatingPssmMultipleSequenceAligner implements MultipleSequenceAligner
{
    private final ColumnSequenceAligner pssmAligner = new LinearColumnAligner();
    private final NumericVector laplacePseudoCounts;
    private final NumericVector columnInsertionCostVector;

    // private final int upweightingCount;

    // private float upweightingPercentage;

    /**
     * TODO Document
     */
    public ReestimatingPssmMultipleSequenceAligner(final NumericVector laplacePseudoCounts,
        final NumericVector columnInsertionCostVector)
    {
        this.laplacePseudoCounts = laplacePseudoCounts;
        this.columnInsertionCostVector = columnInsertionCostVector;
    }

    @Override
    public MultipleSequenceAlignment align(final MappedSequence[] sequences, final Matrix distanceMatrix,
        final AlignmentModel alignmentModel)
    {
        return align(sequences, distanceMatrix);
    }

    public MultipleSequenceAlignment align(final MappedSequence[] sequences, final Matrix distanceMatrix)
    {
        final MappedSequence[] unalignedSequences = new MappedSequence[sequences.length];
        System.arraycopy(sequences, 0, unalignedSequences, 0, sequences.length);

        // A Distance Tree would allow O(n^2) access to the distance matrix (argmin at each
        // alignment would be O(n^3)). But the tree approach requires O(n^2) space with a fairly
        // large constant factor (since each tree entry is an object with 20 bytes of content).

        // An alternate approach?
        // Maintain rowMin and rowArgMin for each row of matrix - O(n^2) to initialize and O(n)
        // space
        // At each alignment:
        // --Find minimum row - O(n) (We could maintain a sorted tree of row mins for O(log n), but
        // that's probably not worthwhile)
        // --Find min in row (O(n) or O(1) if we keep rowArgMin as well)
        // --Align
        // --'Blackout' row distances to already-aligned sequences - O(n)
        // --Update rowMin, rowArgMin arrays (possibly O(n^2)...)

        // final TreeSet<PairwiseDistance> distanceTree = createDistanceTree(distanceMatrix);

        final MultipleSequenceAlignment alignedSequences = new MultipleSequenceAlignment();

        // TODO: Bug
        final int firstSequenceToAlign = distanceMatrix.argMin()[0];

        // Mark all distances to/from the aligned sequence as infinite, so we won't try to align
        // this sequence again.
        distanceMatrix.set(firstSequenceToAlign, firstSequenceToAlign, distanceMatrix.infinity());

        alignedSequences.addSequence(unalignedSequences[firstSequenceToAlign], firstSequenceToAlign);
        unalignedSequences[firstSequenceToAlign] = null;
        int sequencesAligned = 1;

        // Once all sequences are aligned, we can stop
        while (sequencesAligned < unalignedSequences.length)
        {
            // Find the sequence closest to an already-aligned sequence
            // TODO: This search could probably be made more efficient. As it is, it's O(n^2) for
            // each sequence aligned, or O(n^3) total.
            int unalignedIndex = 1;
            float min = Float.POSITIVE_INFINITY;
            for (int i = 0; i < distanceMatrix.rows(); i++)
            {
                final boolean iUnaligned = (unalignedSequences[i] != null);
                for (int j = 0; j <= i; j++)
                {
                    final float distance = distanceMatrix.getFloat(i, j);
                    if (distance < min)
                    {
                        if (iUnaligned)
                        {
                            if (unalignedSequences[j] != null)
                            {
                                unalignedIndex = i;
                                min = distance;
                            }
                        }
                        else if (unalignedSequences[j] != null)
                        {
                            unalignedIndex = j;
                            min = distance;
                        }
                    }
                }
            }

            System.out.format("%d : Aligning sentence %d (current alignment length %d)\n", sequencesAligned,
                unalignedIndex, alignedSequences.length());

            // Estimate a new PSSM
            final ColumnAlignmentModel columnAlignmentModel = alignedSequences.induceLogLinearAlignmentModel(
                laplacePseudoCounts, null, columnInsertionCostVector);

            // int pssmHeadColumn = -1;
            // Matrix pssmHeadCostMatrix = pssmAlignmentModel.costMatrix(2);
            // for (int j = 0; j < pssmAlignmentModel.columns(); j++)
            // {
            // if (!Float.isInfinite(pssmHeadCostMatrix.getFloat(1, j)))
            // {
            // if (pssmHeadColumn >= 0)
            // {
            // System.err.format("Mismatched Head Columns: %d, %d\n", pssmHeadColumn, j);
            // }
            // else
            // {
            // pssmHeadColumn = j;
            // }
            // }
            // }
            // System.out.println("PSSM Head Column: " + pssmHeadColumn);

            // The first sequence in the pair is already aligned but the second isn't. Align
            // the unaligned sequence with the newly induced PSSM
            final SequenceAlignment alignment = pssmAligner.align(unalignedSequences[unalignedIndex],
                columnAlignmentModel);

            // Update already aligned sequences to include gaps where needed. For the moment,
            // we'll skip re-computing distance metrics...
            alignedSequences.insertGaps(alignment.insertedColumnIndices());

            alignedSequences.addSequence(alignment.alignedSequence(), unalignedIndex);
            unalignedSequences[unalignedIndex] = null;

            sequencesAligned++;

            // System.out.println(alignedSequences.toString());
            blackoutAlignedSequence(distanceMatrix, unalignedSequences, unalignedIndex);
        }
        return alignedSequences;
    }

    public MultipleSequenceAlignment alignInOrder(final MappedSequence[] sequences, final boolean verbose)
    {
        final MultipleSequenceAlignment alignedSequences = new MultipleSequenceAlignment();

        alignedSequences.addSequence(sequences[0]);

        NumericVector cachedColumnInsertionCostVector = columnInsertionCostVector.scalarMultiply(alignedSequences
            .length());
        for (int i = 1; i < sequences.length; i++)
        {
            // Estimate a new PSSM
            final ColumnAlignmentModel columnAlignmentModel = alignedSequences.induceLogLinearAlignmentModel(
                laplacePseudoCounts, null, cachedColumnInsertionCostVector);

            if (verbose)
            {
                System.out.format("Aligning sentence %d (of %d). Current Alignment Length: %d\n", i + 1,
                    sequences.length, columnAlignmentModel.columnCount());
            }

            // Align the unaligned sequence with the newly induced PSSM
            final SequenceAlignment alignment = pssmAligner.align(sequences[i], columnAlignmentModel);

            // Update already aligned sequences to include gaps where needed.
            if (alignment.alignedSequence().length() != columnAlignmentModel.columnCount())
            {
                alignedSequences.insertGaps(alignment.insertedColumnIndices());
                cachedColumnInsertionCostVector = columnInsertionCostVector.scalarMultiply(alignedSequences.length());
            }

            alignedSequences.addSequence(alignment.alignedSequence());
        }
        return alignedSequences;
    }

    private void blackoutAlignedSequence(final Matrix distanceMatrix, final Sequence[] unalignedSequences,
        final int sequenceIndex)
    {
        final float infinity = distanceMatrix.infinity();

        // Set all cells in row index2 of the distance matrix for columns of already-aligned
        // sequences to the maximum storable value (we don't need to consider the distance
        // between two already-aligned sequences)
        for (int j = 0; j < distanceMatrix.columns(); j++)
        {
            if (unalignedSequences[j] == null)
            {
                distanceMatrix.set(sequenceIndex, j, infinity);
            }
        }
    }
}
