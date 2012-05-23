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

import it.unimi.dsi.fastutil.longs.LongArrayList;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;

/**
 * Trains an averaged-perceptron part-of-speech tagger.
 * 
 * Input: POS-tagged tokens, one sentence per line. Format: '(POS token) (POS token) ...'
 * 
 * @author Aaron Dunlop
 * @since May 21, 2012
 */
public class TrainPosTagger extends BaseCommandlineTool {

    @Option(name = "-i", aliases = { "--iterations" }, metaVar = "count", usage = "Iterations over training corpus")
    private int iterations = 10;

    @Option(name = "-a", aliases = { "--alpha" }, metaVar = "value", usage = "Update step size (alpha)")
    private float alpha = 0.1f;

    @Option(name = "-d", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    private File devSet;

    @Option(name = "-s", metaVar = "file", usage = "Output model file (Java Serialized Object)")
    private File outputModelFile;

    private final static String NULL_TOKEN = "<null>";

    @Override
    protected void run() throws Exception {

        final long startTime = System.currentTimeMillis();
        final ArrayList<TagSequence> trainingCorpusSequences = new ArrayList<TrainPosTagger.TagSequence>();
        final ArrayList<BitVector[]> trainingCorpusFeatures = new ArrayList<BitVector[]>();

        final SymbolSet<String> lexicon = new SymbolSet<String>();
        lexicon.addSymbol(NULL_TOKEN);
        final SymbolSet<String> posSet = new SymbolSet<String>();
        posSet.addSymbol(NULL_TOKEN);

        //
        // Read in the training corpus and map each token
        //
        for (final String line : inputLines()) {

            final TagSequence tagSequence = new TagSequence(line, lexicon, posSet);
            trainingCorpusSequences.add(tagSequence);
        }

        //
        // Pre-compute all features
        //
        final PosFeatureExtractor fe = new PosFeatureExtractor(lexicon, posSet);
        for (final TagSequence tagSequence : trainingCorpusSequences) {

            final BitVector[] featureVectors = new BitVector[tagSequence.tags.length];
            for (int i = 0; i < featureVectors.length; i++) {
                featureVectors[i] = (BitVector) fe.forwardFeatureVector(tagSequence, i);
            }
            trainingCorpusFeatures.add(featureVectors);
        }

        final AveragedPerceptron model = new AveragedPerceptron(posSet.size(), fe.featureCount());

        //
        // Iterate over training corpus
        //
        for (int i = 1; i <= iterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final TagSequence tagSequence = trainingCorpusSequences.get(j);
                final BitVector[] featureVectors = trainingCorpusFeatures.get(j);

                for (int k = 0; k < featureVectors.length; k++) {
                    model.train(tagSequence.tags[k], featureVectors[k]);
                }
                if (j > 0 && j % 100 == 0) {
                    System.out.print('.');
                    if (j % 5000 == 0) {
                        System.out.println(j);
                    }
                }
            }

            if (devSet != null) {
                // Test the development set
                int total = 0, correct = 0;
                final BufferedReader br = fileAsBufferedReader(devSet);
                for (String line = br.readLine(); line != null; line = br.readLine()) {
                    final TagSequence tagSequence = new TagSequence(line, lexicon, posSet);

                    for (int k = 0; k < tagSequence.mappedTokens.length; k++) {
                        // final short predictedTag = (short) model.classify(model.rawWeights,
                        // fe.forwardFeatureVector(tagSequence, k));
                        final short predictedTag = (short) model.classify(fe.forwardFeatureVector(tagSequence, k));
                        if (predictedTag == tagSequence.tags[k]) {
                            correct++;
                        }
                        tagSequence.tags[k] = predictedTag;
                        total++;
                    }
                }
                System.out.format("Iteration=%d Devset Accuracy=%.2f\n", i, correct * 100f / total);
                br.close();
            }
        }

        // Write out the model file
        if (outputModelFile != null) {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModelFile));
            oos.writeObject(lexicon);
            oos.writeObject(posSet);
            oos.writeObject(model);
            oos.close();
        }

        System.out.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000);
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
        final int length;
        final SymbolSet<String> lexicon;
        final SymbolSet<String> posSet;

        /**
         * Constructor for tagged training sequence
         * 
         * @param mappedTokens
         * @param tags
         */
        public TagSequence(final String[] tokens, final int[] mappedTokens, final short[] tags,
                final SymbolSet<String> lexicon, final SymbolSet<String> posSet) {
            this.tokens = tokens;
            this.mappedTokens = mappedTokens;
            this.tags = tags != null ? tags : new short[tokens.length];
            this.length = mappedTokens.length;
            this.lexicon = lexicon;
            this.posSet = posSet;

            this.mappedUnkSymbols = new int[tokens.length];
            for (int i = 0; i < tokens.length; i++) {
                mappedUnkSymbols[i] = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0, lexicon));
            }
        }

        /**
         * Constructor for untagged test sequence
         * 
         * @param tokens
         */
        public TagSequence(final String[] tokens, final int[] mappedTokens, final SymbolSet<String> lexicon,
                final SymbolSet<String> posSet) {
            this(tokens, mappedTokens, null, lexicon, posSet);
        }

        public TagSequence(final String sentence, final SymbolSet<String> lexicon, final SymbolSet<String> posSet) {
            // If the sentence starts with '(', treat it as a tagged sequence
            if (sentence.startsWith("(")) {
                final String[] split = sentence.replaceAll(" ?\\(", "").split("\\)");
                this.tokens = new String[split.length];
                this.mappedTokens = new int[split.length];
                this.mappedUnkSymbols = new int[split.length];
                this.tags = new short[split.length];
                this.length = split.length;

                for (int i = 0; i < split.length; i++) {
                    final String[] tokenAndPos = split[i].split(" ");
                    tags[i] = (short) posSet.addSymbol(tokenAndPos[0]);
                    tokens[i] = tokenAndPos[1];
                    mappedTokens[i] = lexicon.addSymbol(tokenAndPos[1]);
                    mappedUnkSymbols[i] = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0, lexicon));
                }
            } else {
                // Otherwise, assume it is untagged
                final String[] split = sentence.split(" ");
                this.tokens = new String[split.length];
                this.mappedTokens = new int[split.length];
                this.mappedUnkSymbols = new int[split.length];
                this.tags = new short[split.length];
                Arrays.fill(tags, (short) -1);
                this.length = split.length;

                for (int i = 0; i < split.length; i++) {
                    mappedTokens[i] = lexicon.addSymbol(split[i]);
                    tokens[i] = split[i];
                    mappedUnkSymbols[i] = lexicon.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0, lexicon));
                }
            }
            this.lexicon = lexicon;
            this.posSet = posSet;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < length; i++) {
                sb.append('(');
                sb.append(posSet.getSymbol(tags[i]));
                sb.append(' ');
                sb.append(lexicon.getSymbol(mappedTokens[i]));
                sb.append(')');

                if (i < (length - 1)) {
                    sb.append(' ');
                }
            }
            return sb.toString();
        }
    }

    /**
     * Extracts features for POS tagging
     */
    public static class PosFeatureExtractor extends FeatureExtractor<TagSequence> {

        final SymbolSet<String> lexicon;
        final SymbolSet<String> posSet;

        final long featureCount;
        final long lexiconSize, posSetSize;

        // w token unigram
        // w-1 token unigram
        // w-2 token unigram
        // w+1 token unigram
        // w+2 token unigram
        //
        // w-2, w-1 token bigram
        // w-1, w token bigram
        // w, w+1 token bigram
        // w+1, w+2 token bigram
        //
        // t-1 unigram POS (all POS features are gold when training, 1-best when testing)
        // t-2 unigram POS
        //
        // t-2, t-1 POS bigram
        // t-3, t-2, t-1 POS trigram
        // t-1, w POS/token bigram

        final long w_unigramOffset, w1_unigramOffset, w2_unigramOffset, wp1_unigramOffset, wp2_unigramOffset;
        final long w2_w1_bigramOffset, w1_w_bigramOffset, w_wp1_bigramOffset, wp1_wp2_bigramOffset;
        final long t1_unigramOffset, t2_unigramOffset;
        final long t2_t1_bigramOffset, t3_t2_t1_trigramOffset, t1_w_bigramOffset, t2_t1_w_trigramOffset;

        final int NULL_TOKEN_INDEX, NULL_POS_INDEX;

        /**
         * @param lexicon
         * @param posSet
         */
        public PosFeatureExtractor(final SymbolSet<String> lexicon, final SymbolSet<String> posSet) {
            this.lexicon = lexicon;
            this.posSet = posSet;
            this.lexiconSize = lexicon.size();
            this.posSetSize = posSet.size();

            this.w_unigramOffset = 0;
            this.w1_unigramOffset = w_unigramOffset + lexiconSize;
            this.w2_unigramOffset = w1_unigramOffset + lexiconSize;
            this.wp1_unigramOffset = w2_unigramOffset + lexiconSize;
            this.wp2_unigramOffset = wp1_unigramOffset + lexiconSize;

            this.w2_w1_bigramOffset = wp2_unigramOffset + lexiconSize;
            this.w1_w_bigramOffset = w2_w1_bigramOffset + lexiconSize * lexiconSize;
            this.w_wp1_bigramOffset = w1_w_bigramOffset + lexiconSize * lexiconSize;
            this.wp1_wp2_bigramOffset = w_wp1_bigramOffset + lexiconSize * lexiconSize;

            this.t1_unigramOffset = wp1_wp2_bigramOffset + lexiconSize * lexiconSize;
            this.t2_unigramOffset = t1_unigramOffset + posSetSize;

            this.t2_t1_bigramOffset = t2_unigramOffset + posSetSize;
            this.t3_t2_t1_trigramOffset = t2_t1_bigramOffset + posSetSize * posSetSize;
            this.t1_w_bigramOffset = t2_t1_bigramOffset + posSetSize * posSetSize * posSetSize;
            this.t2_t1_w_trigramOffset = t1_w_bigramOffset + posSetSize * lexiconSize;

            this.featureCount = t2_t1_w_trigramOffset + posSetSize * posSetSize * lexiconSize;

            this.NULL_TOKEN_INDEX = lexicon.getIndex(NULL_TOKEN);
            this.NULL_POS_INDEX = posSet.getIndex(NULL_TOKEN);
        }

        @Override
        public long featureCount() {
            return featureCount;
        }

        @Override
        public Vector forwardFeatureVector(final TagSequence source, final int tokenIndex) {

            final LongArrayList featureIndices = new LongArrayList();

            // Unigram features
            addUnigramFeatures(featureIndices, source, tokenIndex, w_unigramOffset);
            addUnigramFeatures(featureIndices, source, tokenIndex - 1, w1_unigramOffset);
            addUnigramFeatures(featureIndices, source, tokenIndex - 2, w2_unigramOffset);
            addUnigramFeatures(featureIndices, source, tokenIndex + 1, wp1_unigramOffset);
            addUnigramFeatures(featureIndices, source, tokenIndex + 2, wp2_unigramOffset);

            // Token bigram features
            if (tokenIndex == 0) {
                featureIndices.add(w2_w1_bigramOffset + NULL_TOKEN_INDEX * lexiconSize + NULL_TOKEN_INDEX);
            } else if (tokenIndex == 1) {
                featureIndices.add(w2_w1_bigramOffset + NULL_TOKEN_INDEX * lexiconSize
                        + source.mappedTokens[tokenIndex - 1]);
            } else {
                featureIndices.add(w2_w1_bigramOffset + source.mappedTokens[tokenIndex - 2] * lexiconSize
                        + source.mappedTokens[tokenIndex - 1]);
            }

            if (tokenIndex == 0) {
                featureIndices
                        .add(w1_w_bigramOffset + NULL_TOKEN_INDEX * lexiconSize + source.mappedTokens[tokenIndex]);
            } else {
                featureIndices.add(w1_w_bigramOffset + source.mappedTokens[tokenIndex - 1] * lexiconSize
                        + source.mappedTokens[tokenIndex]);
            }

            if (tokenIndex == source.length - 1) {
                featureIndices.add(w_wp1_bigramOffset + source.mappedTokens[tokenIndex] * lexiconSize
                        + NULL_TOKEN_INDEX);
            } else {
                featureIndices.add(w_wp1_bigramOffset + source.mappedTokens[tokenIndex] * lexiconSize
                        + source.mappedTokens[tokenIndex + 1]);
            }

            if (tokenIndex == source.length - 1) {
                featureIndices.add(wp1_wp2_bigramOffset + NULL_TOKEN_INDEX * lexiconSize + NULL_TOKEN_INDEX);
            } else if (tokenIndex == source.length - 2) {
                featureIndices.add(wp1_wp2_bigramOffset + source.mappedTokens[tokenIndex + 1] * lexiconSize
                        + NULL_TOKEN_INDEX);
            } else {
                featureIndices.add(wp1_wp2_bigramOffset + source.mappedTokens[tokenIndex + 1] * lexiconSize
                        + source.mappedTokens[tokenIndex + 2]);
            }

            // POS Unigram features
            if (tokenIndex == 0) {
                featureIndices.add(t1_unigramOffset + NULL_POS_INDEX);
            } else {
                featureIndices.add(t1_unigramOffset + source.tags[tokenIndex - 1]);
            }

            if (tokenIndex < 2) {
                featureIndices.add(t2_unigramOffset + NULL_POS_INDEX);
            } else {
                featureIndices.add(t2_unigramOffset + source.tags[tokenIndex - 2]);
            }

            // POS Bigram
            if (tokenIndex == 0) {
                featureIndices.add(t2_t1_bigramOffset + NULL_POS_INDEX * posSetSize + NULL_POS_INDEX);
            } else if (tokenIndex == 1) {
                featureIndices.add(t2_t1_bigramOffset + NULL_POS_INDEX * posSetSize + source.tags[tokenIndex - 1]);
            } else {
                featureIndices.add(t2_t1_bigramOffset + source.tags[tokenIndex - 2] * posSetSize
                        + source.tags[tokenIndex - 1]);
            }

            // POS Trigram
            if (tokenIndex == 0) {
                featureIndices.add(t3_t2_t1_trigramOffset + NULL_POS_INDEX * posSetSize * posSetSize + NULL_POS_INDEX
                        * posSetSize + NULL_POS_INDEX);
            } else if (tokenIndex == 1) {
                featureIndices.add(t3_t2_t1_trigramOffset + NULL_POS_INDEX * posSetSize * posSetSize + NULL_POS_INDEX
                        * posSetSize + source.tags[tokenIndex - 1]);
            } else if (tokenIndex == 2) {
                featureIndices.add(t3_t2_t1_trigramOffset + NULL_POS_INDEX * posSetSize * posSetSize
                        + source.tags[tokenIndex - 2] * posSetSize + source.tags[tokenIndex - 1]);
            } else {
                featureIndices.add(t3_t2_t1_trigramOffset + source.tags[tokenIndex - 3] * posSetSize * posSetSize
                        + source.tags[tokenIndex - 2] * posSetSize + source.tags[tokenIndex - 1]);
            }

            // Previous POS and current word
            if (tokenIndex == 0) {
                featureIndices.add(t1_w_bigramOffset + NULL_POS_INDEX * lexiconSize + NULL_TOKEN_INDEX);
            } else {
                featureIndices.add(t1_w_bigramOffset + source.tags[tokenIndex - 1] * lexiconSize
                        + source.mappedTokens[tokenIndex]);
            }

            // Previous 2 POS and current word
            if (tokenIndex == 0) {
                featureIndices.add(t1_w_bigramOffset + NULL_POS_INDEX * posSetSize * lexiconSize + NULL_POS_INDEX
                        * lexiconSize + NULL_TOKEN_INDEX);
            } else if (tokenIndex == 1) {
                featureIndices.add(t1_w_bigramOffset + NULL_POS_INDEX * posSetSize * lexiconSize
                        + source.tags[tokenIndex - 1] * lexiconSize + source.mappedTokens[tokenIndex]);
            } else {
                featureIndices.add(t1_w_bigramOffset + source.tags[tokenIndex - 2] * posSetSize * lexiconSize
                        + source.tags[tokenIndex - 1] * lexiconSize + source.mappedTokens[tokenIndex]);
            }

            return new LargeSparseBitVector(featureCount, featureIndices.toLongArray());
        }

        private void addUnigramFeatures(final LongArrayList featureIndices, final TagSequence source, final int index,
                final long offset) {

            if (index < 0 || index >= source.length) {
                featureIndices.add(offset + lexicon.getIndex(NULL_TOKEN));
            } else {
                featureIndices.add(offset + source.mappedTokens[index]);
                featureIndices.add(offset + source.mappedUnkSymbols[index]);
            }
        }

        @Override
        public Vector forwardFeatureVector(final TagSequence source, final int tokenIndex, final float[] tagScores) {
            return null;
        }

    }
}
