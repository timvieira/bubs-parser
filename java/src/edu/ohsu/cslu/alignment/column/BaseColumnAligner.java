package edu.ohsu.cslu.alignment.column;

import edu.ohsu.cslu.alignment.pairwise.SequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;

/**
 * Aligns a sequence according to a Position-Specific Score Matrix by inserting gaps in the sequence
 * to match the modeled length.
 * 
 * @author Aaron Dunlop
 * @since Jul 4, 2008
 * 
 *        $Id$
 */
public abstract class BaseColumnAligner implements ColumnSequenceAligner
{
    // 0 = substitution, 1 = gap in unaligned sequence, 2 = insert column
    protected final static byte BACKPOINTER_SUBSTITUTION = 0;
    protected final static byte BACKPOINTER_UNALIGNED_GAP = 1;
    protected final static byte BACKPOINTER_INSERT_COLUMN = 2;

    @Override
    public SequenceAlignment align(MappedSequence sequence, ColumnAlignmentModel model)
    {
        int[] features = new int[model.features()];
        for (int i = 0; i < features.length; i++)
        {
            features[i] = i;
        }
        return align(sequence, model, features);
    }

    /**
     * Returns the number of character matches found between the two strings supplied. (e.g.
     * matches("aaa", "aaa") will return 3, and matches("abc", "abd") will return 2).
     * 
     * The two strings must be the same length.
     * 
     * Note that matches(x, y) / x.length() will yield a percentage accuracy.
     * 
     * @param expected
     * @param actual
     * @return Number of character matches found between the two strings
     */
    public int matches(String expected, String actual)
    {
        if (expected == null || actual == null || expected.length() != actual.length())
        {
            throw new RuntimeException("String lengths must match");
        }

        int matches = 0;
        for (int i = 0; i < expected.length(); i++)
        {
            if (actual.charAt(i) == expected.charAt(i))
            {
                matches++;
            }
        }
        return matches;
    }
}
