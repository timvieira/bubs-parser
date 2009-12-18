package edu.ohsu.cslu.alignment.pairwise;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

import java.util.Arrays;
import java.util.LinkedList;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Implements {@link PairwiseAligner} using standard dynamic-programming algorithm. Stores the entire DP array
 * so that {@link #toString()} can display all costs and back-pointers.
 * 
 * TODO Implement a Linear-space version (which of course could not provide the toString() functionality, but
 * should run slightly faster and require less memory).
 * 
 * @author Aaron Dunlop
 * @since Apr 11, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class FullDynamicPairwiseAligner extends BaseDynamicAligner implements PairwiseAligner {

    // 0 = substitution, 1 = gap in unaligned sequence, 2 = gap in already-aligned sequence
    private final static byte BACKPOINTER_SUBSTITUTION = 0;
    private final static byte BACKPOINTER_UNALIGNED_GAP = 1;
    private final static byte BACKPOINTER_ALIGNED_GAP = 2;

    private byte[][] m_backpointer;

    public SequenceAlignment alignPair(Sequence unaligned, Sequence aligned, final AlignmentModel model) {

        // WHAT IS THE POINT OF ALL THESE M_ VARIABLES THAT JUST DOUBLE THE VARIABLES THAT ARE
        // ACTUALLY USED
        //
        // The m_ variables make toString() work, while local copies are faster in memory.
        // 

        m_aligned = aligned;
        m_unaligned = unaligned;
        m_model = model;

        SubstitutionAlignmentModel subModel = (SubstitutionAlignmentModel) model;
        final int unalignedSize = unaligned.length() + 1;
        final int alignedSize = aligned.length() + 1;
        final int currentAlignmentLength = aligned.length();
        final int unalignedLength = unaligned.length();
        final float[][] edits = new float[unalignedSize][alignedSize];
        final byte[][] backpointers = new byte[unalignedSize][alignedSize];

        final int[] GAP = new int[model.featureCount()];
        Arrays.fill(GAP, SubstitutionAlignmentModel.GAP_INDEX);

        m_costs = edits;
        m_backpointer = backpointers;

        // Gaps all the way through unaligned sequence
        for (int alignedIndex = 1; alignedIndex < alignedSize; alignedIndex++) {
            final int alignedIndexMinusOne = alignedIndex - 1;
            edits[0][alignedIndex] = edits[0][alignedIndexMinusOne]
                    + subModel.gapInsertionCost(aligned.elementAt(alignedIndexMinusOne),
                        currentAlignmentLength); // GAP_COST;
            backpointers[0][alignedIndex] = BACKPOINTER_UNALIGNED_GAP;
        }

        // Gaps all the way through aligned sequence
        for (int unalignedIndex = 1; unalignedIndex < unalignedSize; unalignedIndex++) {
            final int unalignedIndexMinusOne = unalignedIndex - 1;
            edits[unalignedIndex][0] = edits[unalignedIndexMinusOne][0]
                    + subModel.gapInsertionCost(unaligned.elementAt(unalignedIndexMinusOne), unalignedLength); // GAP_COST;
            backpointers[unalignedIndex][0] = BACKPOINTER_ALIGNED_GAP;
        }

        backpointers[0][0] = BACKPOINTER_SUBSTITUTION;

        for (int unalignedIndex = 1; unalignedIndex < unalignedSize; unalignedIndex++) {
            final int unalignedIndexMinusOne = unalignedIndex - 1;

            final float[] currentIEdits = edits[unalignedIndex];
            final float[] previousIEdits = edits[unalignedIndexMinusOne];
            final Vector previousUnaligned = unaligned.elementAt(unalignedIndexMinusOne);

            for (int alignedIndex = 1; alignedIndex < alignedSize; alignedIndex++) {
                final int alignedIndexMinusOne = alignedIndex - 1;
                final Vector previousAligned = aligned.elementAt(alignedIndexMinusOne);

                // Inserting a gap into unaligned sequence
                final float scoreOfGapInUnaligned = currentIEdits[alignedIndexMinusOne]
                        + subModel.gapInsertionCost(previousAligned, unalignedLength);

                // Inserting a gap into the aligned sequence
                final float scoreOfGapInAligned = previousIEdits[alignedIndex]
                        + subModel.gapInsertionCost(previousUnaligned, currentAlignmentLength);

                // Substitution / match
                float scoreOfMatching = previousIEdits[alignedIndexMinusOne]
                        + subModel.cost(previousAligned, previousUnaligned);

                if (scoreOfGapInUnaligned < scoreOfGapInAligned) {
                    if (scoreOfGapInUnaligned < scoreOfMatching) {
                        edits[unalignedIndex][alignedIndex] = scoreOfGapInUnaligned;
                        backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_UNALIGNED_GAP;
                    } else {
                        edits[unalignedIndex][alignedIndex] = scoreOfMatching;
                        backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_SUBSTITUTION;
                    }
                } else if (scoreOfGapInAligned < scoreOfMatching) {
                    edits[unalignedIndex][alignedIndex] = scoreOfGapInAligned;
                    backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_ALIGNED_GAP;
                } else {
                    edits[unalignedIndex][alignedIndex] = scoreOfMatching;
                    backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_SUBSTITUTION;
                }

                // This commented-out code changes the preference of when to match and when to
                // insert a gap
                // Note the '<='s in place of the '<'s
                // if (scoreOfGapInUnaligned <= scoreOfGapInAligned)
                // {
                // if (scoreOfGapInUnaligned <= scoreOfMatching)
                // {
                // edits[unalignedIndex][alignedIndex] = scoreOfGapInUnaligned;
                // backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_UNALIGNED_GAP;
                // }
                // else
                // {
                // edits[unalignedIndex][alignedIndex] = scoreOfMatching;
                // backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_SUBSTITUTION;
                // }
                // }
                // else if (scoreOfGapInAligned <= scoreOfMatching)
                // {
                // edits[unalignedIndex][alignedIndex] = scoreOfGapInAligned;
                // backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_ALIGNED_GAP;
                // }
                // else
                // {
                // edits[unalignedIndex][alignedIndex] = scoreOfMatching;
                // backpointers[unalignedIndex][alignedIndex] = BACKPOINTER_SUBSTITUTION;
                // }
            }
        }

        // Backtrace to reconstruct the winning alignment, adding to the beginning of a linked-list
        // to implicitly construct the buffer in order, while we traverse the alignment in reverse.
        final LinkedList<Vector> buffer = new LinkedList<Vector>();
        final IntList gapList = new IntArrayList(unaligned.length());

        final Vector gapVector = model.gapVector();

        int unalignedIndex = unaligned.length();
        int alignedIndex = aligned.length();
        while (unalignedIndex > 0 || alignedIndex > 0) {
            switch (backpointers[unalignedIndex][alignedIndex]) {
            case BACKPOINTER_SUBSTITUTION:
                // Match
                buffer.addFirst(unaligned.elementAt(--unalignedIndex));
                alignedIndex--;
                break;
            case BACKPOINTER_UNALIGNED_GAP:
                // Gap in unaligned sequence
                buffer.addFirst(gapVector);
                alignedIndex--;
                break;
            case BACKPOINTER_ALIGNED_GAP:
                // Gap in aligned sequence
                buffer.addFirst(unaligned.elementAt(--unalignedIndex));
                gapList.add(alignedIndex);
                break;
            default:
                throw new IllegalArgumentException("Should never get here");
            }
        }

        // Reverse gapList
        final int[] gapIndices = new int[gapList.size()];
        final IntListIterator gapIterator = gapList.listIterator();
        for (unalignedIndex = gapIndices.length - 1; unalignedIndex >= 0; unalignedIndex--) {
            gapIndices[unalignedIndex] = gapIterator.nextInt();
        }

        return new SequenceAlignment((MappedSequence) ((SubstitutionAlignmentModel) model)
            .createSequence(buffer.toArray(new Vector[buffer.size()])), gapIndices,
            edits[unalignedSize - 1][alignedSize - 1]);
    }

    @Override
    public String toString() {
        int maxI = m_unaligned.length() + 1;
        int maxJ = m_aligned.length() + 1;

        Vocabulary vocabulary = m_model.vocabularies()[0];
        // TODO: Take length of mapped tokens into account in formatting

        StringBuffer sb = new StringBuffer(1024);
        sb.append("       ");
        for (int j = 0; j < maxJ; j++) {
            sb.append(String.format("%9s |", j > 0 ? vocabulary.map(m_aligned.elementAt(j - 1).getInt(0))
                    : ""));
        }
        sb.append('\n');
        for (int i = 0; i < maxI; i++) {
            sb.append(String.format("%5s |", i > 0 ? vocabulary.map(m_unaligned.elementAt(i - 1).getInt(0))
                    : ""));
            for (int j = 0; j < maxJ; j++) {
                float value = m_costs[i][j];
                String backpointer = null;
                switch (m_backpointer[i][j]) {
                case BACKPOINTER_SUBSTITUTION:
                    backpointer = "\\";
                    break;
                case BACKPOINTER_UNALIGNED_GAP:
                    backpointer = "<";
                    break;
                case BACKPOINTER_ALIGNED_GAP:
                    backpointer = "^";
                    break;
                }
                sb.append(String.format(value > 50000 ? "  Max |" : " %s %6.2f |", backpointer, value));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
