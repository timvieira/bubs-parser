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

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Extracts features for multiclass tagging (POS tagging in particular, but other classifications are supported as well)
 * 
 * We support:
 * 
 * <pre>
 * tm3    Tag i-3
 * tm2    Tag i-2
 * tm1    Previous tag
 * 
 * posm3  POS i-3
 * posm2  POS i-2
 * posm1  Previous POS
 * pos    Current POS
 * posp1  Next POS
 * posp2  POS i+2
 * posp3  POS i+3
 * 
 * wm2    Word i-2
 * wm1    Previous word (i-1)
 * w      Word
 * wp1    Next word (i+1)
 * wp2    Word i+2
 * 
 * um2    Unknown word token i-2
 * um1    Unknown word token (i-1)
 * u      Unknown word token
 * up1    Unknown word token (i+1)
 * up2    Unknown word token i+2
 * 
 * (Unknown word features use the standard Berkeley UNK classes)
 * 
 * numm1  Previous token contains a numeral
 * num    Token contains a numeral
 * nump1  Next token contains a numeral
 * 
 * num0   0% numerals
 * num20  20+% numerals
 * num40  40+% numerals
 * num60  60+% numerals
 * num80  80+% numerals
 * num100 100% numerals
 * 
 * punctm1  Previous token contains punctuation
 * punct    Token contains punctuation
 * punctp1  Next token contains punctuation
 * 
 * punct0   0% punctuation
 * punct20  20+% punctuation
 * punct40  40+% punctuation
 * punct60  60+% punctuation
 * punct80  80+% punctuation
 * punct100 100% punctuation
 * 
 * us       Unigram suffix
 * bs       Bigram suffix
 * </pre>
 */
public abstract class TaggerFeatureExtractor<S extends Sequence> extends FeatureExtractor<S> {

    private static final long serialVersionUID = 1L;

    final TaggerFeatureExtractor.TemplateElement[][] templates;
    final long[] featureOffsets;

    final SymbolSet<String> lexicon;
    final SymbolSet<String> tags;

    final SymbolSet<String> posSet;
    final SymbolSet<String> unigramSuffixSet, bigramSuffixSet;

    final int nullToken, nullTag;
    final int lexiconSize, tagSetSize, unkClassSetSize, posSetSize, unigramSuffixSetSize, bigramSuffixSetSize;
    final long featureVectorLength;

    /**
     * Constructs a {@link FeatureExtractor} using the specified feature templates
     * 
     * @param featureTemplates
     * @param lexicon
     * @param unkClassSet
     * @param tagSet
     * @param posSet
     */
    public TaggerFeatureExtractor(final String featureTemplates, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet, final SymbolSet<String> tagSet) {
        this(featureTemplates, lexicon, unkClassSet, posSet, null, null, tagSet);
    }

    /**
     * Constructs a {@link FeatureExtractor} using the specified feature templates
     * 
     * @param featureTemplates
     * @param lexicon
     * @param unkClassSet
     * @param unigramSuffixSet
     * @param bigramSuffixSet
     * @param tagSet
     * @param posSet
     */
    public TaggerFeatureExtractor(final String featureTemplates, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet,
            final SymbolSet<String> unigramSuffixSet, final SymbolSet<String> bigramSuffixSet,
            final SymbolSet<String> tagSet) {

        this.lexicon = lexicon;
        this.lexiconSize = lexicon.size();
        this.nullToken = lexicon.getIndex(Grammar.nullSymbolStr);

        this.posSet = posSet;
        this.posSetSize = posSet != null ? posSet.size() : 0;

        if (unigramSuffixSet != null) {
            this.unigramSuffixSet = unigramSuffixSet;
            this.bigramSuffixSet = bigramSuffixSet;
        } else {
            this.unigramSuffixSet = new SymbolSet<String>();
            this.unigramSuffixSet.defaultReturnValue(nullToken);
            this.bigramSuffixSet = new SymbolSet<String>();
            this.bigramSuffixSet.defaultReturnValue(nullToken);

            for (int i = 0; i < lexicon.size(); i++) {
                final String token = lexicon.getSymbol(i);
                this.unigramSuffixSet.addSymbol(token.substring(token.length() - 1));
                if (token.length() > 1) {
                    this.bigramSuffixSet.addSymbol(token.substring(token.length() - 2));
                }
            }
        }
        this.unigramSuffixSetSize = this.unigramSuffixSet.size();
        this.bigramSuffixSetSize = this.bigramSuffixSet.size();

        this.tags = tagSet;
        this.tagSetSize = tags.size();
        this.unkClassSetSize = unkClassSet.size();
        this.nullTag = tags.getIndex(Grammar.nullSymbolStr);

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

    @Override
    public int templateCount() {
        return templates.length;
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

            // Lots of binary features
            case numm1:
            case num:
            case nump1:

            case num20:
            case num40:
            case num60:
            case num80:
            case num100:

            case punctm1:
            case punct:
            case punctp1:

            case punct20:
            case punct40:
            case punct60:
            case punct80:
            case punct100:
                size *= 2;
                break;

            case posm3:
            case posm2:
            case posm1:
            case pos:
            case posp1:
            case posp2:
            case posp3:
                size *= posSetSize;
                break;

            case usm1:
            case us:
            case usp1:
                size *= unigramSuffixSetSize;
                break;

            case bsm1:
            case bs:
            case bsp1:
                size *= bigramSuffixSetSize;
                break;
            }

            if (size < 0) {
                throw new IllegalArgumentException("Feature set too large. Features limited to " + Long.MAX_VALUE);
            }
        }
        return size;
    }

    @Override
    public long vectorLength() {
        return featureVectorLength;
    }

    enum TemplateElement {
        tm3(-3), // Tag i-3
        tm2(-2), // Tag i-2
        tm1(-1), // Previous tag

        posm3(-3), // POS i-3
        posm2(-2), // POS i-2
        posm1(-1), // Previous POS
        pos(0), // Current POS
        posp1(1), // Next POS
        posp2(1), // POS i+2
        posp3(1), // POS i+3

        wm2(-2), // Word i-2
        wm1(-1), // Previous word (i-1)
        w(0), // Word
        wp1(1), // Next word (i+1)
        wp2(2), // Word i+2

        um2(-2), // Unknown word token i-2
        um1(-1), // Unknown word token (i-1)
        u(0), // Unknown word token
        up1(1), // Unknown word token (i+1)
        up2(2), // Unknown word token i+2

        numm1(-1), // Previous token contains a numeral
        num(0), // Token contains a numeral
        nump1(1), // Next token contains a numeral

        num20(0, .2f), // 20+% numerals
        num40(0, .4f), // 40+% numerals
        num60(0, .6f), // 60+% numerals
        num80(0, .8f), // 80+% numerals
        num100(0, 1.0f), // 100% numerals

        punctm1(-1), // Previous token contains punctuation
        punct(0), // Token contains punctuation
        punctp1(1), // Next token contains punctuation

        punct20(0, .2f), // 20+% punctuation
        punct40(0, .4f), // 40+% punctuation
        punct60(0, .6f), // 60+% punctuation
        punct80(0, .8f), // 80+% punctuation
        punct100(0, 1.0f), // 100% punctuation

        usm1(-1), // Unigram suffix i-1
        us(0), // Unigram suffix
        usp1(1), // Unigram suffix i+1

        bsm1(-1), // Bigram suffix i-1
        bs(0), // Bigram suffix
        bsp1(1); // Bigram suffix i+1

        final int offset;
        final float value;

        private TemplateElement(final int offset) {
            this(offset, 0);
        }

        private TemplateElement(final int offset, final float value) {
            this.offset = offset;
            this.value = value;
        }
    }
}