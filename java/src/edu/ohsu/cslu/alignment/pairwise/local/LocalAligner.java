package edu.ohsu.cslu.alignment.pairwise.local;

import java.util.Arrays;

/**
 * TODO: It seems there's no difference between a local aligner and an approximate pattern matcher,
 * which we already have.
 *
 * @author Aaron Dunlop
 * @since Jul 5, 2008
 *
 *        $Id$
 */
public abstract class LocalAligner
{
    /** Stored during alignments() so toString() can format reasonably */
    protected String m_currentPattern;

    /** Stored during alignments() so toString() can format reasonably */
    protected String m_text;

    protected final static int MAX_TO_STRING_LENGTH = 4096;

    protected LocalAligner()
    {}

    /**
     * Returns alignments found (if any) between the pattern and the text.
     *
     * @param pattern The pattern to align
     * @param text The text to align against
     * @param edits The number of edits to allow in alignments
     * @return The alignments found (if any)
     */
    public abstract java.util.Set<TextAlignment> alignments(String pattern, String text, int edits);

    /**
     * Returns alignments found (if any) between the pattern and the text.
     *
     * @param pattern The pattern to align
     * @param text The text to align against
     * @param length The minimum length of alignment to look for
     * @param edits The number of edits to allow in alignments
     * @return The alignments found (if any)
     */
    public abstract java.util.Set<TextAlignment> alignments(String pattern, String text, int length, int edits);

    protected String spaces(int count)
    {
        if (count < 0)
        {
            return "";
        }

        char[] buf = new char[count];
        Arrays.fill(buf, ' ');
        return new String(buf);
    }
}
