package edu.ohsu.cslu.alignment.bio;

import java.io.IOException;
import java.io.Writer;

import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.util.Math;

/**
 * Vocabulary for DNA sequences, consisting of A, C, G, and T. Additional characters for gaps ('-')
 * and unknowns/errors ('X') are included.
 * 
 * @author Aaron Dunlop
 * @since Jul 2, 2008
 * 
 *        $Id$
 */
public class DnaVocabulary implements CharVocabulary
{
    protected final int alphabetMin;
    protected final char alphabetMax;
    protected final int[] alphabetIndexArray;

    private final static char[] CHAR_ARRAY = new char[] {'-', 'A', 'C', 'G', 'T', 'X'};
    private final static String[] STRING_ARRAY = new String[] {"-", "A", "C", "G", "T", "X"};
    private final static int[] INT_ARRAY = new int[] {0, 1, 2, 3, 4, 5};

    public DnaVocabulary()
    {
        alphabetMin = Math.min(CHAR_ARRAY);
        alphabetMax = Math.max(CHAR_ARRAY);
        alphabetIndexArray = new int[alphabetMax - alphabetMin + 1];
        for (int i = 0; i < CHAR_ARRAY.length; i++)
        {
            alphabetIndexArray[CHAR_ARRAY[i] - alphabetMin] = i;
        }
    }

    public char[] charVocabulary()
    {
        return CHAR_ARRAY;
    }

    public String[] tokens()
    {
        return STRING_ARRAY;
    }

    public int mapCharacter(char c)
    {
        return alphabetIndexArray[c - alphabetMin];
    }

    public char mapIndex(int i)
    {
        return CHAR_ARRAY[i];
    }

    @Override
    public String map(int i)
    {
        return Character.toString(CHAR_ARRAY[i]);
    }

    @Override
    public int map(String s)
    {
        return mapCharacter(s.charAt(0));
    }

    @Override
    public int size()
    {
        return INT_ARRAY.length;
    }

    public MappedSequence mapSequence(String sequence)
    {
        int[] mappedSequence = new int[sequence.length()];
        for (int i = 0; i < mappedSequence.length; i++)
        {
            mappedSequence[i] = mapCharacter(sequence.charAt(i));
        }
        return new MultipleVocabularyMappedSequence(mappedSequence, this);
    }

    public String mapSequence(MappedSequence sequence)
    {
        char[] mappedSequence = new char[sequence.length()];
        for (int j = 0; j < mappedSequence.length; j++)
        {
            mappedSequence[j] = mapIndex(((IntVector) sequence.elementAt(j)).getInt(0));
        }
        return new String(mappedSequence);
    }

    public String mapSequence(int[] sequence)
    {
        char[] mappedSequence = new char[sequence.length];
        for (int i = 0; i < mappedSequence.length; i++)
        {
            mappedSequence[i] = mapIndex(sequence[i]);
        }
        return new String(mappedSequence);
    }

    public MappedSequence[] mapSequences(String[] sequences)
    {
        MappedSequence[] mappedSequences = new MappedSequence[sequences.length];
        for (int i = 0; i < mappedSequences.length; i++)
        {
            if (sequences[i] != null)
            {
                mappedSequences[i] = mapSequence(sequences[i]);
            }
        }
        return mappedSequences;
    }

    public String[] mapSequences(MappedSequence[] sequences)
    {
        String[] mappedSequences = new String[sequences.length];
        for (int i = 0; i < mappedSequences.length; i++)
        {
            if (sequences[i] != null)
            {
                mappedSequences[i] = mapSequence(sequences[i]);
            }
        }
        return mappedSequences;
    }

    public char charDeleteSymbol()
    {
        return '-';
    }

    public int gapSymbol()
    {
        return 0;
    }

    @Override
    public String[] map(int[] indices)
    {
        String[] result = new String[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            result[i] = map(indices[i]);
        }
        return result;
    }

    @Override
    public int[] map(String[] labels)
    {
        int[] result = new int[labels.length];
        for (int i = 0; i < labels.length; i++)
        {
            result[i] = map(labels[i]);
        }
        return result;
    }

    @Override
    public void write(Writer writer) throws IOException
    {
        writer.write("0:-\n");
        writer.write("1:A\n");
        writer.write("2:C\n");
        writer.write("3:G\n");
        writer.write("4:T\n");
        writer.write("5:X\n");
    }

    @Override
    public boolean isRareToken(String token)
    {
        return false;
    }

    @Override
    public boolean isRareToken(int index)
    {
        return false;
    }
}
