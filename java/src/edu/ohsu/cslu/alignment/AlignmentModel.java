package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.alignment.column.ColumnAlignmentModel;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * A model used for aligning sequences with one another.
 * 
 * The responsibilities of an {@link AlignmentModel} is to assign a cost to aligning a particular
 * token (or a set of features representing a token) at each point in the dynamic alignment process.
 * 
 * For a column-based alignment (i.e., when aligning a new {@link Sequence} with an existing MSA),
 * that cost will depend on the feature vector and on the column index. (see
 * {@link ColumnAlignmentModel}).
 * 
 * For a pairwise alignment (i.e., when aligning one {@link Sequence} with another), that cost will
 * depend on comparing a pair of feature vectors from the two sequences. (see
 * {@link SubstitutionAlignmentModel}).
 * 
 * @author Aaron Dunlop
 * @since Oct 10, 2008
 * 
 *        $Id$
 */
public interface AlignmentModel
{
    /**
     * 'Special' index reserved to represent a gap
     * 
     * TODO: Should this be a part of the vocabulary instead of a part of the model?
     */
    public final static int GAP_INDEX = 0;

    /**
     * @return the vocabulary this alignment model is based on
     */
    public Vocabulary[] vocabularies();

    /**
     * @return the number of features represented in this model.
     */
    public int featureCount();

    /**
     * @return a 'gap' feature vector appropriate for this alignment model
     */
    public Vector gapVector();

}
