package edu.ohsu.cslu.alignment.character;

import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

public class CharSequence implements MappedSequence
{
    private final Vector[] vectors;
    private final String string;

    public CharSequence(String string)
    {
        this.string = string;
        vectors = new Vector[string.length()];
        for (int i = 0; i < string.length(); i++)
        {
            vectors[i] = new SparseBitVector(new int[] {string.charAt(i)}, false);
        }
    }

    public CharSequence(Vector[] vectors)
    {
        if (!(vectors[0] instanceof SparseBitVector))
        {
            throw new IllegalArgumentException("CharSequence only supports SparseBitVector");
        }

        this.vectors = vectors;
        final CharVocabulary vocabulary = new CharVocabulary();

        StringBuilder sb = new StringBuilder(vectors.length);

        for (int i = 0; i < vectors.length; i++)
        {
            sb.append(vocabulary.map(((SparseBitVector) vectors[i]).values()[0]));
        }
        string = sb.toString();
    }

    @Override
    public Vector elementAt(int index)
    {
        return vectors[index];
    }

    @Override
    public int featureCount()
    {
        return 1;
    }

    @Override
    public MappedSequence insertGaps(int[] gapIndices)
    {
        StringBuilder sb = new StringBuilder(string.length() + gapIndices.length);

        final int gaps = gapIndices.length;
        final int newLength = length() + gaps;

        int currentGap = 0;
        int oldI = 0;
        for (int i = 0; i < newLength; i++)
        {
            if (currentGap < gaps && oldI == gapIndices[currentGap])
            {
                sb.append(0);
                currentGap++;
            }
            else
            {
                sb.append(string.charAt(oldI++));
            }
        }

        return new CharSequence(sb.toString());
    }

    @Override
    public int length()
    {
        return vectors.length;
    }

    @Override
    public MappedSequence removeAllGaps()
    {
        StringBuilder sb = new StringBuilder(string.length());
        for (int i = 0; i < string.length(); i++)
        {
            char c = string.charAt(i);
            if (c != 0)
            {
                sb.append(c);
            }
        }
        return new CharSequence(sb.toString());
    }

    @Override
    public MappedSequence retainFeatures(int... features)
    {
        throw new UnsupportedOperationException("Not supported by CharSequence");
    }

    @Override
    public MappedSequence[] splitIntoSentences()
    {
        throw new UnsupportedOperationException("Not supported by CharSequence");
    }

    @Override
    public String stringFeature(int index, int featureIndex)
    {
        if (featureIndex != 0)
        {
            throw new IllegalArgumentException("Indices > 0 not supported by CharSequence");
        }
        return Character.toString(string.charAt(index));
    }

    @Override
    public String[] stringFeatures(int index)
    {
        return new String[] {Character.toString(string.charAt(index))};
    }

    @Override
    public Sequence subSequence(int beginIndex, int endIndex)
    {
        throw new UnsupportedOperationException("Not supported by CharSequence");
    }

    @Override
    public String toBracketedString()
    {
        StringBuilder sb = new StringBuilder(string.length() * 4);
        for (int i = 0; i < string.length(); i++)
        {
            sb.append('(');
            sb.append(string.charAt(i));
            sb.append(')');
            if (i < string.length() - 1)
            {
                sb.append(' ');
            }
        }
        return sb.toString();
    }

    @Override
    public String toSlashSeparatedString()
    {
        return string;
    }

    @Override
    public String toString()
    {
        return string;
    }

    @Override
    public int maxLabelLength(int index)
    {
        return 1;
    }

    @Override
    public String toColumnString(String[] formats)
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vocabulary[] vocabularies()
    {
        return new Vocabulary[] {new CharVocabulary()};
    }

}
