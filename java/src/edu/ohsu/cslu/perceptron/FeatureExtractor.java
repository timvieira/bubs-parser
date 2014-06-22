/*
 * Copyright 2010-2014, Oregon Health & Science University
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>.
 * 
 * Further documentation and contact information is available at
 *   https://code.google.com/p/bubs-parser/ 
 */
package edu.ohsu.cslu.perceptron;

import java.io.Serializable;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.NumericVector;

/**
 * Extracts features from a training example as a {@link NumericVector} suitable for use with a {@link Perceptron}.
 * 
 * Subclasses will generally be instantiated with a sequence of tokens (e.g. a sentence), and consumers will call a
 * <code>featureVector</code> method for each token.
 * 
 * Most subclasses use an {@link Enum} to define individual feature templates (for example, see
 * {@link TaggerFeatureExtractor} and {@link ConstituentBoundaryFeatureExtractor}). Feature templates can be combined,
 * separated with underscores. Feature templates can be specified in a single string, separated by commas; or in a
 * feature file (see {@link ClassifierTool#readFeatureTemplateFile(java.io.File)}. The semantics of feature templates
 * vary somewhat across {@link FeatureExtractor} implementations and their associated {@link Enum}s, but common patterns
 * include:
 * 
 * <ul>
 * <li>t: POS tag</li>
 * <li>w: Word / token</li>
 * <li>u: Unknown-word (UNK) token</li>
 * <li>m#: Minus-# (e.g., tm1 = tag(i-1))</li>
 * <li>p#: Plus-#</li>
 * <li>l: Left (e.g. lum1 = UNK-token 1 word before the left boundary)</li>
 * <li>r: Right
 * </ul>
 * 
 * Other examples:
 * <ul>
 * <li>rwp1: Word to the right of the right boundary</li>
 * <li>tm2_tm1: Bitag feature including tag(i-2) and tag(i-1)</li>
 * <li>tm1_w: Word/tag feature including tag(i-1) and word(i)</li>
 * </ul>
 * 
 * @param <I> the type of sequence or other input that features will be extracted from
 * 
 * @author Aaron Dunlop
 * @since Oct 15, 2010
 */
public abstract class FeatureExtractor<I> implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * @return the length of the feature vectors produced by this extractor
     */
    public abstract long vectorLength();

    /**
     * @return the number of feature templates - i.e., the number of features which will populated in each vector
     *         produced by this extractor.
     */
    public abstract int templateCount();

    /**
     * Returns a feature vector suitable for use with a {@link Perceptron}.
     * 
     * @param input
     * @param position The position in the input for which features should be extracted
     * @return a feature vector suitable for use with a {@link Perceptron}.
     */
    public abstract BitVector featureVector(I input, int position);
}
