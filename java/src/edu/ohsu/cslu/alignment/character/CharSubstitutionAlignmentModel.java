package edu.ohsu.cslu.alignment.character;

import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.Sequence;
import edu.ohsu.cslu.common.Vocabulary;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

public class CharSubstitutionAlignmentModel implements SubstitutionAlignmentModel {

    private final static Vocabulary charVocabulary = new CharVocabulary();

    @Override
    public float cost(int alignedFeature, int unalignedFeature) {
        return (alignedFeature == unalignedFeature) ? 0 : 1;
    }

    @Override
    public float cost(Vector alignedVector, Vector unalignedVector) {
        return alignedVector.equals(unalignedVector) ? 0 : 1;
    }

    @Override
    public float gapInsertionCost(int feature, int sequenceLength) {
        return 1;
    }

    @Override
    public float gapInsertionCost(Vector featureVector, int sequenceLength) {
        return 1;
    }

    @Override
    public int featureCount() {
        return 1;
    }

    @Override
    public Vocabulary[] vocabularies() {
        return new Vocabulary[] { charVocabulary };
    }

    @Override
    public Sequence createSequence(Vector[] elements) {
        return new CharSequence(elements);
    }

    @Override
    public Vector gapVector() {
        return new SparseBitVector(new int[] { 0 }, false);
    }

    @Override
    public float gapInsertionCostForOneFeature(int featureIndex, int featureValueIndex) {
        return 1;
    }

}
