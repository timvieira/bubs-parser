/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortOpenHashMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.Serializable;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.LargeVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * A container object for a tagging model, including an averaged perceptron and the various symbol-mappings required to
 * map features.
 * 
 * @author Aaron Dunlop
 */
class TaggerModel implements Serializable {

    private static final long serialVersionUID = 1L;

    final String featureTemplates;
    final SymbolSet<String> lexicon;
    final SymbolSet<String> unkClassSet;
    final SymbolSet<String> tagSet;
    private transient AveragedPerceptron perceptronModel;

    // Maps feature index (long) -> offset in weight arrays (int)
    private Long2IntOpenHashMap parallelArrayOffsetMap;

    /**
     * Parallel arrays of populated tag index and feature weights. Note that many features will only be observed in
     * conjunction with a subset of the tags; feature weights for other tags will be 0, and we need not store those
     * weights. To compactly represent the non-0 weights, we use parallel arrays, including a series of tag/weight
     * entries for each observed feature. For convenience, the first entry in the sequence for each feature is the
     * number of populated weights (the associated weight entry is ignored).
     * 
     * When we access a feature during tagging, we will probe the weight associated with that feature for each of the
     * (possibly many) tags (call the tag-set T). Arranging the features in separate {@link Vector}s (as in
     * {@link AveragedPerceptron} is conceptually clean and simple, but requires we probe O(T) individual HashMaps, with
     * an essentially random memory-access pattern. Aligning all weights for a specific feature together allows a single
     * hashtable lookup (in {@link #parallelArrayOffsetMap}), followed by a linear scan of these parallel arrays.
     */
    private short[] parallelWeightArrayTags;
    private float[] parallelWeightArray;

    public TaggerModel(final String featureTemplates) {
        this.lexicon = new SymbolSet<String>();
        this.lexicon.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.unkClassSet = new SymbolSet<String>();
        this.unkClassSet.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.tagSet = new SymbolSet<String>();
        this.tagSet.defaultReturnValue(Tagger.NULL_SYMBOL);

        this.featureTemplates = featureTemplates;
    }

    public void finalizeMaps() {
        lexicon.finalize();
        unkClassSet.finalize();
        tagSet.finalize();
    }

    /**
     * Copies the modeled weights from {@link #perceptronModel} (which is convenient and flexible for training) to the
     * parallel array structure ({@link #parallelArrayOffsetMap}, {@link #parallelWeightArrayTags}, and
     * {@link #parallelWeightArray}), which we will use for subsequent tagging.
     */
    public void finalizeModel() {

        // Count the number of non-0 weights
        final Long2ShortOpenHashMap observedWeightCounts = new Long2ShortOpenHashMap();
        for (int i = 0; i < tagSet.size(); i++) {
            for (final long feature : perceptronModel.modelWeights(i).populatedDimensions()) {
                observedWeightCounts.add(feature, (short) 1);
            }
        }

        // Compute the size of the parallel array
        int arraySize = 0;
        for (final long feature : observedWeightCounts.keySet()) {
            arraySize += observedWeightCounts.get(feature);
        }
        // Add 1 entry for each observed feature, to record the number of non-0 weights
        arraySize += observedWeightCounts.size();

        this.parallelArrayOffsetMap = new Long2IntOpenHashMap();
        this.parallelWeightArrayTags = new short[arraySize];
        this.parallelWeightArray = new float[arraySize];

        // Iterate over populated features, probing each tag's perceptron model in turn.
        int index = 0;

        for (final long feature : observedWeightCounts.keySet()) {

            // Start with the observed number of non-0 weights for this feature (leaving the matching entry in the
            // weight array empty)
            parallelWeightArrayTags[index] = observedWeightCounts.get(feature);
            parallelArrayOffsetMap.put(feature, index++);

            // Populate the associated weights for each tag
            for (short tag = 0; tag < tagSet.size(); tag++) {

                final FloatVector modelWeights = perceptronModel.modelWeights(tag);
                final float weight = (modelWeights instanceof LargeVector) ? ((LargeVector) modelWeights)
                        .getFloat(feature) : modelWeights.getFloat((int) feature);

                if (weight != 0) {
                    parallelWeightArrayTags[index] = tag;
                    parallelWeightArray[index++] = weight;
                }
            }
        }
    }

    /**
     * Reads in a serialized tagging model from disk, including the template string, lexicon, trained perceptron, etc.
     * Note that the {@link InputStream} is <em>not</em> closed, since the same model file may contain other serialized
     * models as well. The client should ensure that the stream is closed appropriately.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    public static TaggerModel read(final InputStream is) throws ClassNotFoundException, IOException {
        final TaggerModel m = (TaggerModel) new ObjectInputStream(is).readObject();
        m.perceptronModel.trim();
        return m;
    }

    public void createPerceptronModel(final long featureCount) {
        this.perceptronModel = new AveragedPerceptron(tagSet.size(), featureCount);
    }

    public void train(final int goldClass, final BitVector featureVector) {
        perceptronModel.train(goldClass, featureVector);
    }

    public int classify(final Vector featureVector) {
        if (parallelArrayOffsetMap == null) {
            return perceptronModel.classify(featureVector);
        }

        return perceptronModel.classify(featureVector);

        // int bestClass = -1;
        // float score, bestScore = Float.NEGATIVE_INFINITY;
        // for (int i = 0; i < model.length; i++) {
        // score = featureVector.dotProduct(model[i]) + bias[i];
        // if (score > bestScore) {
        // bestScore = score;
        // bestClass = i;
        // }
        // }
        // return bestClass;

    }
}