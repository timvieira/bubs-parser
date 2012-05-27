package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.EnumSet;
import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;

public abstract class BaseDepParser extends BaseCommandlineTool {

    public DependencyGraph parse(final DependencyGraph input, final NivreParserFeatureExtractor featureExtractor,
            final AveragedPerceptron shiftReduceClassifier, final AveragedPerceptron reduceDirectionClassifier,
            final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        return parse(input, featureExtractor, shiftReduceClassifier, reduceDirectionClassifier, null, tokens, pos, null);
    }

    public DependencyGraph parse(final DependencyGraph input, final NivreParserFeatureExtractor featureExtractor,
            final AveragedPerceptron shiftReduceClassifier, final AveragedPerceptron reduceDirectionClassifier,
            final AveragedPerceptron labelClassifier, final SymbolSet<String> tokens, final SymbolSet<String> pos,
            final SymbolSet<String> labels) {

        final DependencyGraph parse = input.clone().clear();

        final LinkedList<Arc> stack = new LinkedList<Arc>();

        final int totalSteps = parse.size() * 2 - 1;
        for (int step = 0, i = 0; step < totalSteps; step++) {
            final SparseBitVector featureVector = featureExtractor.forwardFeatureVector(new NivreParserContext(stack,
                    parse.arcs), i);

            // TODO Consider changing Classifier to return an enum?
            ParserAction action = null;
            if (stack.size() < 2) {
                action = ParserAction.SHIFT;
            } else {
                action = ParserAction.forInt(shiftReduceClassifier.classify(featureVector));
            }

            switch (action) {
            case SHIFT:
                stack.addFirst(parse.arcs[i++]);
                break;

            case REDUCE:
                final ScoredClassification sc = reduceDirectionClassifier.scoredClassify(featureVector);

                switch (ReduceDirection.forInt(sc.classification)) {
                case LEFT:
                    final Arc right = stack.removeFirst();
                    right.head = stack.peek().index;
                    right.score = sc.score;
                    if (labelClassifier != null) {
                        right.label = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    break;

                case RIGHT:
                    final Arc tmp = stack.removeFirst();
                    final Arc left = stack.removeFirst();
                    left.head = tmp.index;
                    left.score = sc.score;
                    if (labelClassifier != null) {
                        left.label = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    stack.addFirst(tmp);
                    break;
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
