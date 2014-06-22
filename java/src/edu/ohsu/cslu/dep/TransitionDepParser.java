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
package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;
import edu.ohsu.cslu.util.MutableEnumeration;

public class TransitionDepParser implements Serializable {

    private static final long serialVersionUID = 1L;

    public final TransitionParserFeatureExtractor featureExtractor;
    public final AveragedPerceptron shiftReduceClassifier;
    public final AveragedPerceptron reduceDirectionClassifier;
    public final AveragedPerceptron labelClassifier;
    public final MutableEnumeration<String> tokens;
    public final MutableEnumeration<String> pos;
    public final MutableEnumeration<String> labels;

    public TransitionDepParser(final TransitionParserFeatureExtractor featureExtractor,
            final AveragedPerceptron shiftReduceClassifier, final AveragedPerceptron reduceDirectionClassifier,
            final AveragedPerceptron labelClassifier, final MutableEnumeration<String> tokens, final MutableEnumeration<String> pos,
            final MutableEnumeration<String> labels) {

        this.featureExtractor = featureExtractor;
        this.shiftReduceClassifier = shiftReduceClassifier;
        this.reduceDirectionClassifier = reduceDirectionClassifier;
        this.labelClassifier = labelClassifier;
        this.tokens = tokens;
        this.pos = pos;
        this.labels = labels;
    }

    @SuppressWarnings("null")
    public DependencyGraph parse(final DependencyGraph input) {

        final DependencyGraph parse = input.clear();

        final LinkedList<Arc> stack = new LinkedList<Arc>();

        final int totalSteps = parse.size() * 2 - 1;
        for (int step = 0, i = 0; step < totalSteps; step++) {
            final BitVector featureVector = featureExtractor.featureVector(new NivreParserContext(stack,
                    parse.arcs, i), i);

            ParserAction action = null;
            ScoredClassification shiftReduceClassification = null;

            if (stack.size() < 2) {
                action = ParserAction.SHIFT;
            } else {
                shiftReduceClassification = shiftReduceClassifier.scoredClassify(featureVector);
                action = ParserAction.forInt(shiftReduceClassification.classification);
            }

            switch (action) {
            case SHIFT:
                stack.addFirst(parse.arcs[i++]);

                break;

            case REDUCE:
                final ScoredClassification reduceDirectionClassification = reduceDirectionClassifier
                        .scoredClassify(featureVector);
                final ReduceDirection reduceDirection = ReduceDirection
                        .forInt(reduceDirectionClassification.classification);

                switch (reduceDirection) {
                case LEFT: {
                    final Arc top = stack.get(0);
                    final Arc second = stack.get(1);

                    stack.removeFirst();
                    top.predictedHead = second.index;
                    top.score = shiftReduceClassification.score * reduceDirectionClassification.score;
                    if (labelClassifier != null) {
                        top.predictedLabel = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    break;
                }
                case RIGHT: {
                    final Arc top = stack.get(0);
                    final Arc second = stack.get(1);

                    stack.removeFirst();
                    stack.removeFirst();
                    second.predictedHead = top.index;
                    second.score = shiftReduceClassification.score * reduceDirectionClassification.score;
                    if (labelClassifier != null) {
                        second.predictedLabel = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    stack.addFirst(top);
                    break;
                }
                }
            }
        }
        return parse;
    }

    public static enum ParserAction {
        SHIFT, REDUCE;

        final static Int2ObjectOpenHashMap<ParserAction> ordinalValueMap = new Int2ObjectOpenHashMap<ParserAction>();
        static {
            for (final ParserAction a : EnumSet.allOf(ParserAction.class)) {
                ordinalValueMap.put(a.ordinal(), a);
            }
        }

        public static ParserAction forInt(final int ordinalValue) {
            return ordinalValueMap.get(ordinalValue);
        }
    }

    public static enum ReduceDirection {
        RIGHT, LEFT;

        final static Int2ObjectOpenHashMap<ReduceDirection> ordinalValueMap = new Int2ObjectOpenHashMap<ReduceDirection>();
        static {
            for (final ReduceDirection a : EnumSet.allOf(ReduceDirection.class)) {
                ordinalValueMap.put(a.ordinal(), a);
            }
        }

        public static ReduceDirection forInt(final int ordinalValue) {
            return ordinalValueMap.get(ordinalValue);
        }
    }
}
