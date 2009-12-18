package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.datastructs.matrices.DenseMatrix;

public class LengthDependentAlignmentModel extends MatrixSubstitutionAlignmentModel {

    public LengthDependentAlignmentModel(final DenseMatrix[] matrices,
            final AlignmentVocabulary[] vocabularies) {
        super(matrices, vocabularies);
    }

}
