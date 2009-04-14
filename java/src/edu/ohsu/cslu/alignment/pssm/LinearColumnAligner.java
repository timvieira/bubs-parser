package edu.ohsu.cslu.alignment.pssm;

import java.util.Arrays;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Implements {@link ColumnSequenceAligner} using linear space for the intermediate storage of
 * scores (O(mn) space is still required for the backtrace-array)
 * 
 * @author Aaron Dunlop
 * @since Nov 5, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public final class LinearColumnAligner extends FullColumnAligner
{

    @Override
    public SequenceAlignment align(MappedSequence sequence, ColumnAlignmentModel model, int[] features)
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

        // Choose min of (emit, gap in unaligned sequence, insert column)
        // Emit should be i-1, j-1, gap in unaligned sequence i, j-1, insert column i-1,j
        for (int i = 1; i < maxI; i++)
        {
            final int prevI = i - 1;
            final Vector currentElement = sequence.elementAt(prevI);
            final byte[] backpointerI = backpointer[i];

            current[0] = previous[0] + model.columnInsertionCost(currentElement);
            backpointerI[0] = BACKPOINTER_INSERT_COLUMN;

            for (int j = 1; j < maxJ; j++)
            {
                final int prevJ = j - 1;

                // Probability of emission / gap in unaligned / insert column
                final float emit = previous[prevJ] + model.cost(currentElement, prevJ, features);
                // TODO: Pre-compute and cache gap probabilities?
                final float gapInNewSequence = current[prevJ] + model.cost(gapVector, prevJ, features);
                final float gapInPssm = previous[j] + model.columnInsertionCost(currentElement);

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
                    backpointerI[j] = BACKPOINTER_INSERT_COLUMN;
                }
            }

            final float[] tmp = previous;
            previous = current;
            current = tmp;
        }

        return backtrace(sequence, model, backpointer, gapVector);
    }
}
