package edu.ohsu.cslu.common;

import java.io.Serializable;
import java.util.Arrays;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.util.Strings;

/**
 * The simplest possible implementation of the {@link Sequence} interface. Represents a sequence as
 * a simple int[] array, mapped using a {@link Vocabulary}
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
public final class MultipleVocabularyMappedSequence implements MappedSequence, Cloneable, Serializable
{
    private final Vocabulary[] vocabularies;
    private final DenseMatrix matrix;

    public MultipleVocabularyMappedSequence(final int[] sequence, final Vocabulary vocabulary)
    {
        if (sequence.length == 0)
        {
            matrix = new IntMatrix(0, 0);
        }
        else
        {
            matrix = new IntMatrix(new int[][] {sequence}).transpose();
        }
        this.vocabularies = new AlignmentVocabulary[featureCount()];
        Arrays.fill(vocabularies, vocabulary);

    }

    public MultipleVocabularyMappedSequence(final int[][] sequence, final Vocabulary[] vocabularies)
    {
        matrix = new IntMatrix(sequence);
        this.vocabularies = vocabularies;
    }

    public MultipleVocabularyMappedSequence(final Vector[] sequence, final Vocabulary[] vocabularies)
    {
        matrix = new IntMatrix(sequence.length, sequence[0].length());
        for (int i = 0; i < sequence.length; i++)
        {
            for (int j = 0; j < sequence[i].length(); j++)
            {
                matrix.set(i, j, sequence[i].getInt(j));
            }
        }

        this.vocabularies = vocabularies;
    }

    public MultipleVocabularyMappedSequence(final int[][] sequence, final Vocabulary vocabulary)
    {
        matrix = new IntMatrix(sequence);
        this.vocabularies = new Vocabulary[featureCount()];
        Arrays.fill(vocabularies, vocabulary);
    }

    private MultipleVocabularyMappedSequence(final DenseMatrix matrix, final Vocabulary[] vocabularies)
    {
        this.matrix = matrix;
        this.vocabularies = vocabularies;
    }

    public MultipleVocabularyMappedSequence(final DenseMatrix matrix, final Vocabulary vocabulary)
    {
        this.matrix = matrix;
        this.vocabularies = new AlignmentVocabulary[featureCount()];
        Arrays.fill(vocabularies, vocabulary);
    }

    /**
     * Constructs a {@link MultipleVocabularyMappedSequence} from a bracketed sequence of the form:
     * 
     * <pre>
     * '(feature1 feature2...featureN) (feature1 feature2...featureN)...'
     * </pre>
     * 
     * @param bracketedSequence
     * @param vocabularies
     */
    public MultipleVocabularyMappedSequence(final String bracketedSequence, final AlignmentVocabulary[] vocabularies)
    {
        final String[][] split = Strings.bracketedTags(bracketedSequence);
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

    /**
     * @param index position in the {@link Sequence}
     * @param featureIndex feature / {@link Vocabulary}
     * @return integer representation of the token at the specified index, mapped using the
     *         {@link Vocabulary} of the specified <code>featureIndex</code>
     */
    public final int feature(final int index, final int featureIndex)
    {
        return matrix.getInt(index, featureIndex);
    }

    @Override
    public final String stringFeature(final int index, final int featureIndex)
    {
        return vocabularies[featureIndex].map(feature(index, featureIndex));
    }

    /**
     * @param index
     * @return integer representation of the token at the specified index. Each integer represents a
     *         feature mapped using the {@link Vocabulary} of the same index.
     */
    public final Vector elementAt(final int index)
    {
        return new IntVector(matrix.getIntRow(index));
    }

    @Override
    public final String[] stringFeatures(final int index)
    {
        final String[] stringFeatures = new String[featureCount()];
        for (int i = 0; i < stringFeatures.length; i++)
        {
            stringFeatures[i] = stringFeature(index, i);
        }
        return stringFeatures;
    }

    /**
     * @return the vocabulary used to map the specified feature
     */
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
    public final int featureCount()
    {
        return matrix.columns();
    }

    /**
     * Inserts gaps at the specified indices
     * 
     * @param gapIndices
     */
    public final MultipleVocabularyMappedSequence insertGaps(final int[] gapIndices)
    {
        if (gapIndices.length == 0)
        {
            return clone();
        }

        final int[] gapVector = new int[featureCount()];
        Arrays.fill(gapVector, SubstitutionAlignmentModel.GAP_INDEX);

        final int gaps = gapIndices.length;
        final int newLength = length() + gaps;

        final DenseMatrix newMatrix = new IntMatrix(newLength, featureCount());
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

        return new MultipleVocabularyMappedSequence(newMatrix, vocabularies);
    }

    @Override
    public final MultipleVocabularyMappedSequence removeAllGaps()
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

        final DenseMatrix newMatrix = new IntMatrix(oldLength - gaps, featureCount());

        // Old and new column indices
        int newJ = 0;
        for (int oldJ = 0; oldJ < oldLength; oldJ++)
        {
            if (feature(oldJ, 0) != SubstitutionAlignmentModel.GAP_INDEX)
            {
                newMatrix.setRow(newJ++, matrix.getIntRow(oldJ));
            }
        }
        return new MultipleVocabularyMappedSequence(newMatrix, vocabularies);
    }

    @Override
    public MappedSequence retainFeatures(final int... features)
    {
        final int[][] newSequences = new int[features.length][];
        final Vocabulary[] newVocabularies = new Vocabulary[features.length];
        for (int i = 0; i < features.length; i++)
        {
            newVocabularies[i] = vocabularies[features[i]];
            newSequences[i] = matrix.getIntColumn(features[i]);
        }

        // TODO: There's probably a much more efficient way to do this, but it's working.
        return new MultipleVocabularyMappedSequence(new IntMatrix(newSequences).transpose(), newVocabularies);
    }

    @Override
    public Sequence subSequence(final int beginIndex, final int endIndex)
    {
        if (endIndex == beginIndex)
        {
            return new MultipleVocabularyMappedSequence(new int[0][0], vocabularies);
        }

        return new MultipleVocabularyMappedSequence(
            matrix.subMatrix(beginIndex, endIndex - 1, 0, matrix.columns() - 1), vocabularies);
    }

    @Override
    public MappedSequence[] splitIntoSentences()
    {
        throw new UnsupportedOperationException("splitIntoSentences is not implemented");
    }

    @Override
    public final boolean equals(final Object o)
    {
        if (o == this)
        {
            return true;
        }

        if (!(o instanceof MultipleVocabularyMappedSequence))
        {
            return false;
        }

        final MultipleVocabularyMappedSequence s = (MultipleVocabularyMappedSequence) o;
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
    public final MultipleVocabularyMappedSequence clone()
    {
        return new MultipleVocabularyMappedSequence(matrix.clone(), vocabularies);
    }

    @Override
    public final String toString()
    {
        return toColumnString();
    }

    public final String toBracketedString()
    {
        final int length = length();
        final int features = featureCount();

        final StringBuilder sb = new StringBuilder(length * 20);

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
        final int features = featureCount();

        final StringBuilder sb = new StringBuilder(length * 20);

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
        final int features = featureCount();

        // Find the maximum token length in each column
        final String[] formats = new String[length];
        for (int j = 0; j < length; j++)
        {
            int columnLength = stringFeature(j, 0).length();
            for (int i = 1; i < features; i++)
            {
                final int featureLength = stringFeature(j, i).length();
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

    /**
     * Formats this sequence as a String, using the supplied formatting Strings
     * 
     * @param formats
     * @return String visualization of the sequence
     */
    public String toColumnString(final String[] formats)
    {
        final int length = length();
        final int features = featureCount();

        final StringBuilder sb = new StringBuilder(length * 20);

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

    @Override
    public int maxLabelLength(final int index)
    {
        int maxLength = 0;
        for (int i = 0; i < featureCount(); i++)
        {
            final int length = stringFeature(index, i).length();
            if (length > maxLength)
            {
                maxLength = length;
            }
        }
        return maxLength;
    }
}
