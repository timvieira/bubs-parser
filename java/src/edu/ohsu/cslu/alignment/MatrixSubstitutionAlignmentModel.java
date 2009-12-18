package edu.ohsu.cslu.alignment;

import java.io.Serializable;

import edu.ohsu.cslu.common.MultipleVocabularyMappedSequence;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;
import edu.ohsu.cslu.datastructs.matrices.FloatMatrix;
import edu.ohsu.cslu.datastructs.matrices.Matrix;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Implements the {@link SubstitutionAlignmentModel} interfaces using a {@link Matrix} of probabilities.
 * 
 * @author Aaron Dunlop
 * @since Oct 8, 2008
 * 
 *        $Id$
 */
public class MatrixSubstitutionAlignmentModel implements SubstitutionAlignmentModel, Serializable {

    private final DenseMatrix[] substitutionMatrices;
    private final AlignmentVocabulary[] vocabularies;
    protected final IntVector gapVector;

    public MatrixSubstitutionAlignmentModel(final DenseMatrix[] matrices,
            final AlignmentVocabulary[] vocabularies) {
        this.substitutionMatrices = matrices;
        this.vocabularies = vocabularies;
        this.gapVector = new IntVector(matrices.length, 0);
    }

    public MatrixSubstitutionAlignmentModel(final DenseMatrix matrix, final AlignmentVocabulary vocabulary) {
        this(new DenseMatrix[] { matrix }, new AlignmentVocabulary[] { vocabulary });
    }

    public MatrixSubstitutionAlignmentModel(final float[] substitutionCosts, final float[] gapCosts,
            final AlignmentVocabulary[] vocabularies) {
        this.vocabularies = vocabularies;
        this.substitutionMatrices = new DenseMatrix[substitutionCosts.length];
        this.gapVector = new IntVector(substitutionMatrices.length, 0);

        initializeIdentityMatrices(substitutionCosts, gapCosts, vocabularies);
    }

    public MatrixSubstitutionAlignmentModel(final float substitutionCost, final float gapCost,
            final AlignmentVocabulary[] vocabularies) {
        this.vocabularies = vocabularies;
        this.substitutionMatrices = new DenseMatrix[vocabularies.length];
        this.gapVector = new IntVector(substitutionMatrices.length, 0);

        final float[] substitutionCosts = new float[vocabularies.length];
        final float[] gapCosts = new float[vocabularies.length];
        for (int i = 0; i < substitutionCosts.length; i++) {
            substitutionCosts[i] = substitutionCost;
            gapCosts[i] = gapCost;
        }
        initializeIdentityMatrices(substitutionCosts, gapCosts, vocabularies);
    }

    private void initializeIdentityMatrices(final float[] substitutionCosts, final float[] gapCosts,
            final AlignmentVocabulary[] vocab) {
        for (int m = 0; m < substitutionCosts.length; m++) {
            final int size = vocab[m].size();
            substitutionMatrices[m] = new FloatMatrix(size, size, false);

            for (int i = 0; i < size; i++) {
                for (int j = 0; j < size; j++) {
                    substitutionMatrices[m].set(i, j, (i == j) ? 0 : substitutionCosts[m]);
                }
            }

            substitutionMatrices[m].setRow(0, gapCosts[m]);
            substitutionMatrices[m].setColumn(0, gapCosts[m]);
            substitutionMatrices[m].set(0, 0, 0);
        }
    }

    @Override
    public final AlignmentVocabulary[] vocabularies() {
        return vocabularies;
    }

    @Override
    public float cost(final int alignedFeature, final int unalignedFeature) {
        // WARNING: THIS DOES NOT CYCLE THROUGH ALL THE FEATURES!! (IT ONLY LOOKS AS
        // SUBSTITUTIONMATRICES[0])
        return substitutionMatrices[0].getFloat(alignedFeature, unalignedFeature);
    }

    @Override
    public float gapInsertionCost(final int feature, final int sequenceLength) {
        // cost does not depend on sequence length

        // TODO: This assumes that the cost of aligning a feature with a gap is reflexive (that is,
        // that the cost is the same regardless of whether the gap is in the existing alignment or
        // in the new sequence)
        return cost(GAP_INDEX, feature);
    }

    // featureIndex -- the feature we are interested in
    // featureValueIndex -- the value of the feature we are interested in
    @Override
    public float gapInsertionCostForOneFeature(final int feature, final int featureValue) {
        return substitutionMatrices[feature].getFloat(featureValue, GAP_INDEX);
    }

    @Override
    public float cost(final Vector alignedVector, final Vector unalignedVector) {
        float cost = 0f;
        for (int i = 0; i < substitutionMatrices.length; i++) {
            cost += substitutionMatrices[i].getFloat(alignedVector.getInt(i), unalignedVector.getInt(i));
        }
        return cost;
    }

    @Override
    public float gapInsertionCost(final Vector featureVector, final int sequenceLength) {
        // cost does not depend on sequence length

        // TODO: This assumes that the cost of aligning a feature with a gap is reflexive (that is,
        // the same regardless of whether the gap is in the existing alignment or in the new
        // sequence)
        return cost(gapVector, featureVector);
    }

    @Override
    public final int featureCount() {
        return substitutionMatrices.length;
    }

    @Override
    public Sequence createSequence(final Vector[] elements) {
        return new MultipleVocabularyMappedSequence(elements, vocabularies);
    }

    @Override
    public Vector gapVector() {
        return new IntVector(new int[vocabularies.length]);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(8096);
        for (int m = 0; m < substitutionMatrices.length; m++) {
            final Matrix matrix = substitutionMatrices[m];
            if (matrix.rows() <= 100) {
                final AlignmentVocabulary vocabulary = vocabularies[m];

                sb.append("       |");
                for (int j = 0; j < matrix.columns(); j++) {
                    sb.append(String.format(" %5s |", vocabulary.map(j)));
                }
                sb.append('\n');

                for (int i = 0; i < matrix.rows(); i++) {
                    sb.append(String.format(" %5s |", vocabulary.map(i)));
                    for (int j = 0; j < matrix.columns(); j++) {
                        sb.append(String.format(" %5.2f |", matrix.getFloat(i, j)));
                    }
                    sb.append('\n');
                }
            } else {
                sb.append(String.format("Matrix of %d rows", matrix.rows()));
            }
        }
        return sb.toString();
    }
}
