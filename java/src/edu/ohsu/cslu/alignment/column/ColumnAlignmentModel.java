package edu.ohsu.cslu.alignment.column;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Represents a column-based alignment model; used when aligning a {@link Sequence} with a
 * position-specific alignment profile or an existing multiple-sequence-alignment.
 * 
 * Some {@link ColumnAlignmentModel} implementations will allow (for a cost) insertion of additional
 * columns into the model. This cost is computed by the {@link #columnInsertionCost(Vector)} method.
 * Column insertion is generally rare, so this cost will often be considerable.
 * 
 * Other implementations will represent a Position Specific Score Matrix (PSSM), in which the number
 * of columns is fixed, In this case, {@link #columnInsertionCost(Vector)} should return
 * Float.POSITIVE_INFINITY.
 * 
 * @author Aaron Dunlop
 * @since Feb 9, 2009
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface ColumnAlignmentModel extends AlignmentModel
{
    /**
     * Returns the cost of aligning the specified feature-vector into the specified column. This
     * cost will often (but not always) be the same as the negative log of the modeled probability
     * of finding the feature-vector in the specified column.
     * 
     * @param featureVector
     * @param column
     * @return The negative log of the modeled probability
     */
    public float cost(Vector featureVector, int column);

    /**
     * Returns the cost of aligning the specified feature-vector into the specified column. This
     * cost will often (but not always) be the same as the negative log of the modeled probability
     * of finding the feature-vector in the specified column.
     * 
     * This cost is calculated using a subset of the available features, as specified by the
     * <code>featureIndices</code> parameter.
     * 
     * TODO: Document better
     * 
     * @param featureVector
     * @param column
     * @param featureIndices Subset of available features
     * @return The negative log of the modeled probability
     */
    public float cost(Vector featureVector, int column, int[] featureIndices);

    /**
     * Returns the number of columns in this model.
     * 
     * @return Number of columns in this model.
     */
    public int columns();

    /**
     * Returns the cost of inserting a gap into the PSSM itself. Some alignment methods allow this
     * (even though that changes the existing model, making it less of a <i>position specific</i>
     * score matrix...)
     * 
     * @param featureVector The features which would be aligned with the newly-inserted gap
     * @return Cost of gap insertion
     */
    public float columnInsertionCost(Vector featureVector);

}
