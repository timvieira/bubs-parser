package edu.ohsu.cslu.alignment.pssm;

import java.util.Arrays;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.math.linear.IntVector;
import edu.ohsu.cslu.math.linear.Vector;

/**
 * Implements {@link PssmSequenceAligner} using linear space for the intermediate storage of scores
 * (O(mn) space is still required for the backtrace-array)
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class LinearPssmAligner extends FullPssmAligner
{
    @Override
    public final MappedSequence align(final MappedSequence sequence, PssmAlignmentModel model, int[] features)
    {
        final int maxI = sequence.length() + 1;
        final int maxJ = model.columns() + 1;

        float[] current = new float[maxJ];
        float[] previous = new float[maxJ];
        // TODO: Replace with a PackedIntVector and profile
        final byte[][] backpointer = m_backPointer = new byte[maxI][maxJ];

        final Vector gapVector = model.gapVector();

        Arrays.fill(current, Float.MAX_VALUE);

        // Initialize all the 'start' columns - deleting all the way through
        previous[0] = 0f;
        for (int j = 1; j < maxJ; j++)
        {
            previous[j] = previous[j - 1] + model.cost(gapVector, j - 1, features);
        }
        Arrays.fill(backpointer[0], BACKPOINTER_UNALIGNED_GAP);

        // Choose min of (emit, gap) (current element / gap symbol)
        // Emit should be i-1, j-1, gap in unaligned sequence i, j-1.

        for (int i = 1; i < maxI; i++)
        {
            final int prevI = i - 1;
            final Vector currentElement = sequence.elementAt(prevI);
            final byte[] backpointerI = backpointer[i];

            for (int j = i; j < maxJ; j++)
            {
                final int prevJ = j - 1;

                // Probability of emission / gap
                final float emit = previous[prevJ] + model.cost(currentElement, prevJ, features);
                // TODO: Pre-compute and cache gap probabilities?
                final float gap = current[prevJ] + model.cost(gapVector, prevJ, features);

                // Bias toward emission given equal probabilities
                if (emit <= gap)
                {
                    current[j] = emit;
                    backpointerI[j] = BACKPOINTER_SUBSTITUTION;
                }
                else
                {
                    current[j] = gap;
                    backpointerI[j] = BACKPOINTER_UNALIGNED_GAP;
                }
            }

            final float[] tmp = previous;
            previous = current;
            current = tmp;
            Arrays.fill(current, Float.MAX_VALUE);
            // current[i] = Float.MAX_VALUE;
        }

        return backtrace(sequence, model, backpointer, gapVector);
    }

    @Override
    public SequenceAlignment alignWithGaps(MappedSequence sequence, HmmAlignmentModel model, int[] features)
    {
        final int maxI = sequence.length() + 1;
        final int maxJ = model.columns() + 1;

        float[] current = new float[maxJ];
        float[] previous = new float[maxJ];
        // TODO: Replace with a PackedIntVector and profile
        final byte[][] backpointer = new byte[maxI][maxJ];

        final Vocabulary[] vocabularies = model.vocabularies();
        final IntVector gapVector = new IntVector(vocabularies.length);
        for (int i = 0; i < vocabularies.length; i++)
        {
            gapVector.set(i, ((AlignmentVocabulary) vocabularies[i]).gapSymbol());
        }

        Arrays.fill(current, Float.MAX_VALUE);

        // Initialize all the 'start' columns - probabilities of gaps all the way through
        for (int j = 1; j < maxJ; j++)
        {
            previous[j] = previous[j - 1] + model.cost(gapVector, j - 1, features);
        }
        Arrays.fill(backpointer[0], BACKPOINTER_UNALIGNED_GAP);

        // Choose min of (emit, gap in unaligned sequence, gap in pssm)
        // Emit should be i-1, j-1, gap in unaligned sequence i, j-1, gap in pssm i-1,j
        for (int i = 1; i < maxI; i++)
        {
            final int prevI = i - 1;
            final Vector currentElement = sequence.elementAt(prevI);
            final byte[] backpointerI = backpointer[i];

            current[0] = previous[0] + model.pssmGapInsertionCost(currentElement);
            backpointerI[0] = BACKPOINTER_PSSM_GAP;

            for (int j = 1; j < maxJ; j++)
            {
                final int prevJ = j - 1;

                // Probability of emission / gap in unaligned / gap in pssm
                final float emit = previous[prevJ] + model.cost(currentElement, prevJ, features);
                // TODO: Pre-compute and cache gap probabilities?
                final float gapInNewSequence = current[prevJ] + model.cost(gapVector, prevJ, features);
                final float gapInPssm = previous[j] + model.pssmGapInsertionCost(currentElement);

                // Bias toward emission given equal probabilities
                if (emit <= gapInPssm && emit <= gapInNewSequence)
                {
                    current[j] = emit;
                    backpointerI[j] = BACKPOINTER_SUBSTITUTION;
                }
                else if (gapInNewSequence <= gapInPssm)
                {
                    current[j] = gapInNewSequence;
                    backpointerI[j] = BACKPOINTER_UNALIGNED_GAP;
                }
                else
                {
                    current[j] = gapInPssm;
                    backpointerI[j] = BACKPOINTER_PSSM_GAP;
                }
            }

            final float[] tmp = previous;
            previous = current;
            current = tmp;
        }

        return backtraceWithGaps(sequence, model, backpointer, gapVector);
    }
}
