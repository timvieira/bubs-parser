package edu.ohsu.cslu.dep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedList;
import java.util.logging.Level;

import cltool4j.BaseCommandlineTool;
import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.dep.DependencyGraph.StackProjectiveAction;
import edu.ohsu.cslu.dep.TransitionDepParser.ParserAction;
import edu.ohsu.cslu.dep.TransitionDepParser.ReduceDirection;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;
import edu.ohsu.cslu.perceptron.AveragedPerceptron.ScoredClassification;
import edu.ohsu.cslu.perceptron.Perceptron;

/**
 * Trains a perceptron classifier for greedy dependency parsing.
 * 
 * Input: Dependency treebank in CoNLL 2007 format
 * 
 * Output: Training status and training/dev-set accuracies
 * 
 * The model (3 averaged perceptrons, vocabulary and lexicon) will be serialized to the specified model file.
 */
public class EvalDepClassifiers extends BaseCommandlineTool {

    @Option(name = "-i", metaVar = "count", usage = "Training iterations")
    private int trainingIterations = 10;

    @Option(name = "-d", required = true, metaVar = "file", usage = "Development set in CoNLL 2007 format")
    private File devSet;

    @Option(name = "-f", metaVar = "features", usage = "Feature templates")
    private String featureTemplates = "st1,st2,st3,it1,it2,it3,sw1,sw2,iw1";

    @Option(name = "-l", usage = "Label arcs (if false, no arc labels will be assigned)")
    private boolean classifyLabels = false;

    @Option(name = "-cas", metaVar = "file", usage = "Output constrained arc scores to file")
    private File constrainedArcScores;

    @Option(name = "-as", metaVar = "file", usage = "Output arc scores to file")
    private File arcScores;

    @Option(name = "-alpha", metaVar = "alpha", usage = "Training rate")
    private float alpha = 0.25f;

    @SuppressWarnings("null")
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

        final SymbolSet<String> tokens = new SymbolSet<String>();
        tokens.addSymbol(DependencyGraph.NULL);
        tokens.addSymbol(DependencyGraph.ROOT.token);
        final SymbolSet<String> pos = new SymbolSet<String>();
        pos.addSymbol(DependencyGraph.NULL);
        pos.addSymbol(DependencyGraph.ROOT.pos);
        final SymbolSet<String> labels = new SymbolSet<String>();
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
        final AveragedPerceptron shiftReduceClassifier = new AveragedPerceptron(alpha, new Perceptron.ZeroOneLoss(), 2,
                fe.vectorLength());
        final AveragedPerceptron reduceDirectionClassifier = new AveragedPerceptron(alpha,
                new Perceptron.ZeroOneLoss(), 2, fe.vectorLength());
        // Label arcs, with a third classifier
        final AveragedPerceptron labelClassifier = classifyLabels ? new AveragedPerceptron(alpha,
                new Perceptron.ZeroOneLoss(), labels.size(), fe.vectorLength()) : null;
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

            FileWriter arcScoreWriter;
            if (constrainedArcScores != null && iteration == trainingIterations - 1) {
                arcScoreWriter = new FileWriter(constrainedArcScores);
                arcScoreWriter
                        .write("sr_gold,sr_score,sr_class,lr_gold,lr_score,lr_class,l_index,span,buf,sent_len,label_gold,label_score,label_margin,label_class\n");
            } else {
                arcScoreWriter = null;
            }

            int shiftReduceClassifications = 0, correctShiftReduceClassifications = 0;
            int missedShifts = 0, missedReduces = 0;
            int reduceDirectionClassifications = 0, correctReduceDirectionClassifications = 0;

            for (final DependencyGraph example : devExamples) {
                example.clear();
                try {
                    final DependencyGraph.StackProjectiveAction[] derivation = example.stackProjectiveDerivation();

                    final Arc[] arcs = example.arcs;
                    final LinkedList<Arc> stack = new LinkedList<Arc>();

                    for (int step = 0, i = 0; step < derivation.length; step++) {
                        final NivreParserContext context = new NivreParserContext(stack, arcs, i);
                        final BitVector featureVector = fe.featureVector(context, i);
                        final ScoredClassification srClassification = shiftReduceClassifier
                                .scoredClassify(featureVector);

                        switch (derivation[step]) {

                        case SHIFT:
                            if (stack.size() >= 2) {
                                shiftReduceClassifications++;
                                if (srClassification.classification == ParserAction.SHIFT.ordinal()) {
                                    correctShiftReduceClassifications++;
                                } else {
                                    missedShifts++;
                                    if (BaseLogger.singleton().isLoggable(Level.FINER)) {
                                        outputParserState("DEBUG: Shift", context, i);
                                    }
                                }
                            }
                            stack.addFirst(arcs[i++]);
                            break;

                        case REDUCE_LEFT: {
                            shiftReduceClassifications++;
                            if (srClassification.classification == ParserAction.REDUCE.ordinal()) {
                                correctShiftReduceClassifications++;
                            } else {
                                missedReduces++;
                            }

                            reduceDirectionClassifications++;
                            final ScoredClassification lrClassification = reduceDirectionClassifier
                                    .scoredClassify(featureVector);
                            if (lrClassification.classification == ReduceDirection.LEFT.ordinal()) {
                                correctReduceDirectionClassifications++;
                            }

                            final Arc top = stack.removeFirst();
                            top.predictedHead = stack.peek().index;

                            final ScoredClassification labelClassification = labelClassifier != null ? labelClassifier
                                    .scoredClassify(featureVector) : null;
                            if (labelClassification != null) {
                                top.predictedLabel = labels.getSymbol(labelClassification.classification);
                            }

                            if (arcScoreWriter != null) {
                                write(arcScoreWriter, derivation[step], context, i, srClassification, lrClassification,
                                        top, labelClassification);
                            }

                            break;
                        }
                        case REDUCE_RIGHT: {
                            shiftReduceClassifications++;
                            if (shiftReduceClassifier.classify(featureVector) == ParserAction.REDUCE.ordinal()) {
                                correctShiftReduceClassifications++;
                            } else {
                                missedReduces++;
                            }

                            reduceDirectionClassifications++;
                            final ScoredClassification lrClassification = reduceDirectionClassifier
                                    .scoredClassify(featureVector);
                            if (reduceDirectionClassifier.classify(featureVector) == ReduceDirection.RIGHT.ordinal()) {
                                correctReduceDirectionClassifications++;
                            }

                            final Arc top = stack.removeFirst();
                            final Arc second = stack.removeFirst();
                            second.predictedHead = top.index;
                            stack.addFirst(top);

                            final ScoredClassification labelClassification = labelClassifier != null ? labelClassifier
                                    .scoredClassify(featureVector) : null;
                            if (labelClassifier != null) {
                                top.predictedLabel = labels.getSymbol(labelClassifier.classify(featureVector));
                            }

                            if (arcScoreWriter != null) {
                                write(arcScoreWriter, derivation[step], context, i, srClassification, lrClassification,
                                        top, labelClassification);
                            }
                            break;
                        }
                        }
                    }
                } catch (final IllegalArgumentException ignore) {
                    // Ignore non-projective dependencies
                }
            }

            if (arcScoreWriter != null) {
                arcScoreWriter.close();
            }

            System.out.println(iteration + 1);

            if (BaseLogger.singleton().isLoggable(Level.FINE)) {
                test(parser, trainingExamples, "Training-set");
            }
            test(parser, devExamples, "Dev-set");

            BaseLogger.singleton().info(
                    String.format("Shift/Reduce: %d/%d (%.2f%%)", correctShiftReduceClassifications,
                            shiftReduceClassifications, 100.0 * correctShiftReduceClassifications
                                    / shiftReduceClassifications));
            BaseLogger.singleton()
                    .info(String.format("Missed Shifts: %d/%d", missedShifts, shiftReduceClassifications));
            BaseLogger.singleton().info(
                    String.format("Missed Reduces: %d/%d", missedReduces, shiftReduceClassifications));
            BaseLogger.singleton().info(
                    String.format("Reduce Direction: %d/%d (%.2f%%)", correctReduceDirectionClassifications,
                            reduceDirectionClassifications, 100.0 * correctReduceDirectionClassifications
                                    / reduceDirectionClassifications));
        }

        if (arcScores != null) {
            testClassifierScoring(parser, devExamples);
        }
    }

    private void write(final FileWriter arcScoreWriter, final DependencyGraph.StackProjectiveAction derivationStep,
            final NivreParserContext context, final int i, final ScoredClassification srClassification,
            final ScoredClassification lrClassification, final Arc top, final ScoredClassification labelClassification)
            throws IOException {

        arcScoreWriter.write(String.format(
                "%d,%.3f,%d,%d,%.3f,%d,%d,%d,%d,%d,%s,%.3f,%.3f,%s\n",
                // Shift-reduce
                derivationStep == StackProjectiveAction.SHIFT ? 0 : 1,
                derivationStep == StackProjectiveAction.SHIFT ? -srClassification.score : srClassification.score,
                srClassification.classification,

                // Left-right
                derivationStep.ordinal() - 1,
                derivationStep == StackProjectiveAction.REDUCE_LEFT ? -lrClassification.score : lrClassification.score,
                lrClassification.classification,

                // State
                top.predictedHead, top.index - top.predictedHead, context.arcs.length - i, context.arcs.length - 1,

                // Label
                labelClassification != null ? top.label : "-", labelClassification != null ? labelClassification.score
                        : 0, labelClassification != null ? labelClassification.margin : 0,
                labelClassification != null ? top.predictedLabel : "-"));
    }

    private void outputParserState(final String prefix, final NivreParserContext state, final int i) {
        final StringBuilder sb = new StringBuilder();

        sb.append(String.format("%10s | ", prefix));

        Arc a = state.stack.get(1);
        sb.append(a.pos);
        sb.append('/');
        sb.append(a.predictedLabel != null ? a.predictedLabel : "");

        sb.append("   ");
        a = state.stack.get(0);
        sb.append(a.pos);
        sb.append('/');
        sb.append(a.predictedLabel != null ? a.predictedLabel : "");

        sb.append("   ");
        a = state.arcs[i];
        sb.append(a.pos);
        sb.append('/');
        sb.append(a.predictedLabel != null ? a.predictedLabel : "");

        BaseLogger.singleton().finer(sb.toString());
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
        System.out.format("%s accuracy - unlabeled: %.3f  labeled %.3f  (%d ms, %.2f words/sec)\n", label, correctArcs
                * 1.0 / total, correctLabels * 1.0 / total, time, total * 1000.0 / time);
    }

    private void testClassifierScoring(final TransitionDepParser parser, final LinkedList<DependencyGraph> examples)
            throws IOException {

        final FileWriter arcScoreWriter = new FileWriter(arcScores);
        arcScoreWriter
                .write("sr_score,lr_gold,lr_score,lr_class,l_index,span,buf,sent_len,label_gold,label_score,label_margin,label_class\n");

        for (final DependencyGraph example : examples) {
            example.clear();

            final LinkedList<Arc> stack = new LinkedList<Arc>();

            final int totalSteps = example.size() * 2 - 1;
            for (int step = 0, i = 0; step < totalSteps; step++) {
                final BitVector featureVector = parser.featureExtractor.featureVector(new NivreParserContext(
                        stack, example.arcs, i), i);

                ParserAction action = null;
                ScoredClassification srClassification = null;
                if (stack.size() < 2) {
                    action = ParserAction.SHIFT;
                } else {
                    srClassification = parser.shiftReduceClassifier.scoredClassify(featureVector);
                    action = ParserAction.forInt(srClassification.classification);
                }

                switch (action) {
                case SHIFT:
                    stack.addFirst(example.arcs[i++]);

                    break;

                case REDUCE:
                    final ScoredClassification lrClassification = parser.reduceDirectionClassifier
                            .scoredClassify(featureVector);
                    final ReduceDirection reduceDirection = ReduceDirection.forInt(lrClassification.classification);

                    switch (reduceDirection) {
                    case LEFT: {
                        final Arc top = stack.get(0);
                        final Arc second = stack.get(1);

                        stack.removeFirst();
                        top.predictedHead = second.index;
                        top.score = lrClassification.score;
                        ScoredClassification labelClassification = null;
                        if (parser.labelClassifier != null) {
                            labelClassification = parser.labelClassifier.scoredClassify(featureVector);
                            top.predictedLabel = parser.labels.getSymbol(labelClassification.classification);
                        }

                        write(arcScoreWriter, example.arcs, i, srClassification, lrClassification, top, second,
                                labelClassification);

                        break;
                    }
                    case RIGHT: {
                        final Arc top = stack.get(0);
                        final Arc second = stack.get(1);

                        stack.removeFirst();
                        stack.removeFirst();
                        second.predictedHead = top.index;
                        second.score = lrClassification.score;
                        ScoredClassification labelClassification = null;
                        if (parser.labelClassifier != null) {
                            labelClassification = parser.labelClassifier.scoredClassify(featureVector);
                            second.predictedLabel = parser.labels.getSymbol(labelClassification.classification);
                        }
                        stack.addFirst(top);

                        write(arcScoreWriter, example.arcs, i, srClassification, lrClassification, top, second,
                                labelClassification);
                        break;
                    }
                    }
                }
            }
        }
        arcScoreWriter.close();
    }

    private void write(final FileWriter arcScoreWriter, final Arc[] arcs, final int i,
            final ScoredClassification srClassification, final ScoredClassification lrClassification, final Arc top,
            final Arc second, final ScoredClassification labelClassification) throws IOException {

        int lrGold;
        if (second.head == top.index) {
            lrGold = 0;
        } else if (top.head == second.index) {
            lrGold = 1;
        } else {
            lrGold = -1;
        }

        arcScoreWriter.write(String.format(
                "%.3f,%d,%.3f,%d,%d,%d,%d,%d,%s,%.3f,%.3f,%s\n",
                // Shift-reduce
                srClassification.score,

                // Left-right
                lrGold, lrClassification.classification == 0 ? -lrClassification.score : lrClassification.score,
                lrClassification.classification,

                // State
                top.predictedHead, top.index - top.predictedHead, arcs.length - i, arcs.length - 1,

                // Label
                labelClassification != null ? top.label : "-", labelClassification != null ? labelClassification.score
                        : 0, labelClassification != null ? labelClassification.margin : 0,
                labelClassification != null ? top.predictedLabel : "-"));
    }

    public static void main(final String[] args) {
        run(args);
    }
}
