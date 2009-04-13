package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.math.linear.Vector;

/**
 * Represents a substitution-cost based alignment model, in which each substitution, deletion, or
 * gap insertion is penalized by a particular cost.
 * 
 * Implementations may use hand-tuned or induced cost matrices or (potentially) more complex cost
 * functions.
 * 
 * @author Aaron Dunlop
 * @since Oct 7, 2008
 * 
 *        $Id$
 */
public interface SubstitutionAlignmentModel extends AlignmentModel
{
    /**
     * Returns the cost of substituting one feature for another.
     * 
     * @param alignedFeature
     * @param unalignedFeature
     * @return substitution cost
     */
    public float cost(int alignedFeature, int unalignedFeature);

    /**
     * Returns the cost of inserting a gap in an alignment of the specified length. The gap
     * insertion cost is the same when inserting a gap into an already-aligned sequence (in which
     * case the relevant length is that of the existing alignment) or when inserting the gap into
     * the unaligned sequence (in which case the relevant length is that of the unaligned sequence).
     * 
     * The cost of gap insertion may depend on the feature the potential gap would align with.
     * 
     * @param feature
     * @param sequenceLength
     * @return gap insertion cost
     */
    public float gapInsertionCost(int feature, int sequenceLength);

    /**
     * Returns the cost of substituting one feature vector for another.
     * 
     * @param alignedVector
     * @param unalignedVector
     * @return substitution cost
     */
    public float cost(Vector alignedVector, Vector unalignedVector);

    /**
     * Returns the cost of inserting a gap in an alignment of the specified length. The gap
     * insertion cost is the same when inserting a gap into an already-aligned sequence (in which
     * case the relevant length is that of the existing alignment) or when inserting the gap into
     * the unaligned sequence (in which case the relevant length is that of the unaligned sequence).
     * 
     * The cost of gap insertion may depend on the feature the potential gap would align with.
     * 
     * @param featureVector
     * @param sequenceLength
     * @return gap insertion cost
     */
    public float gapInsertionCost(Vector featureVector, int sequenceLength);
}
