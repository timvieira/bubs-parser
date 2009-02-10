package edu.ohsu.cslu.alignment.pairwise.local;

import java.util.Set;
import java.util.TreeSet;

/**
 * TODO: I believe this functionality duplicates that in the matching.approximate package
 * 
 * @author Aaron Dunlop
 * @since Oct 2, 2008
 * 
 *        $Id$
 */
public class FullDynamicLocalAligner extends LocalAligner
{
    private int m_edits[][];

    @Override
    public Set<TextAlignment> alignments(String pattern, String text, int length, int edits)
    {
        m_text = text;
        m_currentPattern = pattern;

        int iSize = pattern.length() + 1;
        int jSize = text.length() + 1;
        m_edits = new int[iSize][jSize];
        int[][] start = new int[iSize][jSize];
        for (int j = 0; j < jSize; j++)
        {
            start[0][j] = j;
        }

        // Note: Java arrays are initialized to 0, so
        // m_edits[i][0] == 0 for all i and
        // m_edits[0][j] == 0 for all j

        for (int i = 1; i < iSize; i++)
        {
            for (int j = 1; j < jSize; j++)
            {
                int f1 = m_edits[i][j - 1] - 100;
                int f2 = m_edits[i - 1][j] - 100;
                int m = (m_currentPattern.charAt(i - 1) == m_text.charAt(j - 1) ? 100 : -99);
                int f3 = m_edits[i - 1][j - 1] + m;
                int f4 = 0;

                int f = Math.max(Math.max(f1, f2), Math.max(f3, f4));
                m_edits[i][j] = f;
                if (f == f3)
                {
                    start[i][j] = start[i - 1][j - 1];
                }
                else if (f == f2)
                {
                    start[i][j] = start[i - 1][j];
                }
                else if (f == f1)
                {
                    start[i][j] = start[i][j - 1];
                }
                else if (f == f4)
                {
                    start[i][j] = j;
                }
            }
        }

        int minValidAlignment = (length - 2 * edits) * 100;
        int maxI = iSize - 1;

        Set<TextAlignment> alignments = new TreeSet<TextAlignment>();
        for (int i = length; i <= maxI; i++)
        {
            for (int j = length - edits; j < jSize; j++)
            {
                int currentScore = m_edits[i][j];
                int currentStart = start[i][j];
                int alignmentLength = j - currentStart;
                if (currentScore >= minValidAlignment && alignmentLength >= length)
                {
                    // Try to eliminate spurious duplicate alignments
                    int previousScore = j > 0 ? m_edits[maxI][j - 1] : Integer.MIN_VALUE;
                    int nextScore = j < jSize - 1 ? m_edits[maxI][j + 1] : Integer.MIN_VALUE;
                    if (currentScore >= previousScore && currentScore >= nextScore)
                    {
                        alignments.add(new TextAlignment(j - alignmentLength, j, 0, Math.min(alignmentLength, pattern
                            .length())));
                    }
                }
            }
        }
        return alignments;
    }

    @Override
    public Set<TextAlignment> alignments(String pattern, String text, int edits)
    {
        return alignments(pattern, text, pattern.length(), edits);
    }

    // TODO: proper alignments are returned, but locations are misidentified
    public Set<TextAlignment> selfAlignments(String text, int length, int edits)
    {
        m_text = text;
        m_currentPattern = text;

        int iSize = text.length() + 1;
        int jSize = text.length() + 1;
        m_edits = new int[iSize][jSize];
        int[][] start = new int[iSize][jSize];
        for (int j = 0; j < jSize; j++)
        {
            start[0][j] = j;
        }

        // Note: Java arrays are initialized to 0, so
        // m_edits[i][0] == 0 for all i and
        // m_edits[0][j] == 0 for all j

        for (int i = 1; i < iSize; i++)
        {
            for (int j = 1; j < jSize; j++)
            {
                int f1 = m_edits[i][j - 1] - 100;
                int f2 = m_edits[i - 1][j] - 100;
                int m = (m_currentPattern.charAt(i - 1) == m_text.charAt(j - 1) ? 100 : -99);
                int f3 = m_edits[i - 1][j - 1] + m;
                int f4 = 0;

                int f = Math.max(Math.max(f1, f2), Math.max(f3, f4));
                m_edits[i][j] = (i == j ? 0 : f);
                if (f == f3)
                {
                    start[i][j] = start[i - 1][j - 1];
                }
                else if (f == f2)
                {
                    start[i][j] = start[i - 1][j];
                }
                else if (f == f1)
                {
                    start[i][j] = start[i][j - 1];
                }
                else if (f == f4)
                {
                    start[i][j] = j;
                }
            }
        }

        int minValidAlignment = (length - 2 * edits) * 100;
        int maxI = iSize - 1;

        Set<TextAlignment> alignments = new TreeSet<TextAlignment>();
        for (int i = length; i <= maxI; i++)
        {
            for (int j = length - edits; j < jSize; j++)
            {
                int currentScore = m_edits[i][j];
                int currentStart = start[i][j];
                int alignmentLength = j - currentStart;
                if (currentScore >= minValidAlignment && alignmentLength >= length)
                {
                    // Try to eliminate spurious duplicate alignments
                    int previousScore = j > 0 ? m_edits[maxI][j - 1] : Integer.MIN_VALUE;
                    int nextScore = j < jSize - 1 ? m_edits[maxI][j + 1] : Integer.MIN_VALUE;
                    if (currentScore >= previousScore && currentScore >= nextScore)
                    {
                        alignments.add(new TextAlignment(j - alignmentLength, j, 0, Math
                            .min(alignmentLength, text.length())));
                    }
                }
            }
        }
        return alignments;
    }

    @Override
    public String toString()
    {
        int maxI = m_currentPattern.length() + 1;
        int maxJ = m_text.length() + 1;

        StringBuffer sb = new StringBuffer(1024);
        sb.append("       ");
        for (int j = 0; j < maxJ; j++)
        {
            sb.append(j > 0 ? m_text.charAt(j - 1) : " ");
            sb.append("      ");
        }
        sb.append('\n');
        for (int i = 0; i < maxI; i++)
        {
            sb.append(i > 0 ? m_currentPattern.charAt(i - 1) : " ");
            sb.append(" | ");
            for (int j = 0; j < maxJ; j++)
            {
                sb.append(String.format(" %4d |", m_edits[i][j]));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
