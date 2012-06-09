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

package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import java.util.List;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.perceptron.FeatureExtractor;

/**
 * Extracts features for move-classification in Nivre-style dependency parsing from the current state of such a parser
 * (stack, arcs, etc.)
 * 
 * @author Aaron Dunlop
 */
public class NivreParserFeatureExtractor extends FeatureExtractor<NivreParserContext> {

    private static final long serialVersionUID = 1L;

    final static int DISTANCE_NULL = 0;
    final static int DISTANCE_1 = DISTANCE_NULL + 1;
    final static int DISTANCE_2 = DISTANCE_1 + 1;
    final static int DISTANCE_3 = DISTANCE_2 + 1;
    final static int DISTANCE_45 = DISTANCE_3 + 1;
    final static int DISTANCE_6 = DISTANCE_45 + 1;
    final static int DISTANCE_BINS = 6;

    final TemplateElement[][] templates;
    final int[] featureOffsets;

    final SymbolSet<String> tokens;
    final SymbolSet<String> pos;
    final SymbolSet<String> labels;
    final int nullPosTag, nullToken, nullLabel;
    final int tokenSetSize, posSetSize, labelSetSize;
    final int featureVectorLength;

    public NivreParserFeatureExtractor(final SymbolSet<String> tokens, final SymbolSet<String> pos,
            final SymbolSet<String> labels) {
        // Features:
        //
        // Previous word (on the stack), current word (top-of-stack), next word (not-yet-shifted),
        //
        // UNK symbol for each of those 3 words (in the same range as the tokens themselves)
        //
        // POS for each of those 3 words
        //
        // Start-of-string indicator for previous word
        //
        // End-of-string indicator for next word
        //
        // Previous POS + current POS
        // Previous POS + current word
        // Previous word + current POS
        //
        // Distance between the top two words on the stack (the two under consideration for reduce operations)
        // Binned: 1, 2, 3, 4-5, 6+ words
        //
        this("sw2,sw1,iw1,st2,st1,it1,it1_it2,iw1_it2,it1_iw2,d", tokens, pos, labels);
    }

    /**
     * Supported template abbreviations:
     * 
     * sw3, sw2, sw1, iw1, iw1, iw3, iw4
     * 
     * st3, st2, st1, it1, it2, it3, it4
     * 
     * d
     * 
     * @param featureTemplates
     * @param tokens
     * @param pos
     */
    public NivreParserFeatureExtractor(final String featureTemplates, final SymbolSet<String> tokens,
            final SymbolSet<String> pos, final SymbolSet<String> labels) {

        this.tokens = tokens;
        this.tokenSetSize = tokens.size();
        this.nullToken = tokens.getIndex(DependencyGraph.NULL);

        this.pos = pos;
        this.posSetSize = pos.size();
        this.nullPosTag = pos.getIndex(DependencyGraph.NULL);

        this.labels = labels;
        this.labelSetSize = labels.size();
        this.nullLabel = labels.getIndex(DependencyGraph.NULL);

        final String[] templateStrings = featureTemplates.split(",");
        this.templates = new TemplateElement[templateStrings.length][];
        this.featureOffsets = new int[this.templates.length];

        for (int i = 0; i < featureOffsets.length; i++) {
            templates[i] = template(templateStrings[i]);
        }

        for (int i = 1; i < featureOffsets.length; i++) {
            featureOffsets[i] = featureOffsets[i - 1] + templateSize(templates[i - 1]);
        }
        this.featureVectorLength = featureOffsets[featureOffsets.length - 1]
                + templateSize(templates[templates.length - 1]);
    }

    private TemplateElement[] template(final String templateString) {
        final String[] split = templateString.split("_");
        final TemplateElement[] template = new TemplateElement[split.length];
        for (int i = 0; i < split.length; i++) {
            template[i] = TemplateElement.valueOf(TemplateElement.class, split[i]);
        }
        return template;
    }

    private int templateSize(final TemplateElement[] template) {
        int size = 1;
        for (int i = 0; i < template.length; i++) {
            switch (template[i]) {
            case s2t:
            case s1t:
            case s0t:
            case i0t:
            case i1t:
            case i2t:
            case i3t:
            case s0m3t:
            case s0m2t:
            case s0m1t:
            case s01t:
            case s02t:
            case s1m3t:
            case s1m2t:
            case s1m1t:
            case s11t:
            case s12t:
                size *= posSetSize;
                break;

            case s2w:
            case s1w:
            case s0w:
            case i0w:
            case i1w:
            case i2w:
            case i3w:
            case s0m3w:
            case s0m2w:
            case s0m1w:
            case s01w:
            case s02w:
            case s1m3w:
            case s1m2w:
            case s1m1w:
            case s11w:
            case s12w:
                size *= tokenSetSize;
                break;

            case s0ldep:
            case s1ldep:
            case s0rdep:
            case s1rdep:
                size *= labelSetSize;
                break;

            case d:
                size *= DISTANCE_BINS;
                break;
            }
        }
        return size;
    }

    @Override
    public long featureCount() {
        return featureVectorLength;
    }

    @Override
    public SparseBitVector forwardFeatureVector(final NivreParserContext source, final int tokenIndex) {

        final IntArrayList featureIndices = new IntArrayList();

        // TODO Handle UNKs
        for (int i = 0; i < templates.length; i++) {
            try {
                int feature = 0;
                final TemplateElement[] template = templates[i];
                for (int j = 0; j < template.length; j++) {
                    final TemplateElement t = template[j];
                    switch (t) {
                    case s2t:
                    case s1t:
                    case s0t:
                        feature *= posSetSize;
                        feature += tag(source.stack, t.index);
                        break;

                    case s0m3t:
                    case s0m2t:
                    case s0m1t:
                    case s01t:
                    case s02t:
                    case s1m3t:
                    case s1m2t:
                    case s1m1t:
                    case s11t:
                    case s12t:
                        feature *= posSetSize;
                        feature += tag(source.arcs, source.stack, t.index, t.offset);
                        break;

                    case i0t:
                    case i1t:
                    case i2t:
                    case i3t:
                        feature *= posSetSize;
                        feature += tag(source.arcs, tokenIndex + t.index);
                        break;

                    case s2w:
                    case s1w:
                    case s0w:
                        feature *= tokenSetSize;
                        feature += token(source.stack, t.index);
                        break;

                    case s0m3w:
                    case s0m2w:
                    case s0m1w:
                    case s01w:
                    case s02w:
                    case s1m3w:
                    case s1m2w:
                    case s1m1w:
                    case s11w:
                    case s12w:
                        feature *= tokenSetSize;
                        feature += token(source.arcs, source.stack, t.index, t.offset);
                        break;

                    case i0w:
                    case i1w:
                    case i2w:
                    case i3w:
                        feature *= tokenSetSize;
                        feature += token(source.arcs, tokenIndex + t.index);
                        break;

                    case s0ldep:
                    case s1ldep:
                        feature *= labelSetSize;

                        if (source.stack.size() <= t.index) {
                            feature += nullLabel;
                            break;
                        }

                        feature += leftDependentLabel(source.arcs, source.stack.get(t.index).index);
                        break;

                    case s0rdep:
                    case s1rdep:
                        feature *= labelSetSize;

                        if (source.stack.size() <= t.index) {
                            feature += nullLabel;
                            break;
                        }

                        feature += rightDependentLabel(source.arcs, source.stack.get(t.index).index);
                        break;

                    case d:
                        feature *= DISTANCE_BINS;

                        if (source.stack.size() < 2) {
                            throw new InvalidFeatureException();
                        }
                        // Previous word on the stack
                        final Arc previousWord = source.stack.get(1);
                        // Top word on the stack
                        final Arc currentWord = source.stack.get(0);

                        // Distance between top two words on stack
                        switch (currentWord.index - previousWord.index) {
                        case 1:
                            feature += DISTANCE_1;
                            break;
                        case 2:
                            feature += DISTANCE_2;
                            break;
                        case 3:
                            feature += DISTANCE_3;
                            break;
                        case 4:
                        case 5:
                            feature += DISTANCE_45;
                            break;
                        default:
                            feature += DISTANCE_6;
                            break;
                        }
                        if (j < template.length - 1) {
                            feature *= DISTANCE_BINS;
                        }
                        break;
                    }

                }
                featureIndices.add(featureOffsets[i] + feature);
            } catch (final InvalidFeatureException e) {
                // Just skip this feature
            }
        }

        return new SparseBitVector(featureVectorLength, featureIndices.toIntArray());
    }

    /**
     * @param stack
     * @param index
     * @return
     */
    private int token(final List<Arc> stack, final int index) {
        if (index < 0 || index >= stack.size()) {
            return nullToken;
        }
        return tokens.getIndex(stack.get(index).token);
    }

    /**
     * @param arcs
     * @param i
     * @return
     */
    private int token(final Arc[] arcs, final int i) {
        if (i < 0 || i >= arcs.length) {
            return nullToken;
        }
        return tokens.getIndex(arcs[i].token);
    }

    /**
     * @param arcs
     * @param stack
     * @param stackIndex
     * @param offset
     * @return
     */
    private int token(final Arc[] arcs, final List<Arc> stack, final int index, final int offset) {
        if (index < 0 || index >= stack.size()) {
            return nullToken;
        }

        final int i = stack.get(index).index + offset - 1;
        if (i < 0 || i >= arcs.length) {
            return nullToken;
        }

        return tokens.getIndex(arcs[i].token);
    }

    /**
     * @param stack
     * @param stackIndex
     * @return
     */
    private int tag(final List<Arc> stack, final int stackIndex) {
        if (stackIndex < 0 || stackIndex >= stack.size()) {
            return nullPosTag;
        }
        return pos.getIndex(stack.get(stackIndex).pos);
    }

    /**
     * @param arcs
     * @param stack
     * @param stackIndex
     * @param offset
     * @return
     */
    private int tag(final Arc[] arcs, final List<Arc> stack, final int index, final int offset) {
        if (index < 0 || index >= stack.size()) {
            return nullPosTag;
        }

        final int i = stack.get(index).index + offset - 1;
        if (i < 0 || i >= arcs.length) {
            return nullPosTag;
        }

        return pos.getIndex(arcs[i].pos);
    }

    /**
     * @param arcs
     * @param i
     * @return
     */
    private int tag(final Arc[] arcs, final int i) {
        if (i < 0 || i >= arcs.length) {
            return nullPosTag;
        }
        return pos.getIndex(arcs[i].pos);
    }

    /**
     * @param arcs
     * @param i
     * @return
     */
    private int leftDependentLabel(final Arc[] arcs, final int i) {
        if (i < 0 || i >= arcs.length) {
            return nullLabel;
        }
        for (int j = 0; j < arcs.length; j++) {
            if (arcs[j].predictedHead == i) {
                return labels.getIndex(arcs[j].predictedLabel);
            }
        }
        return nullLabel;
    }

    /**
     * @param arcs
     * @param i
     * @return
     */
    private int rightDependentLabel(final Arc[] arcs, final int i) {
        if (i < 0 || i >= arcs.length) {
            return nullLabel;
        }
        for (int j = arcs.length - 1; j >= 0; j--) {
            if (arcs[j].predictedHead == i) {
                return labels.getIndex(arcs[j].predictedLabel);
            }
        }
        return nullLabel;
    }

    /**
     * @param arcs
     * @param i
     * @return
     */
    private int unk(final Arc[] arcs, final int i) {
        if (i < 0 || i >= arcs.length) {
            return nullToken;
        }
        return tokens.getIndex(Tokenizer.berkeleyGetSignature(arcs[i].token, i == 0, tokens));
    }

    @Override
    public Vector forwardFeatureVector(final NivreParserContext source, final int tokenIndex, final float[] tagScores) {
        return null;
    }

    private enum TemplateElement {
        s2w(2, 0),
        s1w(1, 0),
        s0w(0, 0),
        i0w(0, 0),
        i1w(1, 0),
        i2w(2, 0),
        i3w(3, 0),
        s2t(2, 0),
        s1t(1, 0),
        s0t(0, 0),
        i0t(0, 0),
        i1t(1, 0),
        i2t(2, 0),
        i3t(3, 0),
        s0ldep(0, 0),
        s1ldep(1, 0),
        s0rdep(0, 0),
        s1rdep(1, 0),

        // Features with absolute offsets from stack words (e.g., the word prior to the right candidate (top-of-stack)
        // regardless of whether that word has already been reduced)
        s0m3t(0, -3),
        s0m2t(0, -2),
        s0m1t(0, -1),
        s01t(0, 1),
        s02t(0, 2),

        s1m3t(1, -3),
        s1m2t(1, -2),
        s1m1t(1, -1),
        s11t(1, 1),
        s12t(1, 2),

        s0m3w(0, -3),
        s0m2w(0, -2),
        s0m1w(0, -1),
        s01w(0, 1),
        s02w(0, 2),

        s1m3w(1, -3),
        s1m2w(1, -2),
        s1m1w(1, -1),
        s11w(1, 1),
        s12w(1, 2),

        d(-1, -1);

        final int index;
        final int offset;

        private TemplateElement(final int index, final int offset) {
            this.index = index;
            this.offset = offset;
        }
    }

    private class InvalidFeatureException extends Exception {

        private static final long serialVersionUID = 1L;
    }
}
