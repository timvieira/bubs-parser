package edu.ohsu.cslu.alignment.pairwise;


import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Vocabulary;

/**
 * Base class for dynamic pairwise aligners
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
public abstract class BaseDynamicAligner implements PairwiseAligner
{
    protected float m_costs[][];
    protected MappedSequence m_aligned;
    protected MappedSequence m_unaligned;
    protected AlignmentModel m_model;

    @Override
    public String toString()
    {
        int maxI = m_unaligned.length() + 1;
        int maxJ = m_aligned.length() + 1;

        Vocabulary vocabulary = m_model.vocabularies()[0];
        // TODO: Take length of mapped tokens into account in formatting

        StringBuffer sb = new StringBuffer(1024);
        sb.append("       ");
        for (int j = 0; j < maxJ; j++)
        {
            sb.append(String.format("%6s |", j > 0 ? vocabulary.map(m_aligned.feature(j - 1, 0)) : ""));
        }
        sb.append('\n');
        for (int i = 0; i < maxI; i++)
        {
            sb.append(String.format("%5s |", i > 0 ? vocabulary.map(m_unaligned.feature(i - 1, 0)) : ""));
            for (int j = 0; j < maxJ; j++)
            {
                float value = m_costs[i][j];
                sb.append(String.format(value > 10000 ? "  Max |" : " %5.2f |", value));
            }
            sb.append('\n');
        }
        return sb.toString();
    }
}
