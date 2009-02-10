package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.alignment.pssm.PssmAlignmentModel;

public interface CharPssmAlignmentModel extends PssmAlignmentModel
{
    /**
     * Returns the modeled probability of finding the specified character in the specified column
     * 
     * @param c
     * @param column
     * @return The modeled probability of finding the specified character in the specified column
     */
    public float p(char c, int column);

    /**
     * Returns the negative log of the modeled probability of finding the specified character in the
     * specified column
     * 
     * @param c
     * @param column
     * @return The negative log of the modeled probability
     */
    public float negativeLogP(char c, int column);

    /**
     * Returns the {@code Alphabet} this model is representing.
     * 
     * @return the {@code Alphabet} this model is representing.
     */
    public CharVocabulary vocabulary();
}
