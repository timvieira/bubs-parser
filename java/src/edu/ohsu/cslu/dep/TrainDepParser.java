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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.logging.Level;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.dep.TransitionDepParser.ParserAction;
import edu.ohsu.cslu.dep.TransitionDepParser.ReduceDirection;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.parser.cellselector.DepGraphCellSelectorModel;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Trains a perceptron classifier for greedy dependency parsing.
 * 
 * Input: Dependency treebank in CoNLL 2007 format
 * 
 * Output: Training status and training/dev-set accuracies
 * 
 * The model (3 averaged perceptrons, vocabulary and lexicon) will be serialized to the specified model file.
 */
public class TrainDepParser extends BaseCommandlineTool {

    @Option(name = "-i", metaVar = "count", usage = "Training iterations")
    private int trainingIterations = 10;

    @Option(name = "-m", choiceGroup = "output", metaVar = "file", usage = "Output dependency parser model file")
    private File outputModelFile;

    @Option(name = "-csm", choiceGroup = "output", metaVar = "file", usage = "Output cell-selector model file")
    private File outputCellSelectorModelFile;

    @Option(name = "-d", metaVar = "file", usage = "Development set in CoNLL 2007 format")
    private File devSet;

    @Option(name = "-f", metaVar = "features", usage = "Feature templates")
    private String featureTemplates = "st1,st2,st3,it1,it2,it3,sw1,sw2,iw1";

    @Option(name = "-l", usage = "Label arcs (if false, no arc labels will be assigned)")
    private boolean classifyLabels = false;

    @Override
    protected void run() throws Exception {

        //
        // Read each training instance
        //
        final LinkedList<DependencyGraph> trainingExamples = new LinkedList<DependencyGraph>();
        for (final BufferedReader br = inputAsBufferedReader(); br.ready();) {
            try {
                final DependencyGraph g = DependencyGraph.readConll(br);
                // If we can't produce a derivation, skip this example
                g.stackProjectiveDerivation();
                trainingExamples.add(g);
            } catch (final IllegalArgumentException ignore) {
            }
        }

        LinkedList<DependencyGraph> devExamples = null;
        if (devSet != null) {
            devExamples = new LinkedList<DependencyGraph>();
            for (final BufferedReader br = fileAsBufferedReader(devSet); br.ready();) {
                devExamples.add(DependencyGraph.readConll(br));
            }
        }

        final MutableEnumeration<String> tokens = new MutableEnumeration<String>();
        tokens.addSymbol(DependencyGraph.NULL);
        tokens.addSymbol(DependencyGraph.ROOT.token);
        final MutableEnumeration<String> pos = new MutableEnumeration<String>();
        pos.addSymbol(DependencyGraph.NULL);
        pos.addSymbol(DependencyGraph.ROOT.pos);
        final MutableEnumeration<String> labels = new MutableEnumeration<String>();
        labels.addSymbol(DependencyGraph.NULL);
        labels.addSymbol(DependencyGraph.ROOT.label);

        for (final DependencyGraph example : trainingExamples) {
            for (int i = 0; i < example.arcs.length; i++) {
                final Arc arc = example.arcs[i];
                tokens.addSymbol(arc.token);

                // Add an entry for the UNK label as well
                tokens.addSymbol(DecisionTreeTokenClassifier.berkeleyGetSignature(arc.token, i == 0, tokens));
                pos.addSymbol(arc.pos);
                labels.addSymbol(arc.label);
            }
        }

        final TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor(featureTemplates, tokens, pos,
                labels);

        // At each step, we have 3 possible actions (shift, reduce-left, reduce-right), but we divide them into 2
        // classifiers - one to decide between shift and reduce, and one to select reduce direction. For the moment, we
        // use the same feature-set for both.
        final AveragedPerceptron shiftReduceClassifier = new AveragedPerceptron(2, fe.vectorLength());
        final AveragedPerceptron reduceDirectionClassifier = new AveragedPerceptron(2, fe.vectorLength());
        // Label arcs, with a third classifier
        final AveragedPerceptron labelClassifier = classifyLabels ? new AveragedPerceptron(labels.size(),
                fe.vectorLength()) : null;

        final TransitionDepParser parser = new TransitionDepParser(fe, shiftReduceClassifier,
                reduceDirectionClassifier, labelClassifier, tokens, pos, labels);
        //
        // Iterate through the training instances
        //
        for (int iteration = 0, examples = 0; iteration < trainingIterations; iteration++, examples = 0) {
            for (final DependencyGraph example : trainingExamples) {
                example.clear();
                try {
                    final DependencyGraph.StackProjectiveAction[] derivation = example.stackProjectiveDerivation();

                    final Arc[] arcs = example.arcs;
                    final LinkedList<Arc> stack = new LinkedList<Arc>();

                    for (int step = 0, i = 0; step < derivation.length; step++) {
                        final NivreParserContext context = new NivreParserContext(stack, arcs, i);
                        final BitVector featureVector = fe.featureVector(context, i);

                        switch (derivation[step]) {

                        case SHIFT:
                            if (stack.size() >= 2) {
                                shiftReduceClassifier.train(ParserAction.SHIFT.ordinal(), featureVector);
                            }
                            stack.addFirst(arcs[i++]);
                            break;

                        case REDUCE_LEFT: {
                            shiftReduceClassifier.train(ParserAction.REDUCE.ordinal(), featureVector);
                            reduceDirectionClassifier.train(ReduceDirection.LEFT.ordinal(), featureVector);
                            final Arc top = stack.removeFirst();
                            top.predictedHead = stack.peek().index;

                            if (labelClassifier != null) {
                                labelClassifier.train(labels.getIndex(top.label), featureVector);
                            }
                            break;
                        }
                        case REDUCE_RIGHT: {
                            shiftReduceClassifier.train(ParserAction.REDUCE.ordinal(), featureVector);
                            reduceDirectionClassifier.train(ReduceDirection.RIGHT.ordinal(), featureVector);

                            final Arc top = stack.removeFirst();
                            final Arc second = stack.removeFirst();
                            second.predictedHead = top.index;
                            stack.addFirst(top);

                            if (labelClassifier != null) {
                                labelClassifier.train(labels.getIndex(second.label), featureVector);
                            }
                            break;
                        }
                        }
                    }
                } catch (final IllegalArgumentException ignore) {
                    // Ignore non-projective dependencies
                }
                if (examples++ % 1000 == 0) {
                    System.out.print('.');
                }
            }
            System.out.println(iteration + 1);

            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                test(parser, trainingExamples, "Training-set");
            }
            if (devExamples != null) {
                test(parser, devExamples, "Dev-set");
            }
        }

        if (outputModelFile != null) {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModelFile));
            oos.writeObject(parser);
            oos.close();
        } else {
            final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputCellSelectorModelFile));
            oos.writeObject(new DepGraphCellSelectorModel(parser));
            oos.close();
        }
    }

    private void test(final TransitionDepParser parser, final LinkedList<DependencyGraph> examples, final String label) {

        final long startTime = System.currentTimeMillis();

        int correctArcs = 0, correctLabels = 0, total = 0;

        for (final DependencyGraph example : examples) {
            total += example.size() - 1;
            final DependencyGraph parse = parser.parse(example);
            correctArcs += parse.correctArcs();
            correctLabels += parse.correctLabels();
        }
        final long time = System.currentTimeMillis() - startTime;
        System.out.format("%s accuracy - unlabeled: %.2f%%  labeled %.2f%%  (%d ms, %.2f words/sec)\n", label,
                correctArcs * 100.0 / total, correctLabels * 100.0 / total, time, total * 1000.0 / time);
    }

    public static void main(final String[] args) {
        run(args);
    }
}
