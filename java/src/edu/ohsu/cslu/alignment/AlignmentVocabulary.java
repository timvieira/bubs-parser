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

}
