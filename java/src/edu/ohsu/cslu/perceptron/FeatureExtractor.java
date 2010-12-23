package edu.ohsu.cslu.perceptron;

import edu.ohsu.cslu.datastructs.vectors.NumericVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;

/**
 * Extracts features from a training example as a {@link NumericVector} suitable for use with a {@link Perceptron}.
 * 
 * Subclasses will generally be instantiated with a sequence of tokens (e.g. a sentence), and consumers will call a
 * <code>featureVector</code> method for each token.
 * 
 * TODO This class is currently specific to bi-tag tasks; could it be generalized to other tagging or regression tasks?
 * 
 * @author Aaron Dunlop
 * @since Oct 15, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public abstract class FeatureExtractor {

    /**
     * @return the length of the feature vectors produced by this extractor
     */
    public abstract int featureCount();

    /**
     * Returns a feature vector suitable for use with a {@link Perceptron}. If previous or subsequent tags are
     * incorporated into the model as features, this method will generally be used during training, when the
     * {@link FeatureExtractor} instance was constructed with gold-standard tagging information. At test time,
     * {@link #forwardFeatureVector(Object, int, boolean[])} will be used instead.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract Vector forwardFeatureVector(Object source, int tokenIndex);

    /**
     * Returns a feature vector suitable for use with a {@link Perceptron}, incorporating previous or subsequent tags
     * into the model as features.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @param tags The estimated tags for previous tokens.
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract Vector forwardFeatureVector(Object source, int tokenIndex, float[] tagScores);

    /**
     * Returns a feature vector suitable for use in backward estimation with a {@link PerceptronModel}. If subsequent
     * tags are incorporated into the model as features, this method will generally be used during training, when the
     * {@link FeatureExtractor} instance was constructed with gold-standard tagging information. At test time,
     * {@link #backwardFeatureVector(Object, int, boolean[])} will be used instead.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @return a feature vector suitable for use with a {@link PerceptronModel}.
     */
    public abstract Vector backwardFeatureVector(Object source, int tokenIndex);

    /**
     * Returns a feature vector suitable for use in backward estimation with a {@link PerceptronModel}, incorporating
     * subsequent tags into the model as features.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @param tagScores The estimated tags for previous tokens.
     * @return a feature vector suitable for use with a {@link PerceptronModel}.
     */
    public abstract Vector backwardFeatureVector(Object source, int tokenIndex, float[] tagScores);
}
