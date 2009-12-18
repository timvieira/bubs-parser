/**
 * BaseMultipleSequenceAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.pairwise.PairwiseAligner;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * Abstract base class for multiple sequence aligners.
 * 
 * @author Aaron Dunlop
 * @since Feb 17, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class BaseMultipleSequenceAligner implements MultipleSequenceAligner {

    protected PairwiseAligner pairAligner;

    /**
     * Assumes one of the sequences has already been aligned into the aligned set.
     * 
     * @param alignedSequence
     * @param unalignedSequence
     * @param alignmentModel
     * @return Multiple sequence alignment
     */
    protected abstract SequenceAlignment align(MappedSequence alignedSequence,
            MappedSequence unalignedSequence, AlignmentModel alignmentModel);

    protected abstract int firstSequenceToAlign(MappedSequence[] sequences, Matrix distanceMatrix);

    public MultipleSequenceAlignment align(final MappedSequence[] sequences, final Matrix distanceMatrix,
            final AlignmentModel alignmentModel) {
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

        MultipleSequenceAlignment alignedSequences = new MultipleSequenceAlignment();

        int firstSequenceToAlign = firstSequenceToAlign(sequences, distanceMatrix);

        // Mark all distances to/from the aligned sequence as infinite, so we won't try to align
        // this sequence again.
        blackoutAlignedSequence(distanceMatrix, unalignedSequences, firstSequenceToAlign);

        alignedSequences.addSequence(unalignedSequences[firstSequenceToAlign], firstSequenceToAlign);
        unalignedSequences[firstSequenceToAlign] = null;
        int sequencesAligned = 1;

        // Once all sequences are aligned, we can stop
        while (sequencesAligned < unalignedSequences.length) {
            // Find the indices of the closest pair of sequences (a, b) in which exactly one of a and b
            // is already aligned while the other is not aligned.
            // TODO: This search could probably be made more efficient. As it is, it's O(n^2) for
            // each sequence aligned -- O(n^3) total.
            int alignedIndex = -1, unalignedIndex = -1;
            float min = Float.POSITIVE_INFINITY;
            for (int i = 0; i < distanceMatrix.rows(); i++) {
                // The i-th sequence has been aligned if ***un***alignedSequences[i] has been set to null
                final boolean iIsAligned = (unalignedSequences[i] == null);
                for (int j = 0; j < i; j++) {
                    final float distance = distanceMatrix.getFloat(i, j);
                    if (distance < min) {
                        if (iIsAligned) {
                            if (unalignedSequences[j] != null) {
                                alignedIndex = i;
                                unalignedIndex = j;
                                min = distance;
                            }
                        } else if (unalignedSequences[j] == null) {
                            alignedIndex = j;
                            unalignedIndex = i;
                            min = distance;
                        }
                    }
                }
            }

            System.out.println(sequencesAligned + " : (current alignment length " + alignedSequences.length()
                    + ")");
            System.out.println("  Aligning the unaligned sequence (index=" + unalignedIndex + "):");
            System.out.println("    " + unalignedSequences[unalignedIndex]);
            System.out.println("  against the previously aligned sequence (index=" + alignedIndex + "):");
            System.out.println("    " + sequences[alignedIndex]);
            System.out.println();

            // The first sequence in the pair is already aligned but the second isn't. Align
            // the unaligned sequence with its nearest neighbor.
            //
            // The SequenceAlignment class structure that is returned holds:
            // 1) the new sequence to be aligned but with gaps inserted
            // 2) all locations in the already aligned sequences that need new gaps inserted
            SequenceAlignment alignment = align(alignedSequences.get(alignedIndex),
                unalignedSequences[unalignedIndex], alignmentModel);

            // Update already aligned sequences to include gaps where needed. For the moment,
            // we'll skip re-computing distance metrics...
            alignedSequences.insertGaps(alignment.insertedColumnIndices());

            alignedSequences.addSequence(alignment.alignedSequence(), unalignedIndex);
            unalignedSequences[unalignedIndex] = null;

            sequencesAligned++;

            blackoutAlignedSequence(distanceMatrix, unalignedSequences, unalignedIndex);

            System.out.println(alignedSequences);
            System.out.println();

        }
        return alignedSequences;
    }

    private void blackoutAlignedSequence(final Matrix distanceMatrix, final Sequence[] unalignedSequences,
            final int sequenceIndex) {
        final float infinity = distanceMatrix.infinity();

        // Set all cells in row index2 of the distance matrix for columns of already-aligned
        // sequences to the maximum storable value (we don't need to consider the distance
        // between two already-aligned sequences)
        for (int j = 0; j < distanceMatrix.columns(); j++) {
            if (unalignedSequences[j] == null) {
                distanceMatrix.set(sequenceIndex, j, infinity);
            }
        }
    }
}