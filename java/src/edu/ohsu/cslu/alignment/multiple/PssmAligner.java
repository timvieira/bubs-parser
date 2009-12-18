/**
 * ModelAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.column.BaseColumnAligner;
import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.alignment.column.LinearColumnAligner;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

public class PssmAligner implements MultipleSequenceAligner {

    private final BaseColumnAligner aligner;

    public PssmAligner() {
        aligner = new LinearColumnAligner();
    }

    // TODO: PssmAligner doesn't need a distance matrix...

    @Override
    public MultipleSequenceAlignment align(MappedSequence[] unalignedSequences, Matrix distanceMatrix,
            AlignmentModel alignmentModel) {
        MultipleSequenceAlignment alignedSequences = new MultipleSequenceAlignment();
        for (int i = 0; i < unalignedSequences.length; i++) {
            alignedSequences.addSequence(aligner.align(unalignedSequences[i],
                (ColumnAlignmentModel) alignmentModel).alignedSequence(), i);
        }
        return alignedSequences;
    }
}