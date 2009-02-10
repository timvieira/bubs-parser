package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.alignment.AlignmentModel;

/**
 * Represents a Position Specific Score Matrix (PSSM) for aligning a sequence with an alignment
 * profile or an existing alignment.
 * 
 * Each column in the model is assigned a specific probability of containing each token in the
 * vocabulary. When aligning, these probabilities can be used to assign a cost to placing a token in
 * a column.
 * 
 * @author Aaron Dunlop
 * @since Oct 7, 2008
 * 
 *        $Id$
 */
public interface PssmAlignmentModel extends AlignmentModel
{
    /**
     * Returns the negative log of the modeled probability of finding the specified feature vector
     * in the specified column
     * 
     * @param features
     * @param column
     * @return The negative log of the modeled probability
     */
    public float negativeLogP(int[] features, int column);

    /**
     * Returns the negative log of the modeled probability of finding the specified feature vector
     * in the specified column
     * 
     * TODO: Document better
     * 
     * @param features
     * @param column
     * @param featureIndices
     * @return The negative log of the modeled probability
     */
    public float negativeLogP(int[] features, int column, int[] featureIndices);

    /**
     * Returns the number of columns in this model.
     * 
     * @return Number of columns in this model.
     */
    public int columns();
}