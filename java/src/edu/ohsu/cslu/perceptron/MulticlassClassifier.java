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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.longs.Long2ShortAVLTreeMap;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Reader;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.matrices.IntMatrix;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.DenseIntVector;
import edu.ohsu.cslu.datastructs.vectors.FloatVector;
import edu.ohsu.cslu.datastructs.vectors.IntVector;
import edu.ohsu.cslu.datastructs.vectors.LargeBitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.cellselector.CellSelector;
import edu.ohsu.cslu.parser.fom.FigureOfMeritModel.FigureOfMerit;

/**
 * An efficient implementation of multiclass classification using an averaged perceptron classifier. Tested primarily as
 * a POS tagger, but applicable to other tagging tasks as well. Performs POS tagging at over 200k words/second using the
 * ~35 tags of the WSJ tag-set.
 * 
 * Training uses standard {@link Vector} data structures. When training is complete, those structures are copied into a
 * more compact and cache-efficient format for use during inference (see {@link #parallelArrayOffsetMap},
 * {@link #parallelWeightArray}, and {@link #parallelWeightArrayTags}).
 * 
 * @param <S> The class of sequence processed by this {@link Classifier}
 * @param <F> A {@link FeatureExtractor} class appropriate for <code>S</code>
 * @param <I> Canonical representation of a training or test instance (most subclasses will use {@link String}, but a
 *            few may use a more specific representation)
 * 
 * @author Aaron Dunlop
 */
public abstract class MulticlassClassifier<S extends MulticlassSequence, F extends FeatureExtractor<S>, I> extends
        ClassifierTool<S> {

    private static final long serialVersionUID = 1L;

    /**
     * Perform cross-validation training and output accuracy over all folds. Note that when performing cross-validation,
     * we can't output a single model, so this option cannot be used in conjunction with '-m'
     */
    @Option(name = "-xv", metaVar = "folds", optionalChoiceGroup = "model", usage = "Perform k-fold cross-validation on the training set (dev-set will be ignored and no model file will be written)")
    protected int crossValidationFolds;

    /**
     * Performs training using a random sample from the full training corpus. Useful to gauge performance as training
     * data is increased, and can be combined with cross validation ('-xv').
     */
    @Option(name = "-tf", metaVar = "fraction", requires = "-ti", usage = "Limit training to a random sample of the full training corpus")
    protected float trainingFraction = 1;

    protected SymbolSet<String> tagSet;

    /**
     * Temporary storage of the model during training. After training, this model is superseded by
     * {@link #parallelArrayOffsetMap} and the parallel arrays it references.
     */
    protected transient AveragedPerceptron perceptronModel;

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
    public MulticlassClassifier() {
        super();
    }

    /**
     * Constructor for use in embedded training (e.g. when jointly training a POS tagger and cell-classifier in
     * {@link CompleteClosureClassifier}).
     */
    public MulticlassClassifier(final String featureTemplates, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet) {

        this.featureTemplates = featureTemplates;
        this.lexicon = lexicon;
        this.decisionTreeUnkClassSet = unkClassSet;
        this.tagSet = tagSet;
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

    @SuppressWarnings("unchecked")
    protected FeatureExtractor<S> featureExtractor() {

        try {
            final Class<FeatureExtractor<S>> c = (Class<FeatureExtractor<S>>) ((ParameterizedType) getClass()
                    .getGenericSuperclass()).getActualTypeArguments()[1];
            return c.getConstructor(new Class[] { String.class }).newInstance(featureTemplates);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Tags the input sequences read from <code>input</code>, which is assumed to include gold tags. The result includes
     * accuracy evaluation.
     * 
     * @param input Input text including gold tags
     * @return a {@link MulticlassClassifierResult} containing: sentences, words, and correct tags (if the input
     *         sequences include gold tags)
     * 
     * @throws IOException if a read fails
     */
    protected MulticlassClassifierResult testAccuracy(final Iterable<I> input) throws IOException {

        final FeatureExtractor<S> fe = featureExtractor();
        final long t0 = System.currentTimeMillis();

        final MulticlassClassifierResult result = createResult();

        for (final I instance : input) {

            final S sequence = createSequence(instance);
            final BitVector[] featureVectors = new BitVector[sequence.length()];
            for (int i = 0; i < featureVectors.length; i++) {
                featureVectors[i] = fe.featureVector(sequence, i);
            }
            classify(sequence, featureVectors, result);
        }
        result.time = (int) (System.currentTimeMillis() - t0);
        return result;
    }

    protected abstract S createSequence(final I line);

    private MulticlassClassifierResult classify(final ArrayList<? extends MulticlassSequence> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures, MulticlassClassifierResult result) {

        if (result == null) {
            result = createResult();
        }

        // Test the development set
        final long t0 = System.currentTimeMillis();

        for (int j = 0; j < devCorpusFeatures.size(); j++) {
            classify(devCorpusSequences.get(j), devCorpusFeatures.get(j), result);
            Arrays.fill(devCorpusSequences.get(j).predictedClasses(), (short) 0);
        }
        result.time += (int) (System.currentTimeMillis() - t0);
        return result;
    }

    private void classify(final MulticlassSequence sequence, final BitVector[] featureVectors,
            final MulticlassClassifierResult result) {

        result.sequences++;

        for (int k = 0; k < featureVectors.length; k++) {
            final BitVector featureVector = featureVectors[k];

            if (featureVector == null) {
                continue;
            }

            sequence.setPredictedClass(k, classify(featureVector));
            final short goldClass = sequence.goldClass(k);
            if (goldClass < 0) {
                continue;
            }

            result.instancesByClass.set(goldClass, result.instancesByClass.getInt(goldClass) + 1);
            if (sequence.predictedClass(k) == goldClass) {
                result.correct++;
                result.correctByClass.set(goldClass, result.correctByClass.getInt(goldClass) + 1);
            }
            result.instances++;
        }
    }

    /**
     * Tags the input sequences, and returns a reference to the array of predicted tags (
     * {@link MulticlassSequence#predictedClasses}). Intended for use in {@link FigureOfMerit} and {@link CellSelector}
     * initialization.
     * 
     * @param sequence
     * @return a reference to the array of predicted tags ({@link MulticlassSequence#predictedClasses})
     */
    public short[] classify(final S sequence) {

        for (int i = 0; i < sequence.length(); i++) {
            sequence.setPredictedClass(i, classify(featureExtractor.featureVector(sequence, i)));
        }
        return sequence.predictedClasses();
    }

    public short classify(final BitVector featureVector) {

        if (parallelArrayOffsetMap == null) {
            return perceptronModel.classify(featureVector);
        }

        return classify(dotProducts(featureVector));
    }

    protected final float[] dotProducts(final BitVector featureVector) {
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
        return dotProducts;
    }

    /**
     * Returns the selected class, per the computed dot products. The default implementation is a simple <b>argmax</b>,
     * but a chained classifier (e.g. {@link AdaptiveBeamClassifier}) evaluates each dot-product sequentially as
     * independent binary classifiers.
     * 
     * @param dotProducts
     * @return The selected class per the computed dot products.
     */
    protected short classify(final float[] dotProducts) {
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
        final ArrayList<S> trainingCorpusSequences = readTrainingCorpus(input);

        //
        // Read in the dev set
        //
        final ArrayList<S> devCorpusSequences = new ArrayList<S>();
        if (devSet != null) {
            for (final I instance : corpusReader(fileAsBufferedReader(devSet))) {
                final S sequence = createSequence(instance);
                devCorpusSequences.add(sequence);
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

    private ArrayList<S> readTrainingCorpus(final BufferedReader input) throws IOException {
        final ArrayList<S> trainingCorpusSequences = new ArrayList<S>();

        if (this.lexicon == null) {
            this.lexicon = new SymbolSet<String>();
            this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        }

        if (this.decisionTreeUnkClassSet == null) {
            this.decisionTreeUnkClassSet = new SymbolSet<String>();
            this.decisionTreeUnkClassSet.defaultReturnValue(Grammar.nullSymbolStr);
        }

        if (tagSet == null) {
            this.tagSet = new SymbolSet<String>();
            this.tagSet.defaultReturnValue(Grammar.nullSymbolStr);
        }

        //
        // Read in the training corpus and map each token
        //
        for (final I instance : corpusReader(input)) {
            trainingCorpusSequences.add(createSequence(instance));
        }
        finalizeMaps();

        return trainingCorpusSequences;
    }

    /**
     * Performs k-fold cross validation. Splits the training data into k folds, trains k separate models (holding out
     * each fold from one training run), and reports accuracy averaged over all training runs.
     * 
     * @param input
     * @throws IOException
     */
    protected MulticlassClassifierResult crossValidate(final BufferedReader input) throws IOException {

        final ArrayList<S> allSequences = readTrainingCorpus(input);

        final int foldSize = Math.round(1f * allSequences.size() / crossValidationFolds);

        MulticlassClassifierResult totalResult = createResult();

        for (int i = 0; i < crossValidationFolds; i++) {

            BaseLogger.singleton().info("--- Cross-validation fold " + i + " ---");
            //
            // Separate the training corpus into training and held-out sets
            //
            final ArrayList<S> trainingSequences = new ArrayList<S>();
            final ArrayList<S> devSequences = new ArrayList<S>();

            for (int j = 0; j < allSequences.size(); j++) {
                if (j < i * foldSize || j >= (i + 1) * foldSize) {
                    trainingSequences.add(allSequences.get(j));
                } else {
                    devSequences.add(allSequences.get(j));
                }
            }

            //
            // Train
            //
            totalResult = totalResult.sum(train(trainingSequences, devSequences, trainingIterations));
        }

        return totalResult;
    }

    /**
     * Creates an instance of {@link MulticlassClassifierResult} or the appropriate subclass thereof
     * 
     * @return Result
     */
    protected MulticlassClassifierResult createResult() {
        return new MulticlassClassifierResult(tagSet);
    }

    /**
     * @return A model representing the entire state of the {@link MulticlassClassifier}. Generally a subclass of
     *         {@link Model}, adding any additional state required by the specific subclass.
     */
    protected Model model() {
        return new Model(featureTemplates, lexicon, decisionTreeUnkClassSet, tagSet, parallelArrayOffsetMap,
                parallelWeightArrayTags, parallelWeightArray);
    }

    MulticlassClassifierResult train(final ArrayList<S> trainingCorpusSequences, final ArrayList<S> devCorpusSequences,
            final int iterations) {

        final ArrayList<S> selectedTrainingSequences;
        if (trainingFraction == 1) {
            selectedTrainingSequences = trainingCorpusSequences;
        } else {
            // Sample randomly from the full training corpus
            selectedTrainingSequences = new ArrayList<S>();
            @SuppressWarnings("unchecked")
            final ArrayList<S> tmp = (ArrayList<S>) trainingCorpusSequences.clone();
            Collections.shuffle(tmp);
            for (int i = Math.round(trainingCorpusSequences.size() * trainingFraction); i > 0; i--) {
                selectedTrainingSequences.add(tmp.get(i));
            }
        }

        featureExtractor = featureExtractor();
        perceptronModel = new AveragedPerceptron(tagSet.size(), featureExtractor.vectorLength());

        //
        // Pre-compute all features
        //
        final ArrayList<BitVector[]> trainingCorpusFeatures = extractFeatures(selectedTrainingSequences);
        final ArrayList<BitVector[]> devCorpusFeatures = extractFeatures(devCorpusSequences);

        //
        // Iterate over training corpus, training the model
        //
        for (int i = 1; i <= iterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final S sequence = selectedTrainingSequences.get(j);

                final BitVector[] featureVectors = trainingCorpusFeatures.get(j);
                for (int k = 0; k < featureVectors.length; k++) {
                    final BitVector featureVector = featureVectors[k];
                    if (featureVector != null) {
                        train(sequence.goldClass(k), featureVector);
                    }
                }

                progressBar(100, 5000, j);
            }
            System.out.println();

            // Skip the last iteration - we'll test after we finalize below
            if (!devCorpusSequences.isEmpty() && i < iterations) {
                evaluateDevset(devCorpusSequences, devCorpusFeatures, i);
            }
        }

        // Store the trained model in a memory- and cache-efficient format for tagging (we do this even if we're not
        // writing out the serialized model, specifically so we can unit test train() and tag())
        finalizeModel();

        // Test on the dev-set
        if (!devCorpusSequences.isEmpty()) {
            return evaluateDevset(devCorpusSequences, devCorpusFeatures, iterations);
        }

        return null;
    }

    /**
     * Evaluates the development set and reports accuracy. Returns the result as a {@link MulticlassClassifierResult}
     * 
     * @param devCorpusSequences
     * @param devCorpusFeatures
     * @param trainingIteration
     * @return Dev-set evaluation
     */
    protected MulticlassClassifierResult evaluateDevset(final ArrayList<S> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures, final int trainingIteration) {

        final MulticlassClassifierResult devResult = classify(devCorpusSequences, devCorpusFeatures, null);
        BaseLogger.singleton().info(
                String.format("Iteration=%d Devset %s  Time=%d\n", trainingIteration, devResult.toString(),
                        devResult.time));

        if (BaseLogger.singleton().isLoggable(Level.FINE)) {
            BaseLogger.singleton().fine(devResult.resultByClassReport());
        }

        return devResult;
    }

    protected void train(final short goldClass, final BitVector featureVector) {
        perceptronModel.train(goldClass, featureVector);
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
    protected ArrayList<BitVector[]> extractFeatures(final ArrayList<S> sequences) {

        final ArrayList<BitVector[]> features = new ArrayList<BitVector[]>();

        for (final S tagSequence : sequences) {

            final BitVector[] featureVectors = new BitVector[tagSequence.length()];
            for (int i = 0; i < tagSequence.length(); i++) {
                if (tagSequence.goldClass(i) >= 0) {
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
        tagSet.finalize();
    }

    private void finalizeModel() {

        perceptronModel.averageAllFeatures();

        final Long2ShortAVLTreeMap observedWeightCounts = observedWeightCounts(perceptronModel.avgWeights);
        final int arraySize = finalizedArraySize(observedWeightCounts);

        this.parallelArrayOffsetMap = new Long2IntOpenHashMap();
        this.parallelArrayOffsetMap.defaultReturnValue(-1);
        this.parallelWeightArrayTags = new short[arraySize];
        this.parallelWeightArray = new float[arraySize];

        finalizeModel(perceptronModel.avgWeights, observedWeightCounts, parallelArrayOffsetMap,
                parallelWeightArrayTags, parallelWeightArray);
    }

    static Long2ShortAVLTreeMap observedWeightCounts(final FloatVector[] avgWeights) {
        // Count the number of non-0 weights
        final Long2ShortAVLTreeMap observedWeightCounts = new Long2ShortAVLTreeMap();

        for (int i = 0; i < avgWeights.length; i++) {
            for (final long feature : avgWeights[i].populatedDimensions()) {
                observedWeightCounts.put(feature, (short) (observedWeightCounts.get(feature) + 1));
            }
        }
        return observedWeightCounts;
    }

    static int finalizedArraySize(final Long2ShortAVLTreeMap observedWeightCounts) {
        // Compute the size of the parallel array
        int arraySize = 0;
        for (final long feature : observedWeightCounts.keySet()) {
            arraySize += observedWeightCounts.get(feature);
        }
        // Add 1 entry for each observed feature, to record the number of non-0 weights
        arraySize += observedWeightCounts.size();

        return arraySize;
    }

    static void finalizeModel(final FloatVector[] avgWeights, final Long2ShortAVLTreeMap observedWeightCounts,
            final Long2IntOpenHashMap parallelArrayOffsetMap, final short[] parallelWeightArrayTags,
            final float[] parallelWeightArray) {

        // Iterate over populated features, probing each tag's perceptron model in turn.
        int index = 0;

        for (final long feature : observedWeightCounts.keySet()) {

            // Start with the observed number of non-0 weights for this feature (leaving the matching entry in the
            // weight array empty)
            parallelWeightArrayTags[index] = observedWeightCounts.get(feature);
            parallelArrayOffsetMap.put(feature, index++);

            // Populate the associated weights for each class
            for (short c = 0; c < avgWeights.length; c++) {

                final FloatVector modelWeights = avgWeights[c];
                final float weight = (modelWeights instanceof LargeVector) ? ((LargeVector) modelWeights)
                        .getFloat(feature) : modelWeights.getFloat((int) feature);

                if (weight != 0) {
                    parallelWeightArrayTags[index] = c;
                    parallelWeightArray[index++] = weight;
                }
            }
        }
    }

    /**
     * Copy-and-paste from
     * {@link #finalizeModel(FloatVector[], Long2ShortAVLTreeMap, Long2IntOpenHashMap, short[], float[])}, with a byte[]
     * array for tags instead of short[].
     * 
     * @param avgWeights
     * @param observedWeightCounts
     * @param parallelArrayOffsetMap
     * @param parallelWeightArrayTags
     * @param parallelWeightArray
     */
    static void finalizeModel(final FloatVector[] avgWeights, final Long2ShortAVLTreeMap observedWeightCounts,
            final Long2IntOpenHashMap parallelArrayOffsetMap, final byte[] parallelWeightArrayTags,
            final float[] parallelWeightArray) {

        // Iterate over populated features, probing each tag's perceptron model in turn.
        int index = 0;

        for (final long feature : observedWeightCounts.keySet()) {

            // Start with the observed number of non-0 weights for this feature (leaving the matching entry in the
            // weight array empty)
            parallelWeightArrayTags[index] = (byte) observedWeightCounts.get(feature);
            parallelArrayOffsetMap.put(feature, index++);

            // Populate the associated weights for each class
            for (byte c = 0; c < avgWeights.length; c++) {

                final FloatVector modelWeights = avgWeights[c];
                final float weight = (modelWeights instanceof LargeVector) ? ((LargeVector) modelWeights)
                        .getFloat(feature) : modelWeights.getFloat((int) feature);

                if (weight != 0) {
                    parallelWeightArrayTags[index] = c;
                    parallelWeightArray[index++] = weight;
                }
            }
        }
    }

    public SymbolSet<String> tagSet() {
        return tagSet;
    }

    /**
     * Default to reading input line-by-line. Subclasses which want to read other formats should override this method
     * 
     * @param r
     * @return <code>Iterable</code> over input instances
     * @throws IOException if an error occurs while opening or reading the input corpus
     */
    @SuppressWarnings("unchecked")
    public Iterable<I> corpusReader(final Reader r) throws IOException {
        return (Iterable<I>) new LineIterator(r);
    }

    protected static class MulticlassClassifierResult {

        protected final SymbolSet<String> tagSet;
        protected int sequences, instances, correct;
        protected IntVector instancesByClass, correctByClass;

        /**
         * Counts of the erroneously predicted classes for each gold class. Indexed by goldClass, prediction; excludes
         * counts for correct predictions (i.e., m[x,x] == 0 for all x)
         */
        protected final IntMatrix confusionMatrix;
        protected int time;

        public MulticlassClassifierResult(final int classes) {
            this(null, classes);
        }

        public MulticlassClassifierResult(final SymbolSet<String> tagSet) {
            this(tagSet, tagSet.size());
        }

        private MulticlassClassifierResult(final SymbolSet<String> tagSet, final int classes) {
            this.tagSet = tagSet;
            this.instancesByClass = new DenseIntVector(classes);
            this.correctByClass = new DenseIntVector(classes);
            this.confusionMatrix = new IntMatrix(classes, classes);
        }

        public MulticlassClassifierResult(final SymbolSet<String> tagSet, final int sequences, final int instances,
                final int correct, final IntVector instancesByClass, final IntVector correctByClass,
                final IntMatrix confusionMatrix, final int time) {

            this.tagSet = tagSet;
            this.sequences = sequences;
            this.instances = instances;
            this.correct = correct;
            this.time = time;

            assert (instancesByClass.length() == correctByClass.length());
            this.instancesByClass = instancesByClass;
            this.correctByClass = correctByClass;
            this.confusionMatrix = confusionMatrix;
        }

        public void addSequence() {
            sequences++;
        }

        public void addInstance(final short goldClass, final short predictedClass) {
            instances++;
            if (instancesByClass != null) {
                instancesByClass.set(goldClass, instancesByClass.getInt(goldClass) + 1);
            }
            if (goldClass == predictedClass) {
                correct++;
                if (correctByClass != null) {
                    correctByClass.set(goldClass, correctByClass.getInt(goldClass) + 1);
                }
            } else {
                // Update the confusion matrix
                confusionMatrix.increment(goldClass, predictedClass);
            }
        }

        public void addTime(final int increment) {
            time += increment;
        }

        public float accuracy() {
            return correct * 1f / instances;
        }

        public float accuracy(final short goldClass) {
            return correctByClass.getFloat(goldClass) / instancesByClass.getFloat(goldClass);
        }

        public MulticlassClassifierResult sum(final MulticlassClassifierResult other) {
            return new MulticlassClassifierResult(tagSet, sequences + other.sequences, instances + other.instances,
                    correct + other.correct, (IntVector) instancesByClass.add(other.instancesByClass),
                    (IntVector) correctByClass.add(other.correctByClass),
                    (IntMatrix) confusionMatrix.add(other.confusionMatrix), time + other.time);
        }

        public int time() {
            return time;
        }

        public String resultByClassReport() {

            final StringBuilder sb = new StringBuilder(512);

            for (short goldClass = 0; goldClass < instancesByClass.length(); goldClass++) {

                if (instancesByClass.getInt(goldClass) > 0) {
                    sb.append(String.format("%s (Class %d)    Instances: %5d  Correct: %5d  Accuracy: %.2f\n",
                            tagSet.getSymbol(goldClass), goldClass, instancesByClass.getInt(goldClass),
                            correctByClass.getInt(goldClass), accuracy(goldClass)));
                }
            }
            return sb.toString();
        }

        public String confusionMatrixReport() {

            final StringBuilder sb = new StringBuilder(512);

            for (short goldClass = 0; goldClass < confusionMatrix.rows(); goldClass++) {

                // Skip unobserved classes
                if (instancesByClass.getInt(goldClass) == 0) {
                    continue;

                }

                sb.append(String.format("%-20s (Class %5d): total instances %d\n", tagSet.getSymbol(goldClass),
                        goldClass, instancesByClass.getInt(goldClass)));

                for (short predictedClass = 0; predictedClass < confusionMatrix.columns(); predictedClass++) {
                    final int errors = confusionMatrix.getInt(goldClass, predictedClass);

                    if (errors != 0) {
                        if (instancesByClass.getInt(goldClass) > 0) {
                            sb.append(String.format("    %-20s (Class %5d)    Errors: %d\n",
                                    tagSet.getSymbol(predictedClass), predictedClass, errors));
                        }
                    }
                }
            }
            return sb.toString();
        }

        @Override
        public String toString() {
            return String.format("Accuracy=%.2f", accuracy() * 100f);
        }
    }

    protected static class Model extends ClassifierTool.Model {

        private static final long serialVersionUID = 1L;

        final SymbolSet<String> tagSet;

        private final Long2IntOpenHashMap parallelArrayOffsetMap;
        private final short[] parallelWeightArrayTags;
        private final float[] parallelWeightArray;

        protected Model(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet,
                final Long2IntOpenHashMap parallelArrayOffsetMap, final short[] parallelWeightArrayTags,
                final float[] parallelWeightArray) {

            super(featureTemplates, lexicon, unkClassSet);
            this.tagSet = tagSet;
            this.parallelArrayOffsetMap = parallelArrayOffsetMap;
            this.parallelWeightArrayTags = parallelWeightArrayTags;
            this.parallelWeightArray = parallelWeightArray;
        }
    }

    public static class LineIterator implements Iterable<String> {

        private final BufferedReader bufferedReader;

        public LineIterator(final Reader r) {
            this.bufferedReader = new BufferedReader(r);
        }

        public LineIterator(final InputStream is) {
            this.bufferedReader = new BufferedReader(new InputStreamReader(is));
        }

        @Override
        public Iterator<String> iterator() {

            return new Iterator<String>() {

                private String next;

                {
                    try {
                        next = bufferedReader.readLine();
                    } catch (final IOException e) {
                        // next remains null, and hasNext() will return false
                    }
                }

                @Override
                public boolean hasNext() {
                    return next != null;
                }

                @Override
                public String next() {
                    try {
                        final String s = next;
                        next = bufferedReader.readLine();
                        return s;
                    } catch (final IOException e) {
                        throw new NoSuchElementException();
                    }
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }
    }
}
