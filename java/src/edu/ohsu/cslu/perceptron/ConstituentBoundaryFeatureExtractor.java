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

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.datastructs.vectors.LargeSparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.chart.Chart;

/**
 * 
 * Extracts constituent boundary features from sequence, as mapped onto a chart structure. Position indexes a specific
 * chart cell.
 * 
 * TODO Consolidate shared code with {@link TaggerFeatureExtractor}
 * 
 * TODO Extract the enum somewhere it can be JavaDoc'd (this applies to all FeatureExtractors)
 * 
 * @author Aaron Dunlop
 * @since Feb 8, 2013
 */
public class ConstituentBoundaryFeatureExtractor<S extends ConstituentBoundarySequence> extends FeatureExtractor<S> {

    private static final long serialVersionUID = 1L;

    final TemplateElement[][] templates;
    final long[] featureOffsets;

    final SymbolSet<String> lexicon;

    final int nullToken, nullTag;
    final int lexiconSize, posSetSize, unkClassSetSize;
    final long featureVectorLength;

    private final boolean excludeSpan1Cells;

    /**
     * Constructs a {@link FeatureExtractor} using the specified feature templates
     * 
     * @param featureTemplates
     * @param lexicon
     * @param unkClassSet
     * @param posSet
     */
    public ConstituentBoundaryFeatureExtractor(final String featureTemplates, final SymbolSet<String> lexicon,
            final SymbolSet<String> unkClassSet, final SymbolSet<String> posSet, final boolean excludeSpan1Cells) {

        this.lexicon = lexicon;
        this.lexiconSize = lexicon.size();
        this.nullToken = lexicon.getIndex(Grammar.nullSymbolStr);

        this.unkClassSetSize = unkClassSet.size();
        this.nullTag = posSet.getIndex(Grammar.nullSymbolStr);
        this.posSetSize = posSet.size();

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

        this.excludeSpan1Cells = excludeSpan1Cells;
    }

    @Override
    public int templateCount() {
        return templates.length;
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

            case ltm2:
            case ltm1:
            case lt:
            case ltp1:
            case rtm1:
            case rt:
            case rtp1:
            case rtp2:
                size *= posSetSize;
                break;

            case lum1:
            case lu:
            case ru:
            case rup1:
                size *= unkClassSetSize;
                break;

            case lwm1:
            case lw:
            case rw:
            case rwp1:
                size *= lexiconSize;
                break;

            // Indicator features
            case s1:
            case s2:
            case s3:
            case s4:
            case s5:
            case s10:
            case s20:
            case s30:
            case s40:
            case s50:
            case rs2:
            case rs4:
            case rs6:
            case rs8:
            case rs10:
                size *= 2;
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

    /**
     * @param input
     * @param position The index of a specific chart cell (disambiguating a start and end, or start and span)
     */
    @Override
    public BitVector featureVector(final S input, final int position) {

        final long[] featureIndices = new long[templates.length];

        final short[] startAndEnd = Chart.startAndEnd(position, input.sentenceLength, excludeSpan1Cells);
        final short start = startAndEnd[0];
        final short end = startAndEnd[1];
        final int span = end - start;

        for (int i = 0; i < templates.length; i++) {
            long feature = 0;
            final TemplateElement[] template = templates[i];
            for (int j = 0; j < template.length; j++) {
                final TemplateElement t = template[j];
                final int leftIndex = start + t.offset;
                final int rightIndex = end + t.offset - 1;

                switch (t) {

                case ltm2:
                case ltm1:
                case lt:
                case ltp1:
                    feature *= posSetSize;
                    feature += ((leftIndex < 0 || leftIndex >= input.posTags.length) ? nullTag
                            : input.posTags[leftIndex]);
                    break;

                case rtm1:
                case rt:
                case rtp1:
                case rtp2:
                    feature *= posSetSize;
                    feature += ((rightIndex < 0 || rightIndex >= input.posTags.length) ? nullTag
                            : input.posTags[rightIndex]);
                    break;

                case lum1:
                case lu:
                    feature *= unkClassSetSize;
                    feature += ((leftIndex < 0 || leftIndex >= input.mappedTokens.length) ? nullToken
                            : input.mappedUnkSymbols[leftIndex]);
                    break;

                case ru:
                case rup1:
                    feature *= unkClassSetSize;
                    feature += ((rightIndex < 0 || rightIndex >= input.mappedTokens.length) ? nullToken
                            : input.mappedUnkSymbols[rightIndex]);
                    break;

                case lwm1:
                case lw:
                    feature *= lexiconSize;
                    feature += ((leftIndex < 0 || leftIndex >= input.mappedTokens.length) ? nullToken
                            : input.mappedTokens[leftIndex]);
                    break;

                case rw:
                case rwp1:
                    feature *= lexiconSize;
                    feature += ((rightIndex < 0 || rightIndex >= input.mappedTokens.length) ? nullToken
                            : input.mappedTokens[rightIndex]);
                    break;

                // Indicator features
                case s1:
                case s2:
                case s3:
                case s4:
                case s5:
                case s10:
                case s20:
                case s30:
                case s40:
                case s50:
                    feature *= 2;
                    if (span >= t.metric) {
                        feature++;
                    }
                    break;

                case rs2:
                case rs4:
                case rs6:
                case rs8:
                case rs10:
                    feature *= 2;
                    // Allow a small delta to account for computational rounding issues
                    if (span * 1.0 / input.sentenceLength >= t.metric - .001f) {
                        feature++;
                    }
                    break;
                }
            }
            final long featureIndex = featureOffsets[i] + feature;
            assert featureIndex >= 0 && featureIndex < featureVectorLength;
            featureIndices[i] = featureIndex;
        }

        return featureVectorLength > Integer.MAX_VALUE ? new LargeSparseBitVector(featureVectorLength, featureIndices)
                : new SparseBitVector(featureVectorLength, featureIndices);
    }

    // /**
    // * Returns the chart array index of a chart cell, using left-to-right, bottom-up traversal. Inverse of
    // * {@link #startAndEnd(int, int, boolean)}.
    // *
    // * @param start
    // * @param end
    // * @param length Sequence length
    // * @param excludeSpan1Cells If true, cell indexes will start with the first cell on the span-2 row
    // * @return the chart array index of a chart cell, using left-to-right, bottom-up traversal.
    // */
    // public static int cellIndex(final int start, final int end, final int length, final boolean excludeSpan1Cells) {
    // final int span = end - start;
    // if (excludeSpan1Cells && end - start == 1) {
    // throw new IllegalArgumentException("Cannot compute an index for " + start + "," + end
    // + " while excluding span-1 cells");
    // }
    // final int index = length * (span - 1) - ((span - 2) * (span - 1) / 2) + start
    // - (excludeSpan1Cells ? length : 0);
    // return index;
    // }
    //
    // /**
    // * Returns the start and end indices of a specified chart cell, using left-to-right, bottom-up traversal. Inverse
    // of
    // * {@link #cellIndex(int, int, int, boolean)}.
    // *
    // * @param index
    // * @param sentenceLength Sequence length
    // * @param excludeSpan1Cells If true, cell indexes start with the first cell on the span-2 row
    // * @return the start and end indices of the specified chart cell.
    // */
    // public static short[] startAndEnd(final int index, final int sentenceLength, final boolean excludeSpan1Cells) {
    //
    // for (short span = (short) (excludeSpan1Cells ? 2 : 1), currentRowStart = 0, nextRowStart = 0; span <=
    // sentenceLength; span++, currentRowStart = nextRowStart) {
    // nextRowStart = (short) (currentRowStart + (sentenceLength - span + 1));
    // if (index < nextRowStart) {
    // final short start = (short) (index - currentRowStart);
    // return new short[] { start, (short) (index - currentRowStart + span) };
    // }
    // }
    // throw new IllegalArgumentException("Cell " + index + " not found in chart of size " + sentenceLength);
    // }

    private enum TemplateElement {
        ltm2(-2), // Left boundary tag i-2 (outside the constituent)
        ltm1(-1), // Left boundary tag i-1 (outside the constituent)
        lt(0), // Left boundary tag i (inside the constituent)
        ltp1(1), // Left boundary tag i+1 (inside the constituent)

        rtm1(-1), // Right boundary tag j-1 (inside the constituent)
        rt(0), // Right boundary tag j (inside the constituent)
        rtp1(1), // Right boundary tag j+1 (outside the constituent)
        rtp2(2), // Right boundary tag j+2 (outside the constituent)

        lwm1(-1), // Left boundary word i-1 (outside the constituent)
        lw(0), // Left boundary word i (inside the constituent)

        rw(0), // Right boundary word j (inside the constituent)
        rwp1(1), // Right boundary word j (outside the constituent)

        lum1(-1), // Left boundary UNK i-1 (outside the constituent)
        lu(0), // Left boundary UNK i (inside the constituent)

        ru(0), // Right boundary UNK j (inside the constituent)
        rup1(1), // Right boundary UNK j (outside the constituent)

        // Absolute span - bins 2,3,4,5, >= 10, >= 20, ...
        s1(1.0f),
        s2(2.0f),
        s3(3.0f),
        s4(4.0f),
        s5(5.0f),
        s10(10.0f),
        s20(20.0f),
        s30(30.0f),
        s40(40.0f),
        s50(50.0f),

        // Relative span - .2, .4, .6, .8, 1.0
        rs2(.2f),
        rs4(.4f),
        rs6(.6f),
        rs8(.8f),
        rs10(1.0f);

        final int offset;
        final float metric;

        private TemplateElement(final int offset) {
            this.offset = offset;
            this.metric = 0;
        }

        private TemplateElement(final float metric) {
            this.offset = 0;
            this.metric = metric;
        }
    }
}
