package edu.ohsu.cslu.dep;

import java.util.LinkedList;

import cltool4j.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Action;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;

public abstract class BaseDepParser extends BaseCommandlineTool {

    public DependencyGraph parse(final DependencyGraph input, final AveragedPerceptron model,
            final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        final DependencyGraph parse = input.clone();

        try {
            final LinkedList<Arc> stack = new LinkedList<Arc>();

            for (int step = 0, i = 0; step < parse.size(); step++) {
                final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor(tokens, pos);
                final SparseBitVector featureVector = fe.forwardFeatureVector(
                        new NivreParserContext(stack, parse.arcs), i);

                // TODO Consider changing Classifier to return an enum?
                final Action action = stack.size() < 2 ? Action.SHIFT : Action.forInt(model.classify(featureVector));

                switch (action) {
                case SHIFT:
                    stack.addFirst(parse.arcs[i++]);
                    break;

                case REDUCE_LEFT:
                    stack.removeFirst().head = stack.peek().index;
                    break;

                case REDUCE_RIGHT:
                    final Arc tmp = stack.removeFirst();
                    stack.removeFirst().head = tmp.index;
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
