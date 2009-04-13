package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.math.linear.Vector;

/**
 * Represents a column-based alignment model; used when aligning a {@link Sequence} with an
 * alignment profile or an existing multiple-sequence-alignment.
 * 
 * Some {@link PssmAlignmentModel} implementations will represent a Position Specific Score Matrix
 * (PSSM), in which the number of columns is fixed, calculating a single cost to aligning a
 * feature-vector in each column.
 * 
 * Other implementations will also allow inserting additional columns into the model, again with a
 * cost. Column insertion is generally rare, so this cost would often be considerable.
 * 
 * TODO Collapse PssmAlignmentModel and HmmAlignmentModel into ColumnAlignmentModel
 * 
 * @author Aaron Dunlop
 * @since Oct 7, 2008
 * 
 * @version $Id$
 */
public interface PssmAlignmentModel extends AlignmentModel
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
     * @return a 'gap' feature vector appropriate for this alignment model
     */
    public Vector gapVector();
}