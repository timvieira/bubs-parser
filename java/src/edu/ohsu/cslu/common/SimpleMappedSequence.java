package edu.ohsu.cslu.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.math.linear.IntMatrix;
import edu.ohsu.cslu.math.linear.Matrix;
import edu.ohsu.cslu.util.Strings;


/**
 * The simplest possible implementation of the {@link MappedSequence} interface. Represents a
 * sequence as a simple int[] array, mapped using a {@link Vocabulary}
 * 
 * We're using a matrix of m tokens, each with n features, in hopes of better cache performance
 * during processing (since we'll usually be iterating over the length of the sequence and touching
 * each feature at each index)
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public final class SimpleMappedSequence implements MappedSequence, Cloneable, Serializable
{
    private final Vocabulary[] vocabularies;
    private final Matrix matrix;

    public SimpleMappedSequence(final int[] sequence, final Vocabulary vocabulary)
    {
        if (sequence.length == 0)
        {
            matrix = new IntMatrix(0, 0);
        }
        else
        {
            matrix = new IntMatrix(new int[][] {sequence}).transpose();
        }
        this.vocabularies = new AlignmentVocabulary[features()];
        Arrays.fill(vocabularies, vocabulary);

    }

    public SimpleMappedSequence(final int[][] sequence, final Vocabulary[] vocabularies)
    {
        matrix = new IntMatrix(sequence);
        this.vocabularies = vocabularies;
    }

    public SimpleMappedSequence(final int[][] sequence, final Vocabulary vocabulary)
    {
        matrix = new IntMatrix(sequence);
        this.vocabularies = new Vocabulary[features()];
        Arrays.fill(vocabularies, vocabulary);
    }

    private SimpleMappedSequence(final Matrix matrix, final Vocabulary[] vocabularies)
    {
        this.matrix = matrix;
        this.vocabularies = vocabularies;
    }

    public SimpleMappedSequence(final Matrix matrix, final Vocabulary vocabulary)
    {
        this.matrix = matrix;
        this.vocabularies = new AlignmentVocabulary[features()];
        Arrays.fill(vocabularies, vocabulary);
    }

    /**
     * Constructs a {@link SimpleMappedSequence} from a bracketed sequence of the form:
     * 
     * <pre>
     * '(feature1 feature2...featureN) (feature1 feature2...featureN)...'
     * </pre>
     * 
     * @param bracketedSequence
     * @param vocabularies
     */
    public SimpleMappedSequence(String bracketedSequence, AlignmentVocabulary[] vocabularies)
    {
        String[][] split = Strings.bracketedTags(bracketedSequence);
        matrix = new IntMatrix(split.length, split[0].length);

        for (int j = 0; j < split.length; j++)
        {
            for (int i = 0; i < vocabularies.length; i++)
            {
                matrix.set(j, i, vocabularies[i].map(split[j][i]));
            }
        }
        this.vocabularies = vocabularies;
    }

    @Override
    public final int feature(final int index, final int featureIndex)
    {
        return matrix.getInt(index, featureIndex);
    }

    @Override
    public final String stringFeature(final int index, final int featureIndex)
    {
        return vocabularies[featureIndex].map(feature(index, featureIndex));
    }

    @Override
    public final int[] features(final int index)
    {
        return matrix.getIntRow(index);
    }

    @Override
    public final String[] stringFeatures(final int index)
    {
        String[] stringFeatures = new String[features()];
        for (int i = 0; i < stringFeatures.length; i++)
        {
            stringFeatures[i] = stringFeature(index, i);
        }
        return stringFeatures;
    }

    @Override
    public final Vocabulary vocabulary(final int featureIndex)
    {
        return vocabularies[featureIndex];
    }

    @Override
    public final Vocabulary[] vocabularies()
    {
        return vocabularies;
    }

    @Override
    public final int length()
    {
        return matrix.rows();
    }

    @Override
    public final int features()
    {
        return matrix.columns();
    }

    /**
     * Inserts gaps at the specified indices
     * 
     * @param gapIndices
     */
    public final MappedSequence insertGaps(final int[] gapIndices)
    {
        if (gapIndices.length == 0)
        {
            return clone();
        }

        final int[] gapVector = new int[features()];
        Arrays.fill(gapVector, SubstitutionAlignmentModel.GAP_INDEX);

        final int gaps = gapIndices.length;
        final int newLength = length() + gaps;

        final Matrix newMatrix = new IntMatrix(newLength, features());
        int currentGap = 0;
        int oldI = 0;
        for (int i = 0; i < newLength; i++)
        {
            if (currentGap < gaps && oldI == gapIndices[currentGap])
            {
                newMatrix.setRow(i, gapVector);
                currentGap++;
            }
            else
            {
                newMatrix.setRow(i, matrix.getIntRow(oldI++));
            }
        }

        return new SimpleMappedSequence(newMatrix, vocabularies);
    }

    @Override
    public final MappedSequence removeAllGaps()
    {
        final int oldLength = length();
        int gaps = 0;
        for (int j = 0; j < oldLength; j++)
        {
            if (feature(j, 0) == SubstitutionAlignmentModel.GAP_INDEX)
            {
                gaps++;
            }
        }

        if (gaps == 0)
        {
            return clone();
        }

        Matrix newMatrix = new IntMatrix(oldLength - gaps, features());

        // Old and new column indices
        int newJ = 0;
        for (int oldJ = 0; oldJ < oldLength; oldJ++)
        {
            if (feature(oldJ, 0) != SubstitutionAlignmentModel.GAP_INDEX)
            {
                newMatrix.setRow(newJ++, matrix.getIntRow(oldJ));
            }
        }
        return new SimpleMappedSequence(newMatrix, vocabularies);
    }

    @Override
    public Sequence features(int... features)
    {
        int[][] newSequences = new int[features.length][];
        Vocabulary[] newVocabularies = new Vocabulary[features.length];
        for (int i = 0; i < features.length; i++)
        {
            newVocabularies[i] = vocabularies[features[i]];
            newSequences[i] = matrix.getIntColumn(features[i]);
        }

        // TODO: There's probably a much more efficient way to do this, but it's working.
        return new SimpleMappedSequence(new IntMatrix(newSequences).transpose(), newVocabularies);
    }

    @Override
    public String[] stringSequence(int featureIndex)
    {
        // TODO: There might be a more efficient way to do this
        String[] sequence = new String[length()];
        for (int i = 0; i < length(); i++)
        {
            sequence[i] = stringFeature(i, featureIndex);
        }
        return sequence;
    }

    @Override
    public Sequence subSequence(int beginIndex, int endIndex)
    {
        if (endIndex == beginIndex)
        {
            return new SimpleMappedSequence(new int[0][0], vocabularies);
        }

        return new SimpleMappedSequence(matrix.subMatrix(beginIndex, endIndex - 1, 0, matrix.columns() - 1),
            vocabularies);
    }

    @Override
    public Sequence[] splitIntoSentences()
    {
        throw new UnsupportedOperationException("splitIntoSentences is not implemented");
    }

    @Override
    public Iterator<String> iterator()
    {
        ArrayList<String> words = new ArrayList<String>(length());
        for (int i = 0; i < length(); i++)
        {
            words.add(stringFeature(i, 0));
        }
        return words.iterator();
    }

    @Override
    public final boolean equals(Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof SimpleMappedSequence))
        {
            return false;
        }

        SimpleMappedSequence s = (SimpleMappedSequence) o;
        if (vocabularies.length != s.vocabularies.length)
        {
            return false;
        }

        for (int i = 0; i < vocabularies.length; i++)
        {
            if (!(vocabularies[i].equals(s.vocabularies[i])))
            {
                return false;
            }
        }

        if (!matrix.equals(s.matrix))
        {
            return false;
        }
        return true;
    }

    @Override
    public final MappedSequence clone()
    {
        return new SimpleMappedSequence(matrix.clone(), vocabularies);
    }

    @Override
    public final String toString()
    {
        return toColumnString();
    }

    public final String toBracketedString()
    {
        final int length = length();
        final int features = features();

        StringBuilder sb = new StringBuilder(length * 20);

        for (int i = 0; i < length; i++)
        {
            sb.append('(');
            for (int j = 0; j < features - 1; j++)
            {
                sb.append(stringFeature(i, j));
                sb.append(' ');
            }
            sb.append(stringFeature(i, features - 1));
            sb.append(") ");
        }

        // Delete the final space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    public final String toSlashSeparatedString()
    {
        final int length = length();
        final int features = features();

        StringBuilder sb = new StringBuilder(length * 20);

        for (int i = 0; i < length; i++)
        {
            for (int j = 0; j < features - 1; j++)
            {
                sb.append(stringFeature(i, j));
                sb.append('/');
            }
            sb.append(stringFeature(i, features - 1));
            sb.append(' ');
        }

        // Delete the final space
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }

    public final String toColumnString()
    {
        final int length = length();
        final int features = features();

        // Find the maximum token length in each column
        String[] formats = new String[length];
        for (int j = 0; j < length; j++)
        {
            int columnLength = stringFeature(j, 0).length();
            for (int i = 1; i < features; i++)
            {
                int featureLength = stringFeature(j, i).length();
                if (featureLength > columnLength)
                {
                    columnLength = featureLength;
                }
            }
            formats[j] = " %" + columnLength + "s |";
        }

        // Format the string
        return toColumnString(formats);
    }

    @Override
    public String toColumnString(String[] formats)
    {
        final int length = length();
        final int features = features();

        StringBuilder sb = new StringBuilder(length * 20);

        for (int i = 0; i < features; i++)
        {
            for (int j = 0; j < length; j++)
            {
                sb.append(String.format(formats[j], stringFeature(j, i)));
            }
            sb.append('\n');
        }

        // Delete the final newline
        sb.deleteCharAt(sb.length() - 1);

        return sb.toString();
    }
}
