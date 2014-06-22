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

import java.io.BufferedReader;
import java.io.FileInputStream;

import cltool4j.BaseLogger;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.util.Strings;

/**
 * A multiclass sequence tagger
 * 
 * Training input: Gold Trees in standard bracketed format or tagged tokens, one sentence per line (format: '(tag token)
 * (tag token) ...').
 * 
 * Test/inference input: Untagged text, one sentence per line
 * 
 * TODO In classification mode, output tagged sequences
 * 
 * @author Aaron Dunlop
 * @since Jul 11, 2013
 */
public class Tagger extends MulticlassClassifier<MulticlassTagSequence, MulticlassTaggerFeatureExtractor, String> {

    private static final long serialVersionUID = 2L;

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
    public static final String DEFAULT_FEATURE_TEMPLATES = "wm2,wm1,w,wp1,wp2,um2,um1,u,up1,up2,tm3,tm2,tm1,wm2_wm1,wm1_w,w_wp1,wp1_wp2,wm2_um1,wm1_u,u_wp1,up1_wp2,wm2_wm1_w,wm1_w_wp1,w_wp1_wp2,tm3_tm2,tm2_tm1,tm1_wm1,tm1_w,wm2_tm1";

    /**
     * POS tagset (used by some sequence taggers which depend on the output a previous POS-tagging stage). Null
     * otherwise
     */
    SymbolSet<String> posSet;

    @Override
    protected String DEFAULT_FEATURE_TEMPLATES() {
        return DEFAULT_FEATURE_TEMPLATES;
    }

    /**
     * Default constructor
     */
    public Tagger() {
        super();
    }

    /**
     * Constructor for use in embedded training (e.g. when jointly training a POS tagger and cell-classifier in
     * {@link CompleteClosureClassifier}).
     */
    public Tagger(final String featureTemplates, final SymbolSet<String> lexicon, final SymbolSet<String> unkClassSet,
            final SymbolSet<String> tagSet) {
        this.featureTemplates = featureTemplates;
        this.lexicon = lexicon;
        this.decisionTreeUnkClassSet = unkClassSet;
        this.tagSet = tagSet;
    }

    @Override
    protected void run() throws Exception {

        final BufferedReader input = inputAsBufferedReader();

        if (trainingIterations > 0) {

            // Cross-validation (which doesn't output a model) or regular training
            if (crossValidationFolds > 0) {
                final MulticlassClassifierResult totalResult = crossValidate(input);
                BaseLogger.singleton().info(totalResult.toString());
            } else {
                train(input);
            }

        } else {
            readModel(new FileInputStream(modelFile));

            input.mark(8);
            final char firstChar = (char) input.read();
            input.reset();

            // If the first input character is a '(', assume the input includes gold tags and evaluate accuracy only.
            if (firstChar == '(') {
                final MulticlassClassifierResult result = testAccuracy(corpusReader(input));
                BaseLogger.singleton().info(
                        String.format("Accuracy=%.2f  Time=%d\n", result.accuracy() * 100f, result.time()));
            } else {

                // Output tagged text.
                for (final String sentence : corpusReader(input)) {
                    final String tokenizedSentence = Tokenizer.treebankTokenize(sentence);

                    // TODO This is a little inefficient, as MulticlassTagSequence duplicates the string splitting we do
                    // here
                    final MulticlassTagSequence sequence = createSequence(tokenizedSentence);
                    final String[] tokens = Strings.splitOnSpace(tokenizedSentence);
                    final short[] tags = classify(sequence);

                    final StringBuilder sb = new StringBuilder(sentence.length() * 2);
                    for (int i = 0; i < tokens.length; i++) {
                        sb.append('(').append(tagSet.getSymbol(tags[i])).append(' ').append(tokens[i]).append(')')
                                .append(' ');
                    }
                    sb.deleteCharAt(sb.length() - 1);
                    System.out.println(sb.toString());
                }
            }
        }
    }

    @Override
    protected MulticlassTaggerFeatureExtractor featureExtractor() {
        return new MulticlassTaggerFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, tagSet);
    }

    @Override
    protected MulticlassTagSequence createSequence(final String line) {
        return new MulticlassTagSequence(line, this);
    }

    protected void initFromModel(final Model tmp) {
        super.initFromModel(tmp);
        this.posSet = tmp.posSet;
    }

    @Override
    protected Model model() {
        return new Model(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, tagSet, parallelArrayOffsetMap,
                parallelWeightArrayTags, parallelWeightArray);
    }

    @Override
    protected void finalizeMaps() {
        super.finalizeMaps();
        if (posSet != null) {
            posSet.finalize();
        }
    }

    public static void main(final String[] args) {
        run(args);
    }

    protected static class Model extends MulticlassClassifier.Model {

        private static final long serialVersionUID = 1L;

        final SymbolSet<String> posSet;

        protected Model(final String featureTemplates, final SymbolSet<String> lexicon,
                final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet, final SymbolSet<String> tagSet,
                final Long2IntOpenHashMap parallelArrayOffsetMap, final short[] parallelWeightArrayTags,
                final float[] parallelWeightArray) {

            super(featureTemplates, lexicon, unkClassSet, tagSet, parallelArrayOffsetMap, parallelWeightArrayTags,
                    parallelWeightArray);
            this.posSet = posSet;
        }
    }
}
