package edu.ohsu.cslu.alignment.pairwise;

import java.util.Arrays;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;


/**
 * Globally aligns two strings.
 * 
 * TODO: Actually use the specified AlignmentModel
 * 
 * @author aarond
 * @since Mar 17, 2008
 * 
 * @version $Revision$
 */
public class FixedLengthDynamicAligner extends BaseDynamicAligner implements PairwiseAligner
{
    protected final static int MAX_TO_STRING_LENGTH = 4096;

    private final static int SUBSTITUTION_COST = 10;
    private final static int GAP_COST = 9;

    public SequenceAlignment alignPair(MappedSequence unaligned, MappedSequence aligned, AlignmentModel model)
    {
        if (unaligned.length() > aligned.length())
        {
            throw new RuntimeException("Length mismatch - unaligned: " + unaligned.length() + " > aligned: "
                + aligned.length());
        }

        final int iSize = unaligned.length() + 1;
        final int jSize = aligned.length() + 1;
        final float[][] costs = new float[iSize][jSize];

        m_aligned = aligned;
        m_unaligned = unaligned;
        m_costs = costs;
        m_model = model;

        for (int j = 1; j < jSize; j++)
        {
            costs[0][j] = costs[0][j - 1] + GAP_COST;
        }

        for (int i = 1; i < iSize; i++)
        {
            Arrays.fill(costs[i], 50000);
        }

        for (int i = 1; i < iSize; i++)
        {
            final int prevI = i - 1;
            final int maxJ = jSize - iSize + i + 1;

            final float[] currentIEdits = costs[i];
            final float[] previousIEdits = costs[prevI];
            final int previousUnalignedInt = unaligned.feature(prevI, 0);

            for (int j = i; j < maxJ; j++)
            {
                final int prevJ = j - 1;
                // Gap
                final float f1 = currentIEdits[prevJ] + GAP_COST;
                // Substitution or match
                float f2 = previousIEdits[prevJ];
                if (previousUnalignedInt != aligned.feature(prevJ, 0))
                {
                    f2 += SUBSTITUTION_COST;
                }

                currentIEdits[j] = Math.min(f1, f2);
            }
        }

        // Backtrace to reconstruct the winning alignment
        final int[][] alignment = new int[aligned.length()][];

        final int[] gapVector = new int[unaligned.features()];
        Arrays.fill(gapVector, SubstitutionAlignmentModel.GAP_INDEX);

        int i = unaligned.length();
        for (int j = aligned.length() - 1; j >= 0 && i > 0; j--)
        {
            // Gap
            final float f1 = costs[i][j];
            // Substitution or match
            final float f2 = costs[i - 1][j];

            // Default to emit if tied
            if (f2 <= f1)
            {
                // Emit
                alignment[j] = unaligned.features(--i);
            }
            else
            {
                // Gap
                alignment[j] = gapVector;
            }
        }
        // Insert gaps as needed at the beginning of the sequence
        for (int j = 0; j < alignment.length && alignment[j] == null; j++)
        {
            alignment[j] = gapVector;
        }

        return new SequenceAlignment(new SimpleMappedSequence(alignment, model.vocabularies()), new int[0]);
    }
}
