/**
 * ModelAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.pssm.BasePssmAligner;
import edu.ohsu.cslu.alignment.pssm.LinearPssmAligner;
import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.math.linear.Matrix;


public class PssmAligner implements MultipleSequenceAligner
{
    private final BasePssmAligner aligner;

    public PssmAligner()
    {
        aligner = new LinearPssmAligner();
    }

    // TODO: PssmAligner doesn't need a distance matrix...

    @Override
    public MultipleSequenceAlignment align(MappedSequence[] unalignedSequences, Matrix distanceMatrix, AlignmentModel alignmentModel)
    {
        MultipleSequenceAlignment alignedSequences = new MultipleSequenceAlignment();
        for (int i = 0; i < unalignedSequences.length; i++)
        {
            alignedSequences.addSequence(aligner.align(unalignedSequences[i], (PssmAlignmentModel) alignmentModel), i);
        }
        return alignedSequences;
    }
}