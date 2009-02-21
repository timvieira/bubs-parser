package edu.ohsu.cslu.alignment.pairwise;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

import java.util.Arrays;
import java.util.LinkedList;


import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;
import edu.ohsu.cslu.common.Vocabulary;

public class FullDynamicPairwiseAligner extends BaseDynamicAligner implements PairwiseAligner
{
    // 0 = substitution, 1 = gap in unaligned sequence, 2 = gap in already-aligned sequence
    private final static byte BACKPOINTER_SUBSTITUTION = 0;
    private final static byte BACKPOINTER_UNALIGNED_GAP = 1;
    private final static byte BACKPOINTER_ALIGNED_GAP = 2;

    private byte[][] m_backpointer;

    public SequenceAlignment alignPair(MappedSequence unaligned, MappedSequence aligned, final AlignmentModel model)
    {
        m_aligned = aligned;
        m_unaligned = unaligned;
        m_model = model;

        SubstitutionAlignmentModel subModel = (SubstitutionAlignmentModel) model;
        final int iSize = unaligned.length() + 1;
        final int jSize = aligned.length() + 1;
        final int currentAlignmentLength = aligned.length();
        final int unalignedLength = unaligned.length();
        final float[][] edits = new float[iSize][jSize];
        final byte[][] backpointer = new byte[iSize][jSize];

        final int[] GAP = new int[model.features()];
        Arrays.fill(GAP, SubstitutionAlignmentModel.GAP_INDEX);

        m_costs = edits;
        m_backpointer = backpointer;

        // Gaps all the way through unaligned sequence
        for (int j = 1; j < jSize; j++)
        {
            final int prevJ = j - 1;
            edits[0][j] = edits[0][prevJ] + subModel.gapInsertionCost(aligned.features(prevJ), currentAlignmentLength); // GAP_COST;
            backpointer[0][j] = BACKPOINTER_UNALIGNED_GAP;
        }

        // Gaps all the way through aligned sequence
        for (int i = 1; i < iSize; i++)
        {
            final int prevI = i - 1;
            edits[i][0] = edits[prevI][0] + subModel.gapInsertionCost(unaligned.features(prevI), unalignedLength); // GAP_COST;
            backpointer[i][0] = BACKPOINTER_ALIGNED_GAP;
        }

        backpointer[0][0] = BACKPOINTER_SUBSTITUTION;

        for (int i = 1; i < iSize; i++)
        {
            final int prevI = i - 1;

            final float[] currentIEdits = edits[i];
            final float[] previousIEdits = edits[prevI];
            final int[] previousUnaligned = unaligned.features(prevI);

            for (int j = 1; j < jSize; j++)
            {
                final int prevJ = j - 1;
                final int[] previousAligned = aligned.features(prevJ);

                // Inserting a gap into unaligned sequence
                final float f1 = currentIEdits[prevJ] + subModel.gapInsertionCost(previousAligned, unalignedLength);

                // Inserting a gap into the aligned sequence
                final float f2 = previousIEdits[j]
                    + subModel.gapInsertionCost(previousUnaligned, currentAlignmentLength);

                // Substitution / match
                float f3 = previousIEdits[prevJ] + subModel.cost(previousAligned, previousUnaligned);

                if (f1 < f2)
                {
                    if (f1 < f3)
                    {
                        edits[i][j] = f1;
                        backpointer[i][j] = BACKPOINTER_UNALIGNED_GAP;
                    }
                    else
                    {
                        edits[i][j] = f3;
                        backpointer[i][j] = BACKPOINTER_SUBSTITUTION;
                    }
                }
                else if (f2 < f3)
                {
                    edits[i][j] = f2;
                    backpointer[i][j] = BACKPOINTER_ALIGNED_GAP;
                }
                else
                {
                    edits[i][j] = f3;
                    backpointer[i][j] = BACKPOINTER_SUBSTITUTION;
                }
            }
        }

        // Backtrace to reconstruct the winning alignment, adding to the beginning of a linked-list
        // to implicitly construct the buffer in order, while we traverse the alignment in reverse.
        final LinkedList<int[]> buffer = new LinkedList<int[]>();
        final IntList gapList = new IntArrayList(unaligned.length());

        final int[] gapVector = new int[unaligned.features()];
        Arrays.fill(gapVector, SubstitutionAlignmentModel.GAP_INDEX);

        int i = unaligned.length();
        int j = aligned.length();
        while (i > 0 || j > 0)
        {
            switch (backpointer[i][j])
            {
                case BACKPOINTER_SUBSTITUTION :
                    // Match
                    buffer.addFirst(unaligned.features(--i));
                    j--;
                    break;
                case BACKPOINTER_UNALIGNED_GAP :
                    // Gap in unaligned sequence
                    buffer.addFirst(gapVector);
                    j--;
                    break;
                case BACKPOINTER_ALIGNED_GAP :
                    // Gap in aligned sequence
                    buffer.addFirst(unaligned.features(--i));
                    gapList.add(j);
                    break;
                default :
                    throw new IllegalArgumentException("Should never get here");
            }
        }

        // Reverse gapList
        final int[] gapIndices = new int[gapList.size()];
        final IntListIterator gapIterator = gapList.listIterator();
        for (i = gapIndices.length - 1; i >= 0; i--)
        {
            gapIndices[i] = gapIterator.nextInt();
        }

        return new SequenceAlignment(new SimpleMappedSequence(buffer.toArray(new int[0][]), model.vocabularies()),
            gapIndices);
    }

    @Override
    public String toString()
    {
        int maxI = m_unaligned.length() + 1;
        int maxJ = m_aligned.length() + 1;

        Vocabulary vocabulary = m_model.vocabularies()[0];
        // TODO: Take length of mapped tokens into account in formatting

        StringBuffer sb = new StringBuffer(1024);
        sb.append("       ");
        for (int j = 0; j < maxJ; j++)
        {
            sb.append(String.format("%9s |", j > 0 ? vocabulary.map(m_aligned.feature(j - 1, 0)) : ""));
        }
        sb.append('\n');
        for (int i = 0; i < maxI; i++)
        {
            sb.append(String.format("%5s |", i > 0 ? vocabulary.map(m_unaligned.feature(i - 1, 0)) : ""));
            for (int j = 0; j < maxJ; j++)
            {
                float value = m_costs[i][j];
                String backpointer = null;
                switch (m_backpointer[i][j])
                {
                    case BACKPOINTER_SUBSTITUTION :
                        backpointer = "\\";
                        break;
                    case BACKPOINTER_UNALIGNED_GAP :
                        backpointer = "<";
                        break;
                    case BACKPOINTER_ALIGNED_GAP :
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
