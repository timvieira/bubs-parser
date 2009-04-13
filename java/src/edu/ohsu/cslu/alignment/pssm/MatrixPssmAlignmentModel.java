package edu.ohsu.cslu.alignment.pssm;

import edu.ohsu.cslu.alignment.AlignmentVocabulary;
import edu.ohsu.cslu.alignment.CharVocabulary;
import edu.ohsu.cslu.alignment.SubstitutionAlignmentModel;
import edu.ohsu.cslu.common.Vocabulary;
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
public class MatrixPssmAlignmentModel implements HmmAlignmentModel
{
    protected final int MAX_TOSTRING_LENGTH = 256;

    protected final Vocabulary[] vocabularies;
    protected final Matrix[] matrices;
    private final int[] gapSymbols;
    private SubstitutionAlignmentModel substitutionModel;

    public MatrixPssmAlignmentModel(Matrix[] matrices, Vocabulary[] vocabularies)
    {
        this.matrices = matrices;
        this.vocabularies = vocabularies;
        this.gapSymbols = new int[vocabularies.length];

        for (int i = 0; i < vocabularies.length; i++)
        {
            gapSymbols[i] = ((AlignmentVocabulary) vocabularies[i]).gapSymbol();
        }
    }

    protected MatrixPssmAlignmentModel(AlignmentVocabulary vocabulary, SubstitutionAlignmentModel substitutionModel)
    {
        this.vocabularies = new AlignmentVocabulary[] {vocabulary};
        this.matrices = new Matrix[1];
        this.gapSymbols = new int[] {vocabulary.gapSymbol()};
        this.substitutionModel = substitutionModel;
    }

    protected MatrixPssmAlignmentModel(AlignmentVocabulary vocabulary)
    {
        this(vocabulary, null);
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
    public int features()
    {
        return matrices.length;
    }

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
    public float pssmGapInsertionCost(Vector featureVector)
    {
        return substitutionModel.gapInsertionCost(featureVector, columns());
    }

    public void setSubstitutionAlignmentModel(SubstitutionAlignmentModel substitutionModel)
    {
        this.substitutionModel = substitutionModel;
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
}
