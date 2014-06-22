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

import java.util.LinkedList;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.dep.DependencyGraph.ArcEagerAction;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;

/**
 * @author Aaron Dunlop
 * @since Jul 5, 2012
 */
public class ArcEagerParser {
    private static final long serialVersionUID = 1L;

    public final TransitionParserFeatureExtractor featureExtractor;
    public final AveragedPerceptron actionClassifier;
    public final AveragedPerceptron labelClassifier;
    public final SymbolSet<String> tokens;
    public final SymbolSet<String> pos;
    public final SymbolSet<String> labels;

    public ArcEagerParser(final TransitionParserFeatureExtractor featureExtractor,
            final AveragedPerceptron actionClassifier, final AveragedPerceptron labelClassifier,
            final SymbolSet<String> tokens, final SymbolSet<String> pos, final SymbolSet<String> labels) {

        this.featureExtractor = featureExtractor;
        this.actionClassifier = actionClassifier;
        this.labelClassifier = labelClassifier;
        this.tokens = tokens;
        this.pos = pos;
        this.labels = labels;
    }

    public DependencyGraph parse(final DependencyGraph input) {

        final DependencyGraph parse = input.clear();

        final LinkedList<Arc> stack = new LinkedList<Arc>();

        final int totalSteps = parse.size() * 2 - 1;
        for (int step = 0, next = 0; step < totalSteps; step++) {
            final BitVector featureVector = featureExtractor.featureVector(new NivreParserContext(stack,
                    parse.arcs, next), next);

            ArcEagerAction action = null;
            ScoredClassification actionClassification = null;

            if (stack.size() < 1) {
                action = ArcEagerAction.SHIFT;
            } else {
                actionClassification = actionClassifier.scoredClassify(featureVector);
                action = ArcEagerAction.forInt(actionClassification.classification);

                // Prevent shifting ROOT before all arcs are assigned
                if (next == (parse.arcs.length - 1) && stack.size() > 0
                        && (action == ArcEagerAction.SHIFT || action == ArcEagerAction.ARC_RIGHT)) {
                    if (stack.get(0).predictedHead >= 0) {
                        action = ArcEagerAction.REDUCE;
                    } else {
                        action = ArcEagerAction.ARC_LEFT;
                    }
                }
            }

            switch (action) {
            case SHIFT:
                stack.addFirst(parse.arcs[next++]);
                break;

            case ARC_LEFT: {
                final Arc top = stack.removeFirst();
                final Arc nextWord = parse.arcs[next];
                top.predictedHead = nextWord.index;

                if (labelClassifier != null) {
                    top.predictedLabel = labels.getSymbol(labelClassifier.classify(featureVector));
                }
                break;
            }

            case ARC_RIGHT: {
                final Arc top = stack.peekFirst();
                final Arc nextWord = parse.arcs[next++];
                nextWord.predictedHead = top.index;
                stack.addFirst(nextWord);

                if (labelClassifier != null) {
                    top.predictedLabel = labels.getSymbol(labelClassifier.classify(featureVector));
                }
                break;
            }

            case REDUCE:
                stack.removeFirst();
                break;
            }
        }
        return parse;
    }
}
