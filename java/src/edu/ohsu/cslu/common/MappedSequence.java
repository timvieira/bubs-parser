package edu.ohsu.cslu.common;

/**
 * Represents an ordered sequence of tokens, possibly with multiple features for each token. The
 * first feature is generally assumed to be a word; other features such as POS tags, HEAD tags, or
 * segment BEGIN/CONTINUE/END are possible.
 * 
 * All tokens are mapped to ints using a {@link Vocabulary} for efficient access.
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public interface MappedSequence extends Sequence
{
    /**
     * @param index
     * @return the integer representation of the token at the specified index
     */
    public int feature(int index, int featureIndex);

    /**
     * @param index
     * @return the integer representation of the token at the specified index
     */
    public int[] features(int index);

    /**
     * @return the vocabulary used to map the specified feature
     */
    public Vocabulary vocabulary(int featureIndex);

    /**
     * @return the vocabularies used to map this sequence
     */
    public Vocabulary[] vocabularies();

    /**
     * Returns a new sequence with gaps inserted at the specified indices
     * 
     * @param gapIndices
     * @return a copy of this sequence with gaps inserted at the specified indices
     */
    public MappedSequence insertGaps(int[] gapIndices);

    /**
     * @return a copy of this sequence with any gaps removed
     */
    public MappedSequence removeAllGaps();

    /**
     * Formats this sequence as a String, using the supplied formatting Strings
     * 
     * @param formats
     * @return String visualization of the sequence
     */
    public String toColumnString(String[] formats);
}
