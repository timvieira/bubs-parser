package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.common.MappedSequence;

/**
 * A character-based {@link AlignmentVocabulary} for a particular domain (e.g. A, C, G, and T for
 * DNA).
 * 
 * @author Aaron Dunlop
 * @since Jul 4, 2008
 * 
 *        TODO: Add a 'gap' character or string
 * 
 *        $Id$
 */
public interface CharVocabulary extends AlignmentVocabulary
{
    /**
     * Returns the characters supported by this alphabet
     * 
     * @return characters supported by this alphabet.
     */
    public char[] charVocabulary();

    /**
     * Returns the delete symbol of this alphabet.
     * 
     * @return the delete symbol of this alphabet.
     */
    public char charDeleteSymbol();

    /**
     * Maps a character from the current alphabet to an integer index, allowing efficient array
     * storage.
     * 
     * @param c
     * @return integer index
     */
    public int mapCharacter(char c);

    /**
     * Maps an integer array index to a character from the current alphabet.
     * 
     * @param i
     * @return mapped character
     */
    public char mapIndex(int i);

    /**
     * Maps a string to a sequence of characters from the current alphabet to an array of integer
     * indices.
     * 
     * @param sequence The string to map
     * @return Mapped sequence.
     */
    public MappedSequence mapSequence(String sequence);

    /**
     * Maps a sequence of indices to a String.
     * 
     * @param sequence The sequence to map
     * @return Mapped String.
     */
    public String mapSequence(MappedSequence sequence);
}
