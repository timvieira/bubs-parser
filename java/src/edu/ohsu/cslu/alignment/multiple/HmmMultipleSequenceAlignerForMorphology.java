package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.column.ColumnSequenceAligner;
import edu.ohsu.cslu.alignment.column.FullColumnAligner;
import edu.ohsu.cslu.alignment.column.MatrixColumnAlignmentModel;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * Aligns sequences using an HMM. Implemented as a PSSM alignment, in which the PSSM is re-estimated at each
 * iteration, up-weighting the 'closest' already-aligned sequence over the other sequences.
 * 
 * TODO: Share more code with {@link BaseMultipleSequenceAligner}
 * 
 * @author Aaron Dunlop -- edited by Christian Monson
 * @since Jan 22, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class HmmMultipleSequenceAlignerForMorphology implements MultipleSequenceAligner {

    private final ColumnSequenceAligner pssmAligner = new FullColumnAligner();
    private final int[] laplacePseudoCountsPerToken;
    private final int upweightingCount;

    // private float upweightingPercentage;

    public HmmMultipleSequenceAlignerForMorphology(int[] laplacePseudoCountsPerToken, int upweightingCount) {
        this.laplacePseudoCountsPerToken = laplacePseudoCountsPerToken;
        this.upweightingCount = upweightingCount;
    }

    @Override
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

        // BUG !!!
        //
        // How should you pick the first sequence??

        int firstSequenceToAlign = distanceMatrix.argMin()[0];

        // Mark all distances to/from the aligned sequence as infinite, so we won't try to align
        // this sequence again.
        blackoutAlignedSequence(distanceMatrix, unalignedSequences, firstSequenceToAlign);

        alignedSequences.addSequence(unalignedSequences[firstSequenceToAlign], firstSequenceToAlign);
        unalignedSequences[firstSequenceToAlign] = null;
        int sequencesAligned = 1;

        final int[] featureIndices = new int[alignmentModel.featureCount()];
        for (int i = 0; i < featureIndices.length; i++) {
            featureIndices[i] = i;
        }

        // Once all sequences are aligned, we can stop
        while (sequencesAligned < unalignedSequences.length) {
            // Find the indices of the closest pair in which sequence 1 is already aligned and
            // sequence 2 is not
            // TODO: This search could probably be made more efficient. As it is, it's O(n^2 for
            // each sequence aligned - O(n^3) total.
            int unalignedIndex = 1, alignedIndex = 0;
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
            System.out.println();
            System.out.println("    " + unalignedSequences[unalignedIndex]);

            // Estimate a new PSSM using Laplace smoothing
            // TODO: HEAD/NONHEAD gap cost should be a parameter
            final MatrixColumnAlignmentModel pssmAlignmentModel = new MatrixColumnAlignmentModel(
                alignedSequences, laplacePseudoCountsPerToken, featureIndices, alignedIndex,
                upweightingCount, new boolean[] { false }, 0f);

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

            pssmAlignmentModel.setSubstitutionAlignmentModel((SubstitutionAlignmentModel) alignmentModel);

            // if (unalignedIndex == 1908)
            // {
            // System.out.println("Found 1908");
            // }

            // The first sequence in the pair is already aligned but the second isn't. Align
            // the unaligned sequence with the newly induced PSSM
            SequenceAlignment alignment = pssmAligner.alignWithGaps(unalignedSequences[unalignedIndex],
                pssmAlignmentModel, featureIndices);

            // Update already aligned sequences to include gaps where needed. For the moment,
            // we'll skip re-computing distance metrics...
            alignedSequences.insertGaps(alignment.insertedColumnIndices());

            // int alignmentHeadColumn = 0;
            // for (int j = 0; j < alignment..columns(); j++)
            // {
            // if (pssmHeadCostMatrix.getFloat(2, j) < pssmHeadScore)
            // {
            // pssmHeadScore = pssmHeadCostMatrix.getFloat(2, j);
            // pssmHeadColumn = j;
            // }
            // }
            // System.out.println("PSSM Head Column: " + pssmHeadColumn);

            alignedSequences.addSequence(alignment.alignedSequence(), unalignedIndex);
            unalignedSequences[unalignedIndex] = null;

            sequencesAligned++;

            System.out.println();
            System.out.println(alignedSequences);

            blackoutAlignedSequence(distanceMatrix, unalignedSequences, unalignedIndex);
        }
        return alignedSequences;
    }

    private void blackoutAlignedSequence(final Matrix distanceMatrix,
            final MappedSequence[] unalignedSequences, final int sequenceIndex) {
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
