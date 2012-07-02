package edu.ohsu.cslu.dep;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.io.Serializable;
import java.util.EnumSet;
import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;

public class TransitionDepParser implements Serializable {

    private static final long serialVersionUID = 1L;

    public final NivreParserFeatureExtractor featureExtractor;
    public final AveragedPerceptron shiftReduceClassifier;
    public final AveragedPerceptron reduceDirectionClassifier;
    public final AveragedPerceptron labelClassifier;
    public final SymbolSet<String> tokens;
    public final SymbolSet<String> pos;
    public final SymbolSet<String> labels;

    public TransitionDepParser(final NivreParserFeatureExtractor featureExtractor,
            final AveragedPerceptron shiftReduceClassifier, final AveragedPerceptron reduceDirectionClassifier,
            final AveragedPerceptron labelClassifier, final SymbolSet<String> tokens, final SymbolSet<String> pos,
            final SymbolSet<String> labels) {

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
            final BitVector featureVector = featureExtractor.forwardFeatureVector(new NivreParserContext(stack,
                    parse.arcs), i);

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
