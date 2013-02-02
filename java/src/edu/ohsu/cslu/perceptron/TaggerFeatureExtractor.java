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
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.Tagger.TagSequence;

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
public class TaggerFeatureExtractor extends FeatureExtractor<TagSequence> {

    private static final long serialVersionUID = 1L;

    final TaggerFeatureExtractor.TemplateElement[][] templates;
    final long[] featureOffsets;

    final SymbolSet<String> lexicon;
    final SymbolSet<String> tags;

    final int nullToken, nullTag;
    final int lexiconSize, tagSetSize, unkClassSetSize;
    final long featureVectorLength;

    public TaggerFeatureExtractor(final TaggerModel model) {
        this(Tagger.DEFAULT_FEATURE_TEMPLATES, model);
    }

    /**
     * Constructs a {@link FeatureExtractor} using the specified feature templates
     * 
     * @param featureTemplates
     * @param model
     */
    public TaggerFeatureExtractor(final String featureTemplates, final TaggerModel model) {

        this.lexicon = model.lexicon;
        this.lexiconSize = lexicon.size();
        this.nullToken = lexicon.getIndex(Tagger.NULL_SYMBOL);

        this.tags = model.tagSet;
        this.tagSetSize = tags.size();
        this.unkClassSetSize = model.unkClassSet.size();
        this.nullTag = tags.getIndex(Tagger.NULL_SYMBOL);

        final String[] templateStrings = featureTemplates.split(",");
        this.templates = new TaggerFeatureExtractor.TemplateElement[templateStrings.length][];
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

    private TaggerFeatureExtractor.TemplateElement[] template(final String templateString) {
        final String[] split = templateString.split("_");
        final TaggerFeatureExtractor.TemplateElement[] template = new TaggerFeatureExtractor.TemplateElement[split.length];
        for (int i = 0; i < split.length; i++) {
            template[i] = TemplateElement.valueOf(TaggerFeatureExtractor.TemplateElement.class, split[i]);
        }
        return template;
    }

    private long templateSize(final TaggerFeatureExtractor.TemplateElement[] template) {
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
            final TaggerFeatureExtractor.TemplateElement[] template = templates[i];
            for (int j = 0; j < template.length; j++) {
                final TaggerFeatureExtractor.TemplateElement t = template[j];
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
                featureIndices.toLongArray()) : new SparseBitVector(featureVectorLength, featureIndices.toLongArray());
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