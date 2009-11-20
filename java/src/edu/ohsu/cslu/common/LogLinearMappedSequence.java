package edu.ohsu.cslu.common;

import it.unimi.dsi.fastutil.ints.IntRBTreeSet;

import java.util.Arrays;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.LogLinearVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.util.Strings;

/**
 * Represents an ordered sequence of elements, mapped using a {@link Vocabulary}. Each element is
 * represented by a {@link BitVector} denoting the features of that element.
 * 
 * Elements are not required (and generally not expected) to have the same number of features.
 * 
 * For example, assume the following vocabulary:<br/>
 * 
 * vocabulary size=9<br/>
 * 0 : -<br/>
 * 1 : DT<br/>
 * 2 : NN<br/>
 * 3 : VB<br/>
 * 4 : The<br/>
 * 5 : dog<br/>
 * 6 : ran<br/>
 * 7 : start<br/>
 * 8 : head_verb<br/>
 * <br/>
 * 
 * We can represent the sequence "(The DT start) (dog NN) (ran VB head_verb)" using the following 3
 * {@link BitVector}s: (1,4,7) (2,5) (3,6,8)
 * 
 * TODO: Implement Iterator<BitVector> or Iterator<String>
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2008
 * 
 *        $Id$
 */
public class LogLinearMappedSequence implements MappedSequence
{
    // TODO: Represent as a {@link LinkedList} so we can insert and remove efficiently?
    private final BitVector[] elements;
    private final Vocabulary vocabulary;

    /**
     * Constructs a {@link LogLinearMappedSequence} from a bracketed sequence of the form:
     * 
     * <pre>
     * '(feature1 feature2...featureN) (feature1 feature2...featureN)...'
     * </pre>
     * 
     * @param bracketedSequence
     * @param vocabulary {@link LogLinearVocabulary} with which to map the tokens to
     *            {@link BitVector}s.
     */
    public LogLinearMappedSequence(final String bracketedSequence, final AlignmentVocabulary vocabulary)
    {
        final String[][] split = Strings.bracketedTags(bracketedSequence);
        elements = new BitVector[split.length];

        for (int j = 0; j < split.length; j++)
        {
            final int[] features = new int[split[j].length];
            for (int i = 0; i < split[j].length; i++)
            {
                features[i] = vocabulary.map(split[j][i]);
            }
            elements[j] = new SparseBitVector(features);
        }

        // for (int j = 0; j < split.length; j++)
        // {
        // final IntSet featureSet = new IntAVLTreeSet();
        // for (int i = 0; i < split[j].length; i++)
        // {
        // final String token = split[j][i];
        // if (vocabulary.isRareToken(token))
        // {
        // featureSet.add(SimpleVocabulary.UNKNOWN_SYMBOL);
        // }
        // featureSet.add(vocabulary.map(token));
        // }
        // elements[j] = new SparseBitVector(featureSet.toIntArray());
        // }
        this.vocabulary = vocabulary;
    }

    public LogLinearMappedSequence(final BitVector[] elements, final Vocabulary vocabulary)
    {
        this.elements = elements;
        this.vocabulary = vocabulary;
    }

    public LogLinearMappedSequence(final int[] features, final Vocabulary vocabulary)
    {
        this.elements = new BitVector[features.length];
        for (int j = 0; j < features.length; j++)
        {
            elements[j] = new SparseBitVector(new int[] {features[j]});
        }
        this.vocabulary = vocabulary;
    }

    public LogLinearMappedSequence(final int[][] features, final Vocabulary vocabulary)
    {
        this.elements = new BitVector[features.length];
        for (int j = 0; j < features.length; j++)
        {
            elements[j] = new SparseBitVector(features[j]);
        }
        this.vocabulary = vocabulary;
    }

    /**
     * Convenience method, since {@link LogLinearMappedSequence} is always mapped by a single
     * vocabulary.
     * 
     * @return mapping vocabulary
     */
    public Vocabulary vocabulary()
    {
        return vocabulary;
    }

    @Override
    public int featureCount()
    {
        return vocabulary.size();
    }

    @Override
    public int length()
    {
        return elements.length;
    }

    @Override
    public BitVector elementAt(final int index)
    {
        return elements[index];
    }

    @Override
    public String stringFeature(final int index, final int featureIndex)
    {
        // Return null if the specified element does not contain the specified feature
        if (!elements[index].getBoolean(featureIndex))
        {
            return null;
        }

        return vocabulary.map(featureIndex);
    }

    @Override
    public String[] stringFeatures(final int index)
    {
        final int[] values = elements[index].values();
        final String[] stringFeatures = new String[values.length];
        for (int i = 0; i < stringFeatures.length; i++)
        {
            stringFeatures[i] = vocabulary.map(values[i]);
        }
        return stringFeatures;
    }

    @Override
    public MappedSequence insertGaps(final int[] gapIndices)
    {
        if (gapIndices.length == 0)
        {
            return clone();
        }

        final BitVector gapVector = new SparseBitVector(new int[] {SubstitutionAlignmentModel.GAP_INDEX});

        final int gaps = gapIndices.length;
        final int newLength = length() + gaps;

        final BitVector[] newElements = new BitVector[newLength];
        edu.ohsu.cslu.util.Arrays.insertGaps(elements, gapIndices, newElements, gapVector);

        // Note that this does not clone all contained BitVectors. Efficient, but possibly
        // problematic in a functional-programming sense.
        return new LogLinearMappedSequence(newElements, vocabulary);
    }

    @Override
    public MappedSequence removeAllGaps()
    {
        final int oldLength = length();
        int gaps = 0;
        for (int j = 0; j < oldLength; j++)
        {
            if (elements[j].getBoolean(SubstitutionAlignmentModel.GAP_INDEX))
            {
                gaps++;
            }
        }

        if (gaps == 0)
        {
            return clone();
        }

        final BitVector[] newElements = new BitVector[oldLength - gaps];

        // Old and new column indices
        int newJ = 0;
        for (int oldJ = 0; oldJ < oldLength; oldJ++)
        {
            if (!elements[oldJ].getBoolean(SubstitutionAlignmentModel.GAP_INDEX))
            {
                newElements[newJ++] = elements[oldJ];
            }
        }

        // Note that this does not clone all contained BitVectors. Efficient, but possibly
        // problematic in a functional-programming sense.
        return new LogLinearMappedSequence(newElements, vocabulary);
    }

    @Override
    public MappedSequence retainFeatures(final int... features)
    {
        final BitVector[] newElements = new BitVector[elements.length];
        for (int j = 0; j < elements.length; j++)
        {
            final IntRBTreeSet newFeatures = new IntRBTreeSet();
            for (int i = 0; i < features.length; i++)
            {
                if (elements[j].getBoolean(features[i]))
                {
                    newFeatures.add(features[i]);
                }
            }
            newElements[j] = new SparseBitVector(newFeatures.toIntArray());
        }
        return new LogLinearMappedSequence(newElements, vocabulary);
    }

    @Override
    public MappedSequence[] splitIntoSentences()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Sequence subSequence(final int beginIndex, final int endIndex)
    {
        // Note that this does not clone all contained BitVectors. Efficient, but possibly
        // problematic in a functional-programming sense.
        return new LogLinearMappedSequence(Arrays.copyOfRange(elements, beginIndex, endIndex), vocabulary);
    }

    @Override
    public String toBracketedString()
    {
        final StringBuilder sb = new StringBuilder(256);
        for (int j = 0; j < elements.length; j++)
        {
            final int[] features = elements[j].values();
            sb.append("(");
            for (int i = 0; i < features.length; i++)
            {
                sb.append(vocabulary.map(features[i]));
                sb.append(' ');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(") ");
        }

        // Remove the final space
        if (sb.length() > 1)
        {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    @Override
    public String toSlashSeparatedString()
    {
        final StringBuilder sb = new StringBuilder(256);
        for (int j = 0; j < elements.length; j++)
        {
            final int[] features = elements[j].values();
            for (int i = 0; i < features.length; i++)
            {
                sb.append(vocabulary.map(features[i]));
                sb.append('/');
            }
            sb.deleteCharAt(sb.length() - 1);
            sb.append(' ');
        }

        // Remove the final space
        if (sb.length() > 1)
        {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    @Override
    public Vocabulary[] vocabularies()
    {
        return new Vocabulary[] {vocabulary};
    }

    @Override
    public final LogLinearMappedSequence clone()
    {
        final BitVector[] newElements = new BitVector[elements.length];
        for (int j = 0; j < newElements.length; j++)
        {
            newElements[j] = (BitVector) elements[j].clone();
        }
        return new LogLinearMappedSequence(newElements, vocabulary);
    }

    @Override
    public boolean equals(final Object o)
    {
        if (!(o instanceof LogLinearMappedSequence))
        {
            return false;
        }

        return toColumnString().equals(((LogLinearMappedSequence) o).toColumnString());
    }

    @Override
    public String toString()
    {
        return toColumnString();
    }

    public final String toColumnString()
    {
        final int length = length();

        // Find the maximum token length in each column
        final String[] formats = new String[length];

        for (int j = 0; j < length; j++)
        {
            int columnLength = 0;
            final int[] values = elements[j].values();
            for (int i = 0; i < values.length; i++)
            {
                final int featureLength = vocabulary.map(values[i]).length();
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
    @Override
    public String toColumnString(final String[] formats)
    {
        final int length = length();

        // Find the maximum number of populated features (the number of rows we'll need to format)
        int maxFeatures = 0;
        for (int j = 0; j < length; j++)
        {
            final int features = elements[j].values().length;
            if (features > maxFeatures)
            {
                maxFeatures = features;
            }
        }

        final StringBuilder sb = new StringBuilder(length * 20);

        // Rows
        for (int i = 0; i < maxFeatures; i++)
        {
            // Columns
            for (int j = 0; j < length; j++)
            {
                final int[] elementValues = elements[j].values();
                final String stringFeature = (i < elementValues.length) ? vocabulary.map(elementValues[i]) : "";
                sb.append(String.format(formats[j], stringFeature));
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
        final int[] features = elements[index].values();

        int maxLength = 0;
        for (int i = 0; i < features.length; i++)
        {
            final int length = vocabulary.map(features[i]).length();
            if (length > maxLength)
            {
                maxLength = length;
            }
        }
        return maxLength;
    }
}
