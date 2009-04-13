/**
 * MultipleSequenceAligner.java
 */
package edu.ohsu.cslu.alignment.multiple;

import java.io.IOException;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.matrices.Matrix;

/**
 * TODO: Document interface
 * 
 * @author Aaron Dunlop
 * @since Jul 10, 2008
 * 
 *        $Id$
 */
public interface MultipleSequenceAligner
{
    public MultipleSequenceAlignment align(MappedSequence[] unalignedSequences, Matrix distanceMatrix,
        AlignmentModel alignmentModel) throws IOException;
}