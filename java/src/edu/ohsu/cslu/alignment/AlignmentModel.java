package edu.ohsu.cslu.alignment;


import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;
import edu.ohsu.cslu.common.Vocabulary;

/**
 * A model used for aligning sequences with one another. The two basic alignment approaches are a
 * profile-based alignment such as {@link PssmAlignmentModel}, in which each token has a probability
 * of occurrence in each column, and a substitution-based alignment model such as
 * {@link SubstitutionAlignmentModel} in which each insertion, deletion, and substitution is
 * penalized by a cost.
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
    public int features();

}
