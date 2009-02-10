/**
 *
 */
package edu.ohsu.cslu.alignment.pairwise.local;

/**
 * Represents an alignment between a pattern and a text
 *
 * @author Aaron Dunlop
 * @since Jun 27, 2008
 *
 * $Id$
 */
public class TextAlignment implements Cloneable, Comparable<TextAlignment>
{
    public int textBegin;
    public int textEnd;
    public int patternBegin;
    public int patternEnd;

    public TextAlignment(int textBegin, int textEnd, int patternBegin, int patternEnd)
    {
        this.textBegin = textBegin;
        this.textEnd = textEnd;
        this.patternBegin = patternBegin;
        this.patternEnd = patternEnd;
    }

    @Override
    protected Object clone()
    {
        return new TextAlignment(textBegin, textEnd, patternBegin, patternEnd);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (obj == this)
        {
            return true;
        }

        if (obj == null || !(obj instanceof TextAlignment))
        {
            return false;
        }

        TextAlignment other = (TextAlignment) obj;
        return (other.textBegin == textBegin && other.textEnd == textEnd && other.patternBegin == patternBegin && other.patternEnd == patternEnd);
    }

    @Override
    public String toString()
    {
        return textBegin + "," + textEnd + "-" + patternBegin + "," + patternEnd;
    }

    public int compareTo(TextAlignment o)
    {
        return toString().compareTo(o.toString());
    }
}