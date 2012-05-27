package edu.ohsu.cslu.dep;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.util.LinkedList;
import java.util.logging.Level;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.perceptron.AveragedPerceptron;

/**
 * Trains a perceptron classifier for greedy dependency parsing.
 * 
 * Input: Dependency treebank in CoNLL 2007 format
 * 
 * Output: Training status and training/dev-set accuracies
 * 
 * The model (3 averaged perceptrons, vocabulary and lexicon) will be serialized to the specified model file.
 */
public class TrainDepParser extends BaseDepParser {

    @Option(name = "-i", metaVar = "count", usage = "Training iterations")
    private int trainingIterations = 10;

    @Option(name = "-m", required = true, metaVar = "file", usage = "Output model file")
    private File outputModelFile;

    @Option(name = "-d", metaVar = "file", usage = "Development set in CoNLL 2007 format")
    private File devSet;

    @Option(name = "-f", metaVar = "file", usage = "Development set in CoNLL 2007 format")
    private String featureTemplates = "st2,st1,it1,it2,it3,it4,sw2,sw1,iw1,iw2";

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
                g.derivation();
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
        final SymbolSet<String> pos = new SymbolSet<String>();
        final SymbolSet<String> labels = new SymbolSet<String>();

        for (final DependencyGraph example : trainingExamples) {
            for (int i = 0; i < example.arcs.length; i++) {
                final Arc arc = example.arcs[i];
                tokens.addSymbol(arc.token);

                // Add an entry for the UNK label as well
                tokens.addSymbol(Tokenizer.berkeleyGetSignature(arc.token, i == 0, tokens));
                pos.addSymbol(arc.pos);
                labels.addSymbol(arc.label);
            }
        }

        final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor(featureTemplates, tokens, pos);

        // At each step, we have 3 possible actions (shift, reduce-left, reduce-right), but we divide them into 2
        // classifiers - one to decide between shift and reduce, and one to select reduce direction. For the moment, we
        // use the same feature-set for both.
        final AveragedPerceptron shiftReduceClassifier = new AveragedPerceptron(2, fe.featureCount());
        final AveragedPerceptron reduceDirectionClassifier = new AveragedPerceptron(2, fe.featureCount());
        // We also attempt to label arcs, with a third classifier
        final AveragedPerceptron labelClassifier = new AveragedPerceptron(labels.size(), fe.featureCount());

        //
        // Iterate through the training instances
        //
        for (int iteration = 0, examples = 0; iteration < trainingIterations; iteration++, examples = 0) {
            for (final DependencyGraph example : trainingExamples) {
                try {
                    final DependencyGraph.DerivationAction[] derivation = example.derivation();

                    final Arc[] arcs = example.arcs;
                    final LinkedList<Arc> stack = new LinkedList<Arc>();
                    final NivreParserContext context = new NivreParserContext(stack, arcs);

                    for (int step = 0, i = 0; step < derivation.length; step++) {
                        final SparseBitVector featureVector = fe.forwardFeatureVector(context, i);

                        switch (derivation[step]) {

                        case SHIFT:
                            if (stack.size() >= 2) {
                                shiftReduceClassifier.train(ParserAction.SHIFT.ordinal(), featureVector);
                            }
                            stack.addFirst(arcs[i++]);
                            break;

                        case REDUCE_LEFT:
                            shiftReduceClassifier.train(ParserAction.REDUCE.ordinal(), featureVector);
                            reduceDirectionClassifier.train(ReduceDirection.LEFT.ordinal(), featureVector);
                            final Arc right = stack.removeFirst();

                            labelClassifier.train(labels.getIndex(right.label), featureVector);
                            break;

                        case REDUCE_RIGHT:
                            shiftReduceClassifier.train(ParserAction.REDUCE.ordinal(), featureVector);
                            reduceDirectionClassifier.train(ReduceDirection.RIGHT.ordinal(), featureVector);

                            final Arc tmp = stack.removeFirst();
                            final Arc left = stack.removeFirst();
                            stack.addFirst(tmp);

                            labelClassifier.train(labels.getIndex(left.label), featureVector);
                            break;
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
                test(trainingExamples, "Training-set", fe, shiftReduceClassifier, reduceDirectionClassifier,
                        labelClassifier, tokens, pos, labels);
            }
            if (devExamples != null) {
                test(devExamples, "Dev-set", fe, shiftReduceClassifier, reduceDirectionClassifier, labelClassifier,
                        tokens, pos, labels);
            }
        }

        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModelFile));
        oos.writeObject(fe);
        oos.writeObject(shiftReduceClassifier);
        oos.writeObject(reduceDirectionClassifier);
        oos.writeObject(labelClassifier);
        oos.writeObject(tokens);
        oos.writeObject(pos);
        oos.writeObject(labels);
        oos.close();
    }

    private void test(final LinkedList<DependencyGraph> examples, final String label,
            final NivreParserFeatureExtractor featureExtractor, final AveragedPerceptron shiftReduceClassifier,
            final AveragedPerceptron reduceDirectionClassifier, final AveragedPerceptron labelClassifier,
            final SymbolSet<String> tokens, final SymbolSet<String> pos, final SymbolSet<String> labels) {

        final long startTime = System.currentTimeMillis();

        int correctArcs = 0, correctLabels = 0, total = 0;
        for (final DependencyGraph example : examples) {
            total += example.size() - 1;
            int sentenceCorrect = 0;
            float sentenceScore = 0f;
            final DependencyGraph parse = parse(example, featureExtractor, shiftReduceClassifier,
                    reduceDirectionClassifier, labelClassifier, tokens, pos, labels);
            for (int i = 0; i < example.size() - 1; i++) {
                if (parse.arcs[i].head == example.arcs[i].head) {
                    correctArcs++;
                    sentenceCorrect++;
                    sentenceScore += parse.arcs[i].score;

                    if (parse.arcs[i].label.equals(example.arcs[i].label)) {
                        correctLabels++;
                    }
                } else {
                    sentenceScore -= parse.arcs[i].score;
                }
            }
            BaseLogger.singleton().finer(
                    String.format("%.3f %.3f", sentenceCorrect * 1.0 / (example.size() - 1), sentenceScore));
        }
        final long time = System.currentTimeMillis() - startTime;
        System.out.format("%s accuracy: %.3f unlabeled %.3f labeled (%d ms, %.2f words/sec)\n", label, correctArcs
                * 1.0 / total, correctLabels * 1.0 / total, time, total * 1000.0 / time);
    }

    public static void main(final String[] args) {
        run(args);
    }
}
