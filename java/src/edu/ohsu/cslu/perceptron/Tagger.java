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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.SymbolSet;
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
    private final static String DEFAULT_FEATURE_TEMPLATES = "wm2,wm1,w,wp1,wp2,um2,um1,u,up1,up2,tm3,tm2,tm1,wm2_wm1,wm1_w,w_wp1,wp1_wp2,wm2_um1,wm1_u,u_wp1,up1_wp2,wm2_wm1_w,wm1_w_wp1,w_wp1_wp2,tm3_tm2,tm2_tm1,tm1_wm1,tm1_w,wm2_tm1";

    @Option(name = "-ti", metaVar = "iterations", usage = "Train the tagger for n iterations (Optionally tests on dev-set with '-ds' and outputs a model with '-m')")
    int trainingIterations = 0;

    @Option(name = "-ft", requires = "-ti", metaVar = "templates or file", usage = "Feature templates (comma-delimited), or template file")
    private String featureTemplates = DEFAULT_FEATURE_TEMPLATES;

    @Option(name = "-d", requires = "-ti", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    private File devSet;

    @Option(name = "-m", metaVar = "file", usage = "Model file (Java Serialized Object)")
    private File modelFile;

    final static String NULL_SYMBOL = "<null>";

    private SymbolSet<String> lexicon;
    private SymbolSet<String> unkClassSet;
    private SymbolSet<String> tagSet;
    private AveragedPerceptron model;

    @Override
    protected void run() throws Exception {
        if (trainingIterations > 0) {
            train(inputAsBufferedReader());
        } else {
            readModel(modelFile);
            tag(inputAsBufferedReader());
        }
    }

    protected int[] tag(final BufferedReader inputReader) throws IOException {
        int sentences = 0, words = 0, correct = 0;
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor(featureTemplates, lexicon, unkClassSet, tagSet);

        for (final String line : inputLines(inputReader)) {
            sentences++;
            final TagSequence tagSequence = new TagSequence(line, lexicon, unkClassSet, tagSet);
            final StringBuilder sb = new StringBuilder(line.length() * 2);

            for (int j = 0; j < tagSequence.length; j++) {
                tagSequence.predictedTags[j] = (short) model.classify(fe.forwardFeatureVector(tagSequence, j));
                sb.append("(" + tagSet.getSymbol(tagSequence.predictedTags[j]) + " " + tagSequence.tokens[j] + ")");
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
     * Reads in a serialized tagging model from disk, including the template string, lexicon, trained perceptron, etc.
     * Separate from {@link #tag()} for unit testing of {@link #train()} and {@link #tag()}.
     * 
     * @throws IOException
     * @throws ClassNotFoundException
     */
    @SuppressWarnings("unchecked")
    private void readModel(final File f) throws IOException, ClassNotFoundException {
        final ObjectInputStream ois = new ObjectInputStream(fileAsInputStream(f));
        featureTemplates = (String) ois.readObject();
        lexicon = (SymbolSet<String>) ois.readObject();
        unkClassSet = (SymbolSet<String>) ois.readObject();
        tagSet = (SymbolSet<String>) ois.readObject();
        model = (AveragedPerceptron) ois.readObject();
        ois.close();
        model.trim();
    }

    protected void train(final BufferedReader inputReader) throws IOException {

        final long startTime = System.currentTimeMillis();
        final ArrayList<TagSequence> trainingCorpusSequences = new ArrayList<Tagger.TagSequence>();
        final ArrayList<BitVector[]> trainingCorpusFeatures = new ArrayList<BitVector[]>();

        final ArrayList<TagSequence> devCorpusSequences = new ArrayList<Tagger.TagSequence>();
        final ArrayList<BitVector[]> devCorpusFeatures = new ArrayList<BitVector[]>();

        lexicon = new SymbolSet<String>();
        lexicon.defaultReturnValue(NULL_SYMBOL);

        unkClassSet = new SymbolSet<String>();
        unkClassSet.defaultReturnValue(NULL_SYMBOL);

        tagSet = new SymbolSet<String>();
        tagSet.defaultReturnValue(NULL_SYMBOL);

        //
        // Read in the training corpus and map each token
        //
        for (final String line : inputLines(inputReader)) {
            trainingCorpusSequences.add(new TagSequence(line, lexicon, unkClassSet, tagSet));
        }
        lexicon.finalize();
        unkClassSet.finalize();
        tagSet.finalize();

        // Read in a feature file if provided
        final File f = new File(featureTemplates);
        if (f.exists()) {
            featureTemplates = readFeatureTemplateFile(f);
        }

        //
        // Pre-compute all features
        //
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor(featureTemplates, lexicon, unkClassSet, tagSet);
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
                final TagSequence tagSequence = new TagSequence(line, lexicon, unkClassSet, tagSet);
                devCorpusSequences.add(tagSequence);

                final BitVector[] featureVectors = new BitVector[tagSequence.tags.length];
                for (int j = 0; j < featureVectors.length; j++) {
                    featureVectors[j] = fe.forwardFeatureVector(tagSequence, j);
                }
                devCorpusFeatures.add(featureVectors);
            }
        }

        model = new AveragedPerceptron(tagSet.size(), fe.featureCount());

        //
        // Iterate over training corpus
        //
        for (int i = 1; i <= trainingIterations; i++) {
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

            if (!devCorpusSequences.isEmpty()) {
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
                System.out.format("Iteration=%d Devset Accuracy=%.2f  Time=%d\n", i, correct * 100f / total,
                        System.currentTimeMillis() - t0);
            }
        }

        // Write out the model file
        if (modelFile != null) {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(modelFile));
            oos.writeObject(featureTemplates);
            oos.writeObject(lexicon);
            oos.writeObject(unkClassSet);
            oos.writeObject(tagSet);
            oos.writeObject(model);
            oos.close();
        }

        System.out.format("Time: %d seconds\n", (System.currentTimeMillis() - startTime) / 1000);
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
        final SymbolSet<String> lexicon;
        final SymbolSet<String> unkClassSet;
        final SymbolSet<String> tagSet;

        /**
         * Constructor for tagged training sequence
         * 
         * @param mappedTokens
         * @param tags
         */
        public TagSequence(final String[] tokens, final int[] mappedTokens, final short[] tags,
                final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet) {

            this.tokens = tokens;
            this.mappedTokens = mappedTokens;
            this.tags = tags != null ? tags : new short[tokens.length];
            this.predictedTags = new short[tokens.length];
            this.length = mappedTokens.length;
            this.lexicon = lexicon;
            this.unkClassSet = unkClassSet;
            this.tagSet = tagSet;

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
                final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet) {
            this(tokens, mappedTokens, null, lexicon, unkClassSet, tagSet);
        }

        public TagSequence(final String sentence, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
                final SymbolSet<String> tagSet) {
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

                    if (tagSet.isFinalized()) {
                        tags[i] = (short) tagSet.getIndex(tokenAndPos[0]);
                    } else {
                        tags[i] = (short) tagSet.addSymbol(tokenAndPos[0]);
                    }
                    predictedTags[i] = tags[i];
                    tokens[i] = tokenAndPos[1];

                    if (lexicon.isFinalized()) {
                        mappedTokens[i] = lexicon.getIndex(tokenAndPos[1]);
                        mappedUnkSymbols[i] = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                                lexicon));
                    } else {
                        mappedTokens[i] = lexicon.addSymbol(tokenAndPos[1]);
                        mappedUnkSymbols[i] = unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                                lexicon));
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
                    if (lexicon.isFinalized()) {
                        mappedTokens[i] = lexicon.getIndex(split[i]);
                        mappedUnkSymbols[i] = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                                lexicon));
                    } else {
                        mappedTokens[i] = lexicon.addSymbol(split[i]);
                        mappedUnkSymbols[i] = unkClassSet.addSymbol(Tokenizer.berkeleyGetSignature(tokens[i], i == 0,
                                lexicon));
                    }
                }
            }
            this.lexicon = lexicon;
            this.unkClassSet = unkClassSet;
            this.tagSet = tagSet;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(256);
            for (int i = 0; i < length; i++) {
                sb.append('(');
                sb.append(tagSet.getSymbol(tags[i]));
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
     * 
     * We support:
     * 
     * <pre>
     * Tag(i-3): tm3 
     * Tag(i-2): tm2 
     * Previous tag: tm1 
     * 
     * Word(i-2): wm2
     * Previous word: wm1 
     * Word: w
     * Word(i+1): wp1
     * Word(i+2): wp2
     * 
     * 1-character suffix: s1 
     * 2-character suffix: s2
     * 3-character suffix: s3 
     * 4-character suffix: s4
     * 
     * 1-character prefix: p1 
     * 2-character prefix: p2 
     * 3-character prefix: p3 
     * 4-character prefix: p4
     * 
     * Word includes digit: d
     * </pre>
     * 
     * Default feature set (copied from Mahsa's tagger):
     * 
     * tm1_tm2,tm1_wm1,tm1_wm2,tm1_w,tm1_s1,tm1_s2,tm1_s3,tm1_s4,tm1_p1,tm1_p2,tm1_p3,tm1_p4,tm1_w_d
     */
    public static class TaggerFeatureExtractor extends FeatureExtractor<TagSequence> {

        private static final long serialVersionUID = 1L;

        final TemplateElement[][] templates;
        final long[] featureOffsets;

        final SymbolSet<String> lexicon;
        final SymbolSet<String> tags;

        final int nullToken, nullTag;
        final int lexiconSize, tagSetSize, unkClassSetSize;
        final long featureVectorLength;

        public TaggerFeatureExtractor(final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
                final SymbolSet<String> tagSet) {
            this(DEFAULT_FEATURE_TEMPLATES, lexicon, unkClassSet, tagSet);
        }

        /**
         * Constructs a {@link FeatureExtractor} using the specified feature templates
         * 
         * @param featureTemplates
         * @param lexicon
         * @param tagSet
         */
        public TaggerFeatureExtractor(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> tagSet) {

            this.lexicon = lexicon;
            this.lexiconSize = lexicon.size();
            this.nullToken = lexicon.getIndex(NULL_SYMBOL);

            this.tags = tagSet;
            this.tagSetSize = tagSet.size();
            this.unkClassSetSize = unkClassSet.size();
            this.nullTag = tagSet.getIndex(NULL_SYMBOL);

            final String[] templateStrings = featureTemplates.split(",");
            this.templates = new TemplateElement[templateStrings.length][];
            this.featureOffsets = new long[this.templates.length];

            for (int i = 0; i < featureOffsets.length; i++) {
                templates[i] = template(templateStrings[i]);
            }

            for (int i = 1; i < featureOffsets.length; i++) {
                featureOffsets[i] = featureOffsets[i - 1] + templateSize(templates[i - 1]);
                // Blow up if we wrap around Long.MAX_VALUE
                if (featureOffsets[i] < 0) {
                    throw new IllegalArgumentException("Feature set too large. Features limited to " + Long.MAX_VALUE);
                }
            }

            this.featureVectorLength = featureOffsets[featureOffsets.length - 1]
                    + templateSize(templates[templates.length - 1]);
            if (featureVectorLength < 0) {
                throw new IllegalArgumentException("Feature set too large. Features limited to " + Long.MAX_VALUE);
            }
        }

        private TemplateElement[] template(final String templateString) {
            final String[] split = templateString.split("_");
            final TemplateElement[] template = new TemplateElement[split.length];
            for (int i = 0; i < split.length; i++) {
                template[i] = TemplateElement.valueOf(TemplateElement.class, split[i]);
            }
            return template;
        }

        private long templateSize(final TemplateElement[] template) {
            long size = 1;
            for (int i = 0; i < template.length; i++) {
                switch (template[i]) {

                case tm1:
                case tm2:
                case tm3:
                    size *= tagSetSize;
                    break;

                case um2:
                case um1:
                case u:
                case up1:
                case up2:
                    size *= unkClassSetSize;
                    break;

                case wm2:
                case wm1:
                case w:
                case wp1:
                case wp2:
                    size *= lexiconSize;
                    break;
                }

                if (size < 0) {
                    throw new IllegalArgumentException("Feature set too large. Features limited to " + Long.MAX_VALUE);
                }
            }
            return size;
        }

        @Override
        public long featureCount() {
            return featureVectorLength;
        }

        @Override
        public BitVector forwardFeatureVector(final TagSequence sequence, final int tokenIndex) {

            final LongArrayList featureIndices = new LongArrayList();

            for (int i = 0; i < templates.length; i++) {
                long feature = 0;
                final TemplateElement[] template = templates[i];
                for (int j = 0; j < template.length; j++) {
                    final TemplateElement t = template[j];
                    final int index = tokenIndex + t.offset;

                    switch (t) {

                    case tm1:
                    case tm2:
                    case tm3:
                        feature *= tagSetSize;
                        feature += ((index < 0 || index >= sequence.predictedTags.length) ? nullTag
                                : sequence.predictedTags[index]);
                        break;

                    case um2:
                    case um1:
                    case u:
                    case up1:
                    case up2:
                        feature *= unkClassSetSize;
                        feature += ((index < 0 || index >= sequence.tokens.length) ? nullToken
                                : sequence.mappedUnkSymbols[index]);
                        break;

                    case wm2:
                    case wm1:
                    case w:
                    case wp1:
                    case wp2:
                        feature *= lexiconSize;
                        feature += ((index < 0 || index >= sequence.tokens.length) ? nullToken
                                : sequence.mappedTokens[index]);
                        break;
                    }
                }
                final long featureIndex = featureOffsets[i] + feature;
                assert featureIndex >= 0 && featureIndex < featureVectorLength;
                featureIndices.add(featureIndex);
            }

            return featureVectorLength > Integer.MAX_VALUE ? new LargeSparseBitVector(featureVectorLength,
                    featureIndices.toLongArray()) : new SparseBitVector(featureVectorLength,
                    featureIndices.toLongArray());
        }

        @Override
        public Vector forwardFeatureVector(final TagSequence source, final int tokenIndex, final float[] tagScores) {
            return null;
        }

        private enum TemplateElement {
            tm3(-3), // Tag i-3
            tm2(-2), // Tag i-2
            tm1(-1), // Previous tag

            wm2(-2), // Word i-2
            wm1(-1), // Previous word (i-1)
            w(0), // Word
            wp1(1), // Next word (i+1)
            wp2(2), // Word i+2

            um2(-2), // Unknown word token i-2
            um1(-1), // Unknown word token (i-1)
            u(0), // Unknown word token
            up1(1), // Unknown word token (i+1)
            up2(2); // Unknown word token i+2

            final int offset;

            private TemplateElement(final int offset) {
                this.offset = offset;
            }
        }
    }
}