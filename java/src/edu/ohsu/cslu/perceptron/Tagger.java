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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.grammar.Tokenizer;

/**
 * Trains an averaged-perceptron part-of-speech tagger.
 * 
 * Input: Tagged tokens, one sentence per line. Format: '(tag token) (tag token) ...'
 * 
 * TODO Handle tree input so we can train directly on a treebank
 * 
 * @author Aaron Dunlop
 * @since May 21, 2012
 */
public class Tagger extends BaseCommandlineTool {

    /**
     * Default Features:
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
    final static String DEFAULT_FEATURE_TEMPLATES = "wm2,wm1,w,wp1,wp2,um2,um1,u,up1,up2,tm3,tm2,tm1,wm2_wm1,wm1_w,w_wp1,wp1_wp2,wm2_um1,wm1_u,u_wp1,up1_wp2,wm2_wm1_w,wm1_w_wp1,w_wp1_wp2,tm3_tm2,tm2_tm1,tm1_wm1,tm1_w,wm2_tm1";

    @Option(name = "-ti", metaVar = "iterations", usage = "Train the tagger for n iterations (Optionally tests on dev-set with '-ds' and outputs a model with '-m')")
    int trainingIterations = 0;

    @Option(name = "-ft", requires = "-ti", metaVar = "templates or file", usage = "Feature templates (comma-delimited), or template file")
    private String featureTemplates = DEFAULT_FEATURE_TEMPLATES;

    @Option(name = "-d", requires = "-ti", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    private File devSet;

    @Option(name = "-m", metaVar = "file", usage = "Model file (Java Serialized Object)")
    private File modelFile;

    final static String NULL_SYMBOL = "<null>";

    private TaggerModel model;

    @Override
    protected void run() throws Exception {
        if (trainingIterations > 0) {
            train(inputAsBufferedReader());
        } else {
            final FileInputStream is = new FileInputStream(modelFile);
            model = TaggerModel.read(is);
            is.close();

            tag(inputAsBufferedReader());
        }
    }

    /**
     * Tags the input sequences read from <code>input</code>
     * 
     * @param input
     * @return an array containing: sentences, words, and correct tags (if the input sequences include gold tags)
     * 
     * @throws IOException if a read fails
     */
    protected int[] tag(final BufferedReader input) throws IOException {
        int sentences = 0, words = 0, correct = 0;
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor(featureTemplates, model);

        for (final String line : inputLines(input)) {
            sentences++;
            final TagSequence tagSequence = new TagSequence(line, model);
            final StringBuilder sb = new StringBuilder(line.length() * 2);

            for (int j = 0; j < tagSequence.length; j++) {
                tagSequence.predictedTags[j] = (short) model.classify(fe.forwardFeatureVector(tagSequence, j));
                sb.append("(" + model.tagSet.getSymbol(tagSequence.predictedTags[j]) + " " + tagSequence.tokens[j]
                        + ")");
                if (j < tagSequence.length - 1) {
                    sb.append(' ');
                }
                if (tagSequence.predictedTags[j] == tagSequence.tags[j]) {
                    correct++;
                }
                words++;
            }
        }
        return new int[] { sentences, words, correct };
    }

    /**
     * Trains a {@link TaggerModel}, optionally validating it on {@link #devSet} and writing it to {@link #modelFile} as
     * a Java serialized object.
     * 
     * @param input
     * @throws IOException
     */
    protected void train(final BufferedReader input) throws IOException {

        final long startTime = System.currentTimeMillis();
        final ArrayList<TagSequence> trainingCorpusSequences = new ArrayList<Tagger.TagSequence>();
        final ArrayList<BitVector[]> trainingCorpusFeatures = new ArrayList<BitVector[]>();

        final ArrayList<TagSequence> devCorpusSequences = new ArrayList<Tagger.TagSequence>();
        final ArrayList<BitVector[]> devCorpusFeatures = new ArrayList<BitVector[]>();

        // Read in a feature file if provided
        final File f = new File(featureTemplates);
        if (f.exists()) {
            featureTemplates = readFeatureTemplateFile(f);
        }
        model = new TaggerModel(featureTemplates);

        //
        // Read in the training corpus and map each token
        //
        for (final String line : inputLines(input)) {
            trainingCorpusSequences.add(new TagSequence(line, model));
        }
        model.finalizeMaps();

        //
        // Pre-compute all features
        //
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor(featureTemplates, model);
        for (final TagSequence tagSequence : trainingCorpusSequences) {

            final BitVector[] featureVectors = new BitVector[tagSequence.tags.length];
            for (int i = 0; i < featureVectors.length; i++) {
                featureVectors[i] = fe.forwardFeatureVector(tagSequence, i);
            }
            trainingCorpusFeatures.add(featureVectors);
        }

        // Read in the dev set and map features
        if (devSet != null) {
            for (final String line : fileLines(devSet)) {
                final TagSequence tagSequence = new TagSequence(line, model);
                devCorpusSequences.add(tagSequence);

                final BitVector[] featureVectors = new BitVector[tagSequence.tags.length];
                for (int j = 0; j < featureVectors.length; j++) {
                    featureVectors[j] = fe.forwardFeatureVector(tagSequence, j);
                }
                devCorpusFeatures.add(featureVectors);
            }
        }

        model.createPerceptronModel(fe.featureCount());

        //
        // Iterate over training corpus, training the model
        //
        for (int i = 1; i <= trainingIterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final TagSequence tagSequence = trainingCorpusSequences.get(j);

                final BitVector[] featureVectors = trainingCorpusFeatures.get(j);
                for (int k = 0; k < featureVectors.length; k++) {
                    model.train(tagSequence.tags[k], featureVectors[k]);
                }

                progressBar(100, 5000, j);
            }

            // Skip the last iteration - we'll test after we finalize below
            if (!devCorpusSequences.isEmpty() && i < trainingIterations) {
                testDevelopmentSet(i, devCorpusSequences, devCorpusFeatures);
            }
        }

        // Store the trained model in a memory- and cache-efficient format for tagging (we do this even if we're not
        // writing out the serialized model, specifically so we can unit test train() and tag())
        // model.finalizeModel();
        if (!devCorpusSequences.isEmpty()) {
            testDevelopmentSet(trainingIterations, devCorpusSequences, devCorpusFeatures);
        }

        // Write out the model file
        if (modelFile != null) {
            // And write it to disk
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile));
            oos.writeObject(model);
            oos.close();
        }

        BaseLogger.singleton().info(
                String.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000));
    }

    private void testDevelopmentSet(final int trainingIteration, final ArrayList<TagSequence> devCorpusSequences,
            final ArrayList<BitVector[]> devCorpusFeatures) {
        // Test the development set
        int total = 0, correct = 0;
        final long t0 = System.currentTimeMillis();
        for (int j = 0; j < devCorpusFeatures.size(); j++) {
            final TagSequence tagSequence = devCorpusSequences.get(j);
            final BitVector[] featureVectors = devCorpusFeatures.get(j);

            for (int k = 0; k < featureVectors.length; k++) {
                tagSequence.predictedTags[k] = (short) model.classify(featureVectors[k]);
                if (tagSequence.predictedTags[k] == tagSequence.tags[k]) {
                    correct++;
                }
                total++;
            }
            Arrays.fill(tagSequence.predictedTags, (short) 0);
        }
        BaseLogger.singleton().info(
                String.format("Iteration=%d Devset Accuracy=%.2f  Time=%d\n", trainingIteration,
                        correct * 100f / total, System.currentTimeMillis() - t0));
    }

    /**
     * Reads a feature template file and returns a comma-delimited sequence of feature templates. The file format
     * ignores whitespace and uses # to denote comments. E.g.:
     * 
     * <pre>
     * wm2,wm1,w,wp1,wp2 # Unigram word features
     * 
     * tm1_tm2 # Tag i-2,i-1
     * tm1_wm1 # Tag i-1, word i-1
     * tm1_wm2 # Tag i-1, word i-2
     * tm1_w   # Tag i-1, word
     * 
     * tm1_w_d # Tag i-1, word, word-contains-digit
     * </pre>
     * 
     * @param f
     * @return Comma-delimited sequence of feature templates
     * @throws IOException
     */
    private String readFeatureTemplateFile(final File f) throws IOException {
        final StringBuilder sb = new StringBuilder();
        for (final String line : fileLines(f)) {
            final String uncommented = line.split("#")[0].trim();
            if (uncommented.length() > 0) {
                sb.append(uncommented);
                sb.append(',');
            }
        }
        // Remove the trailing ','
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

    public static void main(final String[] args) {
        run(args);
    }

    /**
     * Represents a sequence of (possibly-tagged) tokens.
     */
    public static class TagSequence {

        final int[] mappedTokens;
        final int[] mappedUnkSymbols;
        final String[] tokens;
        final short[] tags;
        final short[] predictedTags;
        final int length;
        final TaggerModel model;

        /**
         * Constructor for tagged training sequence
         * 
         * @param mappedTokens
         * @param tags
         */
        public TagSequence(final String[] tokens, final int[] mappedTokens, final short[] tags, final TaggerModel model) {

            this.tokens = tokens;
            this.mappedTokens = mappedTokens;
            this.tags = tags != null ? tags : new short[tokens.length];
            this.predictedTags = new short[tokens.length];
            this.length = mappedTokens.length;
            this.model = model;

            this.mappedUnkSymbols = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                mappedUnkSymbols[i] = model.lexicon.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                        model.lexicon));
            }
        }

        /**
         * Constructor for untagged test sequence
         * 
         * @param tokens
         */
        public TagSequence(final String[] tokens, final int[] mappedTokens, final TaggerModel model) {
            this(tokens, mappedTokens, null, model);
        }

        public TagSequence(final String sentence, final TaggerModel model) {
            // If the sentence starts with '(', treat it as a tagged sequence
            if (sentence.startsWith("(")) {
                final String[] split = sentence.replaceAll(" ?\\(", "").split("\\)");
                this.tokens = new String[split.length];
                this.mappedTokens = new int[split.length];
                this.mappedUnkSymbols = new int[split.length];
                this.tags = new short[split.length];
                this.predictedTags = new short[split.length];
                this.length = split.length;

                for (int i = 0; i < split.length; i++) {
                    final String[] tokenAndPos = split[i].split(" ");

                    if (model.tagSet.isFinalized()) {
                        tags[i] = (short) model.tagSet.getIndex(tokenAndPos[0]);
                    } else {
                        tags[i] = (short) model.tagSet.addSymbol(tokenAndPos[0]);
                    }
                    predictedTags[i] = tags[i];
                    tokens[i] = tokenAndPos[1];

                    if (model.lexicon.isFinalized()) {
                        mappedTokens[i] = model.lexicon.getIndex(tokenAndPos[1]);
                        mappedUnkSymbols[i] = model.unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(tokens[i],
                                i == 0, model.lexicon));
                    } else {
                        mappedTokens[i] = model.lexicon.addSymbol(tokenAndPos[1]);
                        mappedUnkSymbols[i] = model.unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i],
                                i == 0, model.lexicon));
                    }
                }

            } else {
                // Otherwise, assume it is untagged
                final String[] split = sentence.split(" ");
                this.tokens = new String[split.length];
                this.mappedTokens = new int[split.length];
                this.mappedUnkSymbols = new int[split.length];
                this.tags = new short[split.length];
                this.predictedTags = new short[split.length];
                Arrays.fill(tags, (short) -1);
                this.length = split.length;

                for (int i = 0; i < split.length; i++) {
                    tokens[i] = split[i];
                    if (model.lexicon.isFinalized()) {
                        mappedTokens[i] = model.lexicon.getIndex(split[i]);
                        mappedUnkSymbols[i] = model.unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(tokens[i],
                                i == 0, model.lexicon));
                    } else {
                        mappedTokens[i] = model.lexicon.addSymbol(split[i]);
                        mappedUnkSymbols[i] = model.unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i],
                                i == 0, model.lexicon));
                    }
                }
            }
            this.model = model;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < length; i++) {
                sb.append('(');
                sb.append(model.tagSet.getSymbol(tags[i]));
                sb.append(' ');
                sb.append(model.lexicon.getSymbol(mappedTokens[i]));
                sb.append(')');

                if (i < (length - 1)) {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }
    }
}