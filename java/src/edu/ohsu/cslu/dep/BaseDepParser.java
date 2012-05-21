package edu.ohsu.cslu.dep;

import java.util.LinkedList;
import java.util.logging.Level;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Action;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;

public abstract class BaseDepParser extends BaseCommandlineTool {

    public DependencyGraph parse(final DependencyGraph input, final AveragedPerceptron actionClassifier,
            final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        return parse(input, actionClassifier, null, tokens, pos, null);
    }

    public DependencyGraph parse(final DependencyGraph input, final AveragedPerceptron actionClassifier,
            final AveragedPerceptron labelClassifier, final SymbolSet<String> tokens, final SymbolSet<String> pos,
            final SymbolSet<String> labels) {

        final DependencyGraph parse = input.clone();
        float score = 0f;

        try {
            final LinkedList<Arc> stack = new LinkedList<Arc>();

            for (int step = 0, i = 0; step < parse.size(); step++) {
                final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor(tokens, pos);
                final SparseBitVector featureVector = fe.forwardFeatureVector(
                        new NivreParserContext(stack, parse.arcs), i);

                // TODO Consider changing Classifier to return an enum?
                Action action = null;
                if (stack.size() < 2) {
                    action = Action.SHIFT;
                } else if (BaseLogger.singleton().isLoggable(Level.FINER)) {
                    final ScoredClassification sc = actionClassifier.scoredClassify(featureVector);
                    action = Action.forInt(sc.classification);
                    score = sc.score;
                } else {
                    action = Action.forInt(actionClassifier.classify(featureVector));
                }
                switch (action) {
                case SHIFT:
                    stack.addFirst(parse.arcs[i++]);
                    break;

                case REDUCE_LEFT:
                    final Arc right = stack.removeFirst();
                    right.head = stack.peek().index;
                    right.score = score;
                    if (labelClassifier != null) {
                        right.label = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    break;

                case REDUCE_RIGHT:
                    final Arc tmp = stack.removeFirst();
                    final Arc left = stack.removeFirst();
                    left.head = tmp.index;
                    left.score = score;
                    if (labelClassifier != null) {
                        left.label = labels.getSymbol(labelClassifier.classify(featureVector));
                    }
                    stack.addFirst(tmp);
                    break;
                }
            }
        } catch (final IllegalArgumentException ignore) {
            // Ignore non-projective dependencies
        }
        return parse;
    }
}
