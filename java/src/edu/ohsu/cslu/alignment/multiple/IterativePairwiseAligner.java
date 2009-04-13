/**
 * IterativePairwiseAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.pairwise.FullDynamicPairwiseAligner;
import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * A fairly standard iterative pairwise aligner. Iterates through a set of unaligned sequences,
 * choosing at each step the unaligned sequence which is 'closest' (as defined by a supplied
 * distance matrix) to an already-aligned sequence. Performs pairwise alignment of those two
 * sequences using {@link FullDynamicPairwiseAligner}.
 * 
 * TODO: Tune - we lost a lot of speed again somewhere
 * 
 * @author Aaron Dunlop
 * @since Feb 17, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class IterativePairwiseAligner extends BaseMultipleSequenceAligner
{
    public IterativePairwiseAligner()
    {
        aligner = new FullDynamicPairwiseAligner();
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
    public static void insertGaps(Sequence[] sequences, int[] gapIndices)
    {
        if (gapIndices.length == 0)
        {
            return;
        }

        for (int i = 0; i < sequences.length; i++)
        {
            MultipleVocabularyMappedSequence simpleSequence = (MultipleVocabularyMappedSequence) sequences[i];
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