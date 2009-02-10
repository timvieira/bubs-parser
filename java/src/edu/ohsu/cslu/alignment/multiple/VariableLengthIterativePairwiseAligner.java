/**
 * VariableLengthIterativePairwiseAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;


import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.alignment.pairwise.VariableLengthDynamicAligner;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.SimpleMappedSequence;
import edu.ohsu.cslu.math.linear.Matrix;

// TODO: Tune - we lost a lot of speed again somewhere
public class VariableLengthIterativePairwiseAligner extends BaseMultipleSequenceAligner
{
    public VariableLengthIterativePairwiseAligner()
    {
        aligner = new VariableLengthDynamicAligner();
    }

    /**
     * Assumes one of the sequences has already been aligned into the aligned set.
     * 
     * @param pair
     * @return
     */
    @Override
    protected SequenceAlignment align(MappedSequence alignedSequence, MappedSequence unalignedSequence,
        final AlignmentModel alignmentModel)
    {
        return aligner.alignPair(unalignedSequence, alignedSequence, alignmentModel);
    }

    /**
     * Inserts gaps at the specified indices into a set of sequences
     * 
     * @param sequences
     * @param gapIndices
     */
    public static void insertGaps(MappedSequence[] sequences, int[] gapIndices)
    {
        if (gapIndices.length == 0)
        {
            return;
        }

        for (int i = 0; i < sequences.length; i++)
        {
            SimpleMappedSequence simpleSequence = (SimpleMappedSequence) sequences[i];
            if (simpleSequence != null)
            {
                sequences[i] = simpleSequence.insertGaps(gapIndices);
            }
        }
    }

    @Override
    protected int firstSequenceToAlign(MappedSequence[] sequences, Matrix distanceMatrix)
    {
        return distanceMatrix.argMin()[0];
    }
}