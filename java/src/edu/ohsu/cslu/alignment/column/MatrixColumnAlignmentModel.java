package edu.ohsu.cslu.alignment.column;

import edu.ohsu.cslu.alignment.AlignmentModel;
import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.alignment.multiple.MultipleSequenceAlignment;
import edu.ohsu.cslu.common.MappedSequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.util.Strings;

/**
 * Represents a Position-Specific-Score-Matrix (PSSM) alignment model, using a set of matrices (
 * {@link Matrix}) to store probabilities.
 * 
 * @author Aaron Dunlop
 * @since Oct 30, 2008
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class MatrixColumnAlignmentModel implements ColumnAlignmentModel
{
    protected final int MAX_TOSTRING_LENGTH = 256;

    protected Vocabulary[] vocabularies;
    protected Matrix[] matrices;
    private int[] gapSymbols;
    private float columnInsertionCost = Float.POSITIVE_INFINITY;
    
    private SubstitutionAlignmentModel substitutionModel;
    
    // totalCounts[] holds the total number of counts and pseudo-counts that exist for a given feature
    int[] totalCounts;

    public MatrixColumnAlignmentModel(Matrix[] matrices, Vocabulary[] vocabularies)
    {
        this.matrices = matrices;
        this.vocabularies = vocabularies;
        this.gapSymbols = new int[vocabularies.length];

        for (int i = 0; i < vocabularies.length; i++)
        {
            gapSymbols[i] = ((AlignmentVocabulary) vocabularies[i]).gapSymbol();
        }
    }

    protected MatrixColumnAlignmentModel(AlignmentVocabulary vocabulary, float columnInsertionCost)
    {
        this.vocabularies = new AlignmentVocabulary[] {vocabulary};
        this.matrices = new Matrix[1];
        this.gapSymbols = new int[] {vocabulary.gapSymbol()};
        this.columnInsertionCost = columnInsertionCost;
    }

    protected MatrixColumnAlignmentModel(AlignmentVocabulary vocabulary)
    {
        this(vocabulary, Float.POSITIVE_INFINITY);
    }
    
    public MatrixColumnAlignmentModel(
        MultipleSequenceAlignment multipleSequenceAlignment,
        int[] pseudoCountsPerToken, 
        int[] featureIndices,
        int emphasizedSequence, 
        int additionalCounts, 
        boolean[] binaryFeatures, 
        float binaryFeatureGapCost)
    {
        final int featureCount = featureIndices.length;
        final int slots = multipleSequenceAlignment.length();
        final Vocabulary[] newVocabularies = new Vocabulary[featureIndices.length];
        for (int f = 0; f < featureCount; f++)
        {
            Vocabulary[] vocabularies = multipleSequenceAlignment.getVocabularies();
            newVocabularies[f] = vocabularies[featureIndices[f]];
        }

        // Create counts[] and totalCounts[] which index on the feature vector.
        totalCounts = new int[featureCount];
        final IntMatrix[] counts = new IntMatrix[featureCount];
        for (int indexIntoFeatureIndices = 0; indexIntoFeatureIndices < featureCount; indexIntoFeatureIndices++)
        {
            // For a particular feature, counts[i] holds a Matrix with as many slots as multipleSequenceAlignment has columns
            // and as many rows as there are vocabulary items for this feature. 
            // Counts initially holds the base number of pseudo counts for smoothing.
            counts[indexIntoFeatureIndices] = 
                Matrix.Factory.newIntMatrix(
                    newVocabularies[featureIndices[indexIntoFeatureIndices]].size(), 
                    multipleSequenceAlignment.length(),
                    pseudoCountsPerToken[indexIntoFeatureIndices]);
            
            // For a particular feature, the total number of counts for any slot is the sum of:
            //  1) The total number of pseudo-counts as given by: pseudo-counts_for_feature_i * size_of_vocabulary_of_feature_i
            //  2) The number of sequences in the AlignmentModel
            //  3) The number of additional counts by which to upweight the emphasizedSequence
            totalCounts[indexIntoFeatureIndices] = 
                (pseudoCountsPerToken[indexIntoFeatureIndices] * newVocabularies[featureIndices[indexIntoFeatureIndices]].size()) + 
                multipleSequenceAlignment.numOfSequences() + 
                additionalCounts;
        }

        // March through each sequence
        //   for each slot (i.e. position) in the sequence and for each feature
        //      up the count of the feature-value that occurred in the sequence
        for (int sequenceIndex = 0; sequenceIndex < multipleSequenceAlignment.numOfSequences(); sequenceIndex++)
        {
            MappedSequence sequence = multipleSequenceAlignment.get(sequenceIndex);
            if (sequence != null)
            {
                for (int slotIndex = 0; slotIndex < slots; slotIndex++)
                {
                    final int addend = (sequenceIndex == emphasizedSequence) ? additionalCounts + 1 : 1;
                    for (int indexIntoFeatureIndeces = 0; indexIntoFeatureIndeces < featureCount; indexIntoFeatureIndeces++)
                    {
                        counts[indexIntoFeatureIndeces].add(
                            sequence.elementAt(slotIndex).getInt(indexIntoFeatureIndeces),// Integer representation of the 
                                                                                                  // feature-value of the current 
                                                                                                  // feature at slotIndex in this  
                                                                                                  // sequence 
                            slotIndex, 
                            addend);
                    }
                }
            }
        }

        // Now that we have the counts and the total counts, find the probability of each
        // feature-value of each feature at each slot position.
        Matrix[] distributionMatrices = new Matrix[featureCount];
        for (int indexIntoFeatureIndices = 0; indexIntoFeatureIndices < featureCount; indexIntoFeatureIndices++)
        {
            final Matrix matrix = distributionMatrices[indexIntoFeatureIndices] = 
                new FloatMatrix(counts[indexIntoFeatureIndices].rows(), counts[indexIntoFeatureIndices].columns());
            for (int i = 0; i < matrix.rows(); i++)
            {
                if (binaryFeatures[indexIntoFeatureIndices] && i == AlignmentModel.GAP_INDEX)
                {
                    // Set gap cost specific to a binary feature
                    for (int j = 0; j < matrix.columns(); j++)
                    {
                        matrix.set(i, j, binaryFeatureGapCost);
                    }
                }
                else
                {
                    for (int j = 0; j < matrix.columns(); j++)
                    {
                        matrix.set(
                            i, 
                            j, 
                            calculateNegativeLogP(
                                counts[indexIntoFeatureIndices].getFloat(i, j),
                                totalCounts[indexIntoFeatureIndices]));
                    }
                }
            }
        }

        initialize(distributionMatrices, newVocabularies);
    }
    
    private float calculateNegativeLogP(float numerator, float denominator)
    {
        return (float) -Math.log(numerator / denominator);
    }
    
    private void initialize(Matrix[] matrices, Vocabulary[] vocabularies)
    {
        this.matrices = matrices;
        this.vocabularies = vocabularies;
        this.gapSymbols = new int[vocabularies.length];

        for (int i = 0; i < vocabularies.length; i++)
        {
            gapSymbols[i] = ((AlignmentVocabulary) vocabularies[i]).gapSymbol();
        }
    }

    public float cost(final Vector featureVector, final int column)
    {
        int[] featureIndices = new int[featureVector.length()];
        for (int i = 0; i < featureIndices.length; i++)
        {
            featureIndices[i] = i;
        }
        return cost(featureVector, column, featureIndices);
    }

    public float cost(final Vector featureVector, final int column, final int[] featureIndices)
    {
        final boolean gap = (featureVector.getInt(0) == SubstitutionAlignmentModel.GAP_INDEX);

        float sum = 0f;
        for (int i = 0; i < featureIndices.length; i++)
        {
            final int f = featureVector.getInt(featureIndices[i]);

            // 0 probability of a gap in one feature and not in all
            final int gapSymbol = gapSymbols[i];
            if ((gap && f != gapSymbol) || (!gap && f == gapSymbol))
            {
                return Float.POSITIVE_INFINITY;
            }
            sum += matrices[featureIndices[i]].getFloat(f, column);
        }
        return sum;
    }

    public float p(final Vector featureVector, final int column)
    {
        return (float) -Math.exp(cost(featureVector, column));
    }

    public float p(final Vector featureVector, final int column, final int[] featureIndices)
    {
        return (float) -Math.exp(cost(featureVector, column, featureIndices));
    }

    @Override
    public int featureCount()
    {
        return matrices.length;
    }

    // TODO: Change name to columnCount()
    public int columns()  
    {
        return matrices[0].columns();
    }

    public Vocabulary[] vocabularies()
    {
        return vocabularies;
    }

    public Matrix costMatrix(int feature)
    {
        return matrices[feature];
    }

    @Override
    public Vector gapVector()
    {
        final Vector gapVector = new IntVector(vocabularies.length);
        for (int i = 0; i < vocabularies.length; i++)
        {
            gapVector.set(i, ((AlignmentVocabulary) vocabularies[i]).gapSymbol());
        }
        return gapVector;
    }

    @Override
    public float columnInsertionCost(Vector featureVector)
    {
        return columnInsertionCost;
    }

    public void setColumnInsertionCost(float columnInsertionCost)
    {
        this.columnInsertionCost = columnInsertionCost;
    }

    @Override
    public String toString()
    {
        final int columns = columns();
        if (columns > MAX_TOSTRING_LENGTH)
        {
            return "Maximum length exceeded";
        }

        StringBuffer sb = new StringBuffer(1024);

        sb.append("  ");
        for (int i = 0; i < columns; i++)
        {
            sb.append(String.format("%5d ", i));
        }
        sb.append('\n');
        sb.append(Strings.fill('-', columns * 6 + 3));
        sb.append('\n');

        // TODO: Adapt to multiple vocabularies
        for (int i = 0; i < vocabularies[0].size(); i++)
        {
            if (vocabularies[0] instanceof CharVocabulary)
            {
                sb.append(((CharVocabulary) vocabularies[0]).mapIndex(i));
            }
            else
            {
                sb.append(i);
            }
            sb.append(" | ");
            for (int j = 0; j < columns; j++)
            {
                float p = cost(new IntVector(new int[] {i}), j);
                sb.append(Float.isInfinite(p) ? "  Inf " : String.format("%5.3f ", p));
            }
            sb.append('\n');
        }

        return sb.toString();
    }
    
    public void setSubstitutionAlignmentModel(SubstitutionAlignmentModel substitutionModel)
    {
        this.substitutionModel = substitutionModel;
    }
    
    /**
     * Calculates the cost of inserting a gap into this Alignment Model using the following algorithm:
     * 
     *   sum over all features:
     *    a) cost of aligning the NEW feature-value of feature[i] to a gap
     *       TIMES
     *    b) total cost of ripping a gap into all already-aligned sequence  
     *     
     *    Where b) is defined as the same as aligning a featureVector to a slot that contains zero
     *    matching feature values.
     * 
     * @param featureVector -- the values of all features for one slot of the NEW sequence to align
     * @return the cost of inserting a gap
     */
    @Override
    public float costOfInsertingAGapIntoThisAlignmentModel(Vector featureVector)
    {
        float costOfInsertingAGapIntoThisAlignementModel = 0;
        
        for (int featureIndex = 0; featureIndex < featureVector.length(); featureIndex++) 
        {
            float reflexiveCostOfInstertingAGapInThisAlignmentForThisFeature =
                substitutionModel.gapInsertionCostForOneFeature(featureIndex, featureVector.getInt(featureIndex));
            
            float laplaceSmoothingIncrement = calculateNegativeLogP( 1, totalCounts[featureIndex]);
            
            costOfInsertingAGapIntoThisAlignementModel += 
                reflexiveCostOfInstertingAGapInThisAlignmentForThisFeature *
                laplaceSmoothingIncrement;
        }
        
        return costOfInsertingAGapIntoThisAlignementModel;
    }
    
    //@Override
    public float costOfInsertingAGapIntoThisAlignmentModel_reflexive(Vector featureVector)
    {
        return substitutionModel.gapInsertionCost(featureVector, columns());
    }
}
