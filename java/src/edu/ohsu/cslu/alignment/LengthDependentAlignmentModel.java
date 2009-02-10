package edu.ohsu.cslu.alignment;

import edu.ohsu.cslu.math.linear.Matrix;

public class LengthDependentAlignmentModel extends MatrixSubstitutionAlignmentModel
{

    public LengthDependentAlignmentModel(Matrix[] matrices, AlignmentVocabulary[] vocabularies)
    {
        super(matrices, vocabularies);
    }

}
