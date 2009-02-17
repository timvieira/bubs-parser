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

/**
 * An iterative pairwise aligner which inserts gaps into the alignment itself, allowing the MSA to
 * expand in an effort to create the best possible alignment. In general, the alignment runtime will
 * be considerably longer than that of @link {@link FixedLengthIterativePairwiseAligner}
 * 
 * TODO: Tune - we lost a lot of speed again somewhere
 * 
 * @author Aaron Dunlop
 * @since Feb 17, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class VariableLengthIterativePairwiseAligner extends BaseMultipleSequenceAligner
{
    public VariableLengthIterativePairwiseAligner()
    {
        aligner = new VariableLengthDynamicAligner();
    }

    /**
     * Assumes one of the sequences has already been aligned into the aligned set.
     * 
     * @param alignedSequence
     * @param unalignedSequence
     * @param alignmentModel
     * @return Multiple sequence alignment
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