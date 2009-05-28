package edu.ohsu.cslu.common;

import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Represents an ordered sequence of tokens, possibly with multiple features for each token. The
 * first feature is generally assumed to be a word; other features such as POS tags, head_verb tags,
 * or segment BEGIN/CONTINUE/END are possible.
 * 
 * TODO: Implement Iterable<Vector>? or Iterator<String> iterator(featureIndex)?
 * 
 * @author Aaron Dunlop
 * @since Dec 15, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public interface Sequence
{
    /**
     * @param index
     * @return the String representation of the token at the specified index
     * 
     *         TODO: This doesn't make a lot of sense for FeatureMappedSequence - we'll very often
     *         have to return null
     */
    public String stringFeature(int index, int featureIndex);

    /**
     * @param index
     * @return the String representation of the token at the specified index
     */
    public String[] stringFeatures(int index);

    /**
     * @param index
     * @return element at the specified index
     */
    public Vector elementAt(int index);

    /**
     * @return the length of the sequence
     */
    public int length();

    /**
     * @return the number of features represented for each element of this sequence
     */
    public int featureCount();

    /**
     * @param features feature indices
     * @return a copy of this sequence containing only the specified features.
     */
    public Sequence retainFeatures(int... features);

    /**
     * Returns a new sequence that is a subsequence of this sequence.
     * 
     * @param beginIndex the begin index, inclusive.
     * @param endIndex the end index, exclusive.
     * @return the specified subsequence.
     * 
     * @throws IndexOutOfBoundsException if <tt>beginIndex</tt> or <tt>endIndex</tt> are negative,
     *             if <tt>endIndex</tt> is greater than <tt>length()</tt>, or if <tt>beginIndex</tt>
     *             is greater than <tt>startIndex</tt>
     */
    public Sequence subSequence(int beginIndex, int endIndex);

    // /**
    // * Inserts the specified features at the specified location.
    // *
    // * @param features
    // * @param index
    // * @return the new sequence created
    // */
    // public Sequence insert(String[] features, int index);

    /**
     * Returns the sequence formatted using parenthesis-delimited brackets (e.g.
     * "(word1 pos1) (word2 pos2) ... (wordn posn)").
     * 
     * @return parenthesis-formatted string representation of the sequence
     */
    public String toBracketedString();

    /**
     * Returns the sequence formatted using slash-delimited features (e.g.
     * "word1/pos1 word2/pos2 ... wordn/posn"). This is the format used by the Stanford toolset,
     * including their POS tagger.
     * 
     * @return Slash-formatted string representation of the sequence
     */
    public String toSlashSeparatedString();

    /**
     * Splits this sequence into sentences (single-sentence sequences should return a reference to
     * themselves)
     * 
     * @return sentences
     */
    public Sequence[] splitIntoSentences();

    /**
     * Returns a new sequence with gaps inserted at the specified indices
     * 
     * @param gapIndices
     * @return a copy of this sequence with gaps inserted at the specified indices
     */
    public Sequence insertGaps(int[] gapIndices);

    /**
     * @return a copy of this sequence with any gaps removed
     */
    public Sequence removeAllGaps();

}
