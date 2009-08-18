package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.common.Vocabulary;

/**
 * Extends {@link Vocabulary} for use in alignment.
 * 
 * @author Aaron Dunlop
 * @since Oct 6, 2008
 * 
 *        $Id$
 */
public interface AlignmentVocabulary extends Vocabulary
{
    /**
     * Returns the delete symbol of this vocabulary.
     * 
     * @return the delete symbol of this vocabulary.
     */
    public int gapSymbol();

    /**
     * Returns true if the specified token is considered 'rare' and should be treated in the same
     * class as unknown tokens.
     * 
     * @param token
     * @return true if the specified token is 'rare'
     */
    public boolean isRareToken(String token);

    /**
     * Returns true if the specified token is considered 'rare' and should be treated in the same
     * class as unknown tokens.
     * 
     * @param index
     * @return true if the specified token is 'rare'
     */
    public boolean isRareToken(int index);

}
