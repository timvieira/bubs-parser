package edu.ohsu.cslu.alignment;

import java.io.Serializable;
import java.util.Arrays;

import edu.ohsu.cslu.math.linear.FloatMatrix;
import edu.ohsu.cslu.math.linear.Matrix;

/**
 * Implements the {@link SubstitutionAlignmentModel} interfaces using a {@link Matrix} of
 * probabilities.
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
public class MatrixSubstitutionAlignmentModel implements SubstitutionAlignmentModel, Serializable
{
    private final Matrix[] matrices;
    private final AlignmentVocabulary[] vocabularies;
    protected final int[] gapVector;

    public MatrixSubstitutionAlignmentModel(Matrix[] matrices, AlignmentVocabulary[] vocabularies)
    {
        this.matrices = matrices;
        this.vocabularies = vocabularies;
        gapVector = new int[matrices.length];
        Arrays.fill(gapVector, GAP_INDEX);
    }

    public MatrixSubstitutionAlignmentModel(Matrix matrix, AlignmentVocabulary vocabulary)
    {
        this(new Matrix[] {matrix}, new AlignmentVocabulary[] {vocabulary});
    }

    public MatrixSubstitutionAlignmentModel(float[] substitutionCosts, float[] gapCosts,
        AlignmentVocabulary[] vocabularies)
    {
        this.vocabularies = vocabularies;
        this.matrices = new Matrix[substitutionCosts.length];
        this.gapVector = new int[matrices.length];
        Arrays.fill(gapVector, GAP_INDEX);

        initializeIdentityMatrices(substitutionCosts, gapCosts, vocabularies);
    }

    public MatrixSubstitutionAlignmentModel(float substitutionCost, float gapCost, AlignmentVocabulary[] vocabularies)
    {
        this.vocabularies = vocabularies;
        this.matrices = new Matrix[vocabularies.length];
        this.gapVector = new int[vocabularies.length];
        Arrays.fill(gapVector, GAP_INDEX);

        float[] substitutionCosts = new float[vocabularies.length];
        float[] gapCosts = new float[vocabularies.length];
        for (int i = 0; i < substitutionCosts.length; i++)
        {
            substitutionCosts[i] = substitutionCost;
            gapCosts[i] = gapCost;
        }
        initializeIdentityMatrices(substitutionCosts, gapCosts, vocabularies);
    }

    private void initializeIdentityMatrices(float[] substitutionCosts, float[] gapCosts, AlignmentVocabulary[] vocab)
    {
        for (int m = 0; m < substitutionCosts.length; m++)
        {
            final int size = vocab[m].size();
            matrices[m] = new FloatMatrix(size, size, false);

            for (int i = 0; i < size; i++)
            {
                for (int j = 0; j < size; j++)
                {
                    matrices[m].set(i, j, (i == j) ? 0 : substitutionCosts[m]);
                }
            }

            matrices[m].setRow(0, gapCosts[m]);
            matrices[m].setColumn(0, gapCosts[m]);
            matrices[m].set(0, 0, 0);
        }
    }

    @Override
    public final AlignmentVocabulary[] vocabularies()
    {
        return vocabularies;
    }

    @Override
    public float cost(final int alignedFeature, final int unalignedFeature)
    {
        return matrices[0].getFloat(alignedFeature, unalignedFeature);
    }

    @Override
    public float gapInsertionCost(int feature, int sequenceLength)
    {
        // cost does not depend on sequence length

        // TODO: This assumes that the cost of aligning a feature with a gap is reflexive (that is,
        // that the cost is the same regardless of whether the gap is in the existing alignment or
        // in the new sequence)
        return cost(GAP_INDEX, feature);
    }

    @Override
    public float cost(final int[] alignedVector, final int[] unalignedVector)
    {
        float cost = 0f;
        for (int i = 0; i < matrices.length; i++)
        {
            cost += matrices[i].getFloat(alignedVector[i], unalignedVector[i]);
        }
        return cost;
    }

    @Override
    public float gapInsertionCost(int[] featureVector, int sequenceLength)
    {
        // cost does not depend on sequence length

        // TODO: This assumes that the cost of aligning a feature with a gap is reflexive (that is,
        // the same regardless of whether the gap is in the existing alignment or in the new
        // sequence)
        return cost(gapVector, featureVector);
    }

    @Override
    public final int features()
    {
        return matrices.length;
    }

    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(8096);
        for (int m = 0; m < matrices.length; m++)
        {
            Matrix matrix = matrices[m];
            if (matrix.rows() <= 100)
            {
                AlignmentVocabulary vocabulary = vocabularies[m];

                sb.append("       |");
                for (int j = 0; j < matrix.columns(); j++)
                {
                    sb.append(String.format(" %5s |", vocabulary.map(j)));
                }
                sb.append('\n');

                for (int i = 0; i < matrix.rows(); i++)
                {
                    sb.append(String.format(" %5s |", vocabulary.map(i)));
                    for (int j = 0; j < matrix.columns(); j++)
                    {
                        sb.append(String.format(" %5.2f |", matrix.getFloat(i, j)));
                    }
                    sb.append('\n');
                }
            }
            else
            {
                sb.append(String.format("Matrix of %d rows", matrix.rows()));
            }
        }
        return sb.toString();
    }
}
