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
import it.unimi.dsi.fastutil.longs.Long2ShortAVLTreeMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.LargeBitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FigureOfMerit;

/**
 * A generic multiclass averaged-perceptron tagger. Tested primarily as a POS-tagger, but it should be applicable to
 * other tagging tasks as well.
 * 
 * Input: Tagged tokens, one sentence per line. Format: '(tag token) (tag token) ...'
 * 
 * TODO Handle tree input so we can train directly on a treebank
 * 
 * TODO In classification mode, output tagged sequences
 * 
 * @author Aaron Dunlop
 * @since May 21, 2012
 */
public class Tagger extends ClassifierTool<TagSequence> {

    private static final long serialVersionUID = 1L;

    /**
     * Default Feature Templates:
     * 
     * <pre>
     * # Unigram word, UNK, and tag features
     * wm2,wm1,w,wp1,wp2
     * um2,um1,u,up1,up2
     * tm3,tm2,tm1
     * 
     * # Bigram word features
     * wm2_wm1
     * wm1_w
     * w_wp1
     * wp1_wp2
     * 
     * # Bigram word/UNK features
     * wm2_um1
     * wm1_u
     * u_wp1
     * up1_wp2
     * 
     * # Trigram word features
     * wm2_wm1_w
     * wm1_w_wp1
     * w_wp1_wp2
     * 
     * # Bigram tag features
     * tm3_tm2
     * tm2_tm1
     * 
     * # Word/tag features
     * tm1_wm1
     * tm1_w
     * wm2_tm1
     * </pre>
     */
    @Override
    protected String DEFAULT_FEATURE_TEMPLATES() {
        return "wm2,wm1,w,wp1,wp2,um2,um1,u,up1,up2,tm3,tm2,tm1,wm2_wm1,wm1_w,w_wp1,wp1_wp2,wm2_um1,wm1_u,u_wp1,up1_wp2,wm2_wm1_w,wm1_w_wp1,w_wp1_wp2,tm3_tm2,tm2_tm1,tm1_wm1,tm1_w,wm2_tm1";
    }

    SymbolSet<String> posSet;

    protected SymbolSet<String> tagSet;

    /**
     * Temporary storage of the model during training. After training, this model is superseded by
     * {@link #parallelArrayOffsetMap} and the parallel arrays it references.
     */
    private transient AveragedPerceptron perceptronModel;

    // Maps feature index (long) -> offset in weight arrays (int)
    protected Long2IntOpenHashMap parallelArrayOffsetMap;

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
    protected short[] parallelWeightArrayTags;
    protected float[] parallelWeightArray;

    /**
     * Default constructor
     */
    public Tagger() {
    }

    /**
     * Constructor for use in embedded training (e.g. when jointly training a POS tagger and cell-classifier in
     * {@link CompleteClosureClassifier}).
     */
    Tagger(final String featureTemplates, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
            final SymbolSet<String> tagSet) {
        this.featureTemplates = featureTemplates;
        this.lexicon = lexicon;
        this.decisionTreeUnkClassSet = unkClassSet;
        this.tagSet = tagSet;
    }

    @Override
    protected void run() throws Exception {
        if (trainingIterations > 0) {
            train(inputAsBufferedReader());
        } else {
            readModel(new FileInputStream(modelFile));
            final int[] result = classify(inputAsBufferedReader());
            BaseLogger.singleton().info(
                    String.format("Accuracy=%.2f  Time=%d\n", result[2] * 100f / result[1], result[3]));
        }
    }

    @Override
    public void readModel(final InputStream is) throws IOException, ClassNotFoundException {
        // Read in the model parameters as a temporary java serialized object and copy into this object
        final ObjectInputStream ois = new ObjectInputStream(is);
        final Model tmp = (Model) ois.readObject();
        ois.close();
        initFromModel(tmp);
    }

    protected void initFromModel(final Model tmp) {
        this.featureTemplates = tmp.featureTemplates;
        this.lexicon = tmp.lexicon;
        this.decisionTreeUnkClassSet = tmp.unkClassSet;
        this.tagSet = tmp.tagSet;
        this.featureExtractor = featureExtractor();
        this.parallelArrayOffsetMap = tmp.parallelArrayOffsetMap;
        this.parallelWeightArrayTags = tmp.parallelWeightArrayTags;
        this.parallelWeightArray = tmp.parallelWeightArray;
    }

    protected TaggerFeatureExtractor featureExtractor() {
        return new TaggerFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, tagSet);
    }

    /**
     * Tags the input sequences read from <code>input</code>
     * 
     * @param input
     * @return an array containing: sentences, words, and correct tags (if the input sequences include gold tags)
     * 
     * @throws IOException if a read fails
     */
    protected int[] classify(final BufferedReader input) throws IOException {

        final TaggerFeatureExtractor fe = featureExtractor();
        int sentences = 0, words = 0, correct = 0;
        final long t0 = System.currentTimeMillis();

        for (final String line : inputLines(input)) {
            sentences++;
            final TagSequence tagSequence = createSequence(line);
            for (int i = 0; i < tagSequence.length; i++) {
                tagSequence.toString();
                tagSequence.predictedTags[i] = classify(fe.featureVector(tagSequence, i));
            }

            final StringBuilder sb = new StringBuilder(line.length() * 2);

            for (int j = 0; j < tagSequence.length; j++) {
                sb.append("(" + tagSet.getSymbol(tagSequence.predictedTags[j]) + " " + tagSequence.tokens[j] + ")");
                if (j < tagSequence.length - 1) {
                    sb.append(' ');
                }
                if (tagSequence.tags[j] < 0) {
                    continue;
                }
                if (tagSequence.predictedTags[j] == tagSequence.tags[j]) {
                    correct++;
                }
                words++;
            }
        }
        return new int[] { sentences, words, correct, (int) (System.currentTimeMillis() - t0) };
    }

    protected TagSequence createSequence(final String line) {
        return new TagSequence(line, this);
    }

    private int[] classify(final ArrayList<? extends TagSequence> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures) {

        // Test the development set
        int sentences = 0, words = 0, correct = 0;
        final long t0 = System.currentTimeMillis();

        for (int j = 0; j < devCorpusFeatures.size(); j++) {
            sentences++;
            final TagSequence tagSequence = devCorpusSequences.get(j);
            final BitVector[] featureVectors = devCorpusFeatures.get(j);

            for (int k = 0; k < featureVectors.length; k++) {
                if (featureVectors[k] != null) {
                    tagSequence.predictedTags[k] = classify(featureVectors[k]);
                    if (tagSequence.predictedTags[k] == tagSequence.tags[k]) {
                        correct++;
                    }
                    words++;
                }
            }
            Arrays.fill(tagSequence.predictedTags, (short) 0);
        }
        return new int[] { sentences, words, correct, (int) (System.currentTimeMillis() - t0) };
    }

    /**
     * Tags the input sequences, and returns a reference to the array of predicted tags (
     * {@link TagSequence#predictedTags}). Intended for use in {@link FigureOfMerit} and {@link CellSelector}
     * initialization.
     * 
     * @param sequence
     * @return a reference to the array of predicted tags ({@link TagSequence#predictedTags})
     */
    public short[] classify(final TagSequence sequence) {

        for (int i = 0; i < sequence.length; i++) {
            sequence.predictedTags[i] = classify(featureExtractor.featureVector(sequence, i));
        }
        return sequence.predictedTags;
    }

    public short classify(final BitVector featureVector) {

        if (parallelArrayOffsetMap == null) {
            return (short) perceptronModel.classify(featureVector);
        }

        // Compute individual dot-products for each tag
        final float[] dotProducts = new float[tagSet.size()];

        // Unfortunately, we need separate cases for LargeVector and normal Vector classes
        if (featureVector instanceof LargeVector) {
            final LargeBitVector largeFeatureVector = (LargeBitVector) featureVector;

            // Iterate over each feature
            for (final long feature : largeFeatureVector.longValues()) {

                final int offset = parallelArrayOffsetMap.get(feature);
                // Skip any features that aren't populated in the model (those we didn't observe in the training data)
                if (offset < 0) {
                    continue;
                }

                // The first 'tag' position denotes the number of populated weights for this feature
                final int end = offset + parallelWeightArrayTags[offset];

                // Add each non-0 weight to the appropriate dot-product
                for (int i = offset + 1; i <= end; i++) {
                    dotProducts[parallelWeightArrayTags[i]] += parallelWeightArray[i];
                }
            }
        } else {
            for (final int feature : featureVector.values()) {
                final int offset = parallelArrayOffsetMap.get(feature);
                if (offset < 0) {
                    continue;
                }
                final int end = offset + parallelWeightArrayTags[offset];
                for (int i = offset + 1; i <= end; i++) {
                    dotProducts[parallelWeightArrayTags[i]] += parallelWeightArray[i];
                }
            }
        }

        // Find the maximum dot-product
        float max = dotProducts[0];
        short argMax = 0;
        for (short tag = 1; tag < dotProducts.length; tag++) {
            if (dotProducts[tag] > max) {
                max = dotProducts[tag];
                argMax = tag;
            }
        }
        return argMax;
    }

    /**
     * Trains a tagging model, optionally validating it on {@link #devSet} and writing it to {@link #modelFile} as a
     * Java serialized object.
     * 
     * @param input
     * @throws IOException
     */
    @Override
    protected void train(final BufferedReader input) throws IOException {

        final long startTime = System.currentTimeMillis();
        final ArrayList<TagSequence> trainingCorpusSequences = new ArrayList<TagSequence>();
        final ArrayList<TagSequence> devCorpusSequences = new ArrayList<TagSequence>();

        if (this.lexicon == null) {
            this.lexicon = new SymbolSet<String>();
            this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        }

        if (this.decisionTreeUnkClassSet == null) {
            this.decisionTreeUnkClassSet = new SymbolSet<String>();
            this.decisionTreeUnkClassSet.defaultReturnValue(Grammar.nullSymbolStr);
        }

        this.tagSet = new SymbolSet<String>();
        this.tagSet.defaultReturnValue(Grammar.nullSymbolStr);

        //
        // Read in the training corpus and map each token
        //
        for (final String line : inputLines(input)) {
            trainingCorpusSequences.add(createSequence(line));
        }
        finalizeMaps();

        //
        // Read in the dev set
        //
        if (devSet != null) {
            for (final String line : fileLines(devSet)) {
                final TagSequence tagSequence = createSequence(line);
                devCorpusSequences.add(tagSequence);
            }
        }

        //
        // Train and (optionally) test on dev-set
        //
        train(trainingCorpusSequences, devCorpusSequences, trainingIterations);

        // Write out the model file to disk, using Java object serialization
        if (modelFile != null) {
            final FileOutputStream fos = new FileOutputStream(modelFile);
            new ObjectOutputStream(fos).writeObject(model());
            fos.close();
        }

        BaseLogger.singleton().info(
                String.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000));
    }

    /**
     * @return A model representing the entire state of the {@link Tagger}. Generally a subclass of {@link Model},
     *         adding whatever additional state is required by the specific {@link Tagger}.
     */
    protected Model model() {
        return new Model(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, tagSet, parallelArrayOffsetMap,
                parallelWeightArrayTags, parallelWeightArray);
    }

    void train(final ArrayList<? extends TagSequence> trainingCorpusSequences,
            final ArrayList<? extends TagSequence> devCorpusSequences, final int iterations) {

        featureExtractor = featureExtractor();
        perceptronModel = new AveragedPerceptron(tagSet.size(), featureExtractor.vectorLength());

        //
        // Pre-compute all features
        //
        final ArrayList<BitVector[]> trainingCorpusFeatures = extractFeatures(trainingCorpusSequences);
        final ArrayList<BitVector[]> devCorpusFeatures = extractFeatures(devCorpusSequences);

        //
        // Iterate over training corpus, training the model
        //
        for (int i = 1; i <= iterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final TagSequence tagSequence = trainingCorpusSequences.get(j);

                final BitVector[] featureVectors = trainingCorpusFeatures.get(j);
                for (int k = 0; k < featureVectors.length; k++) {
                    if (featureVectors[k] != null) {
                        perceptronModel.train(tagSequence.tags[k], featureVectors[k]);
                    }
                }

                progressBar(100, 5000, j);
            }
            System.out.println();

            // Skip the last iteration - we'll test after we finalize below
            if (!devCorpusSequences.isEmpty() && i < iterations) {
                final int[] devResult = classify(devCorpusSequences, devCorpusFeatures);
                BaseLogger.singleton().info(
                        String.format("Iteration=%d Devset Accuracy=%.2f  Time=%d\n", trainingIterations, devResult[2]
                                * 100f / devResult[1], devResult[3]));
            }
        }

        // Store the trained model in a memory- and cache-efficient format for tagging (we do this even if we're not
        // writing out the serialized model, specifically so we can unit test train() and tag())
        finalizeModel();

        // Test on the dev-set
        if (!devCorpusSequences.isEmpty()) {
            final int[] devResult = classify(devCorpusSequences, devCorpusFeatures);
            BaseLogger.singleton().info(
                    String.format("Iteration=%d Devset Accuracy=%.2f  Time=%d\n", trainingIterations, devResult[2]
                            * 100f / devResult[1], devResult[3]));

        }
    }

    /**
     * Extracts features from a set of sequences. Feature-vectors are only extracted for sequences with populated tags.
     * For full-sequence taggers // (like POS taggers) this is irrelevant, but for taggers which only tag certain tokens
     * (like {@link UnkClassTagger}), we generally won't tag every token. In those cases, some entries in the
     * {@link BitVector}[] arrays will be null.
     * 
     * @param sequences
     * @return Features extracted from the supplied sequences.
     */
    protected ArrayList<BitVector[]> extractFeatures(final ArrayList<? extends TagSequence> sequences) {

        final ArrayList<BitVector[]> features = new ArrayList<BitVector[]>();

        for (final TagSequence tagSequence : sequences) {

            final BitVector[] featureVectors = new BitVector[tagSequence.length];
            for (int i = 0; i < tagSequence.length; i++) {
                if (tagSequence.tags[i] >= 0) {
                    featureVectors[i] = featureExtractor.featureVector(tagSequence, i);
                }
            }
            features.add(featureVectors);
        }
        return features;
    }

    @Override
    protected void finalizeMaps() {
        super.finalizeMaps();
        if (posSet != null) {
            posSet.finalize();
        }
        tagSet.finalize();
    }

    /**
     * Copies the modeled weights from an {@link AveragedPerceptron} (which is convenient and flexible for training) to
     * the parallel array structure ({@link #parallelArrayOffsetMap}, {@link #parallelWeightArrayTags}, and
     * {@link #parallelWeightArray}), which we will use for subsequent tagging.
     */
    public void finalizeModel() {

        // Count the number of non-0 weights
        final Long2ShortAVLTreeMap observedWeightCounts = new Long2ShortAVLTreeMap();
        for (int i = 0; i < tagSet.size(); i++) {
            for (final long feature : perceptronModel.modelWeights(i).populatedDimensions()) {
                observedWeightCounts.put(feature, (short) (observedWeightCounts.get(feature) + 1));
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
        parallelArrayOffsetMap.defaultReturnValue(-1);
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

    public SymbolSet<String> tagSet() {
        return tagSet;
    }

    public static void main(final String[] args) {
        run(args);
    }

    /**
     * Represents the result of a binary classification run and computes precision, recall, etc.
     */
    protected static class MulticlassClassifierResult {

        int totalSequences = 0;
        int positiveExamples = 0, classifiedPositive = 0, correctPositive = 0;
        int negativeExamples = 0, classifiedNegative = 0, correctNegative = 0;

        long time;

        public float accuracy() {
            return (correctPositive + correctNegative) * 1f / (positiveExamples + negativeExamples);
        }

        public float precision() {
            final int incorrectPositive = negativeExamples - correctNegative;
            return correctPositive * 1f / (correctPositive + incorrectPositive);
        }

        public float recall() {
            return correctPositive * 1f / positiveExamples;
        }

        public float negativePrecision() {
            final int incorrectNegative = positiveExamples - correctPositive;
            return correctNegative * 1f / (correctNegative + incorrectNegative);
        }

        public float negativeRecall() {
            return correctNegative * 1f / negativeExamples;
        }
    }

    protected static class Model extends ClassifierTool.Model {

        private static final long serialVersionUID = 1L;

        final SymbolSet<String> tagSet;

        private final Long2IntOpenHashMap parallelArrayOffsetMap;
        private final short[] parallelWeightArrayTags;
        private final float[] parallelWeightArray;

        protected Model(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet, final SymbolSet<String> tagSet,
                final Long2IntOpenHashMap parallelArrayOffsetMap, final short[] parallelWeightArrayTags,
                final float[] parallelWeightArray) {

            super(featureTemplates, lexicon, unkClassSet, posSet);
            this.tagSet = tagSet;
            this.parallelArrayOffsetMap = parallelArrayOffsetMap;
            this.parallelWeightArrayTags = parallelWeightArrayTags;
            this.parallelWeightArray = parallelWeightArray;
        }
    }
}