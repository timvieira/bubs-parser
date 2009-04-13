package edu.ohsu.cslu.alignment.bio;

import edu.ohsu.cslu.common.LogLinearMappedSequence;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;

public class LogLinearDnaVocabulary extends DnaVocabulary
{
    @Override
    public MappedSequence mapSequence(String sequence)
    {
        BitVector[] mappedSequence = new BitVector[sequence.length()];
        for (int i = 0; i < mappedSequence.length; i++)
        {
            mappedSequence[i] = new SparseBitVector(new int[] {mapCharacter(sequence.charAt(i))});
        }
        return new LogLinearMappedSequence(mappedSequence, this);
    }

    @Override
    public String mapSequence(MappedSequence sequence)
    {
        char[] mappedSequence = new char[sequence.length()];
        for (int j = 0; j < mappedSequence.length; j++)
        {
            mappedSequence[j] = mapIndex(((BitVector) sequence.elementAt(j)).values()[0]);
        }
        return new String(mappedSequence);
    }

}
