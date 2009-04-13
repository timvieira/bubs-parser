package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.math.linear.Vector;

/**
 * Implements a hybrid PSSM / Substitution Matrix alignment model.
 * 
 * TODO: Better documentation
 * 
 * @author Aaron Dunlop
 * @since Feb 9, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface HmmAlignmentModel extends PssmAlignmentModel
{
    /**
     * Returns the cost of inserting a gap into the PSSM itself. Some alignment methods allow this
     * (even though that changes the existing model, making it less of a <i>position specific</i>
     * score matrix...)
     * 
     * @param featureVector The features which would be aligned with the newly-inserted gap
     * @return Cost of gap insertion
     */
    public float pssmGapInsertionCost(Vector featureVector);

}
