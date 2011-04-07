/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
 * 
 * This file is part of the BUBS Parser.
 * 
 * The BUBS Parser is free software: you can redistribute it and/or 
 * modify  it under the terms of the GNU Affero General Public License 
 * as published by the Free Software Foundation, either version 3 of 
 * the License, or (at your option) any later version.
 * 
 * The BUBS Parser is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */ 
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
     * {@link #forwardFeatureVector(Object, int, float[])} will be used instead.
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
     * @param tagScores The tag scores for previous tokens.
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract Vector forwardFeatureVector(Object source, int tokenIndex, float[] tagScores);

    /**
     * Returns a feature vector suitable for use in backward estimation with a {@link Perceptron}. If subsequent tags
     * are incorporated into the model as features, this method will generally be used during training, when the
     * {@link FeatureExtractor} instance was constructed with gold-standard tagging information. At test time,
     * {@link #backwardFeatureVector(Object, int, float[])} will be used instead.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract Vector backwardFeatureVector(Object source, int tokenIndex);

    /**
     * Returns a feature vector suitable for use in backward estimation with a {@link Perceptron}, incorporating
     * subsequent tags into the model as features.
     * 
     * @param source
     * @param tokenIndex The index of the token for which features should be extracted
     * @param tagScores The tag scores for previous tokens.
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract Vector backwardFeatureVector(Object source, int tokenIndex, float[] tagScores);
}
