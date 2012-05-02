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
import edu.ohsu.cslu.dep.DependencyGraph.Action;
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

    @Override
    protected void run() throws Exception {

        //
        // Read each training instance
        //
        final LinkedList<DependencyGraph> trainingExamples = new LinkedList<DependencyGraph>();
        for (final BufferedReader br = inputAsBufferedReader(); br.ready();) {
            trainingExamples.add(DependencyGraph.readConll(br));
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
        for (final DependencyGraph example : trainingExamples) {
            for (int i = 0; i < example.arcs.length; i++) {
                final Arc arc = example.arcs[i];
                tokens.addSymbol(arc.token);

                // Add an entry for the UNK label as well
                tokens.addSymbol(Tokenizer.berkeleyGetSignature(arc.token, i == 0, tokens));
                pos.addSymbol(arc.coarsePos);
            }
        }

        final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor(tokens, pos);

        // At each step, we have 3 possible actions (shift, reduce-left, reduce-right)
        final AveragedPerceptron classifier = new AveragedPerceptron(3, fe.featureCount());

        //
        // Iterate through the training instances
        //
        for (int iteration = 0, examples = 0; iteration < trainingIterations; iteration++, examples = 0) {
            for (final DependencyGraph example : trainingExamples) {
                try {
                    final DependencyGraph.Action[] derivation = example.derivation();

                    final Arc[] arcs = example.arcs;
                    final LinkedList<Arc> stack = new LinkedList<Arc>();
                    final NivreParserContext context = new NivreParserContext(stack, arcs);

                    for (int step = 0, i = 0; step < derivation.length; step++) {
                        final SparseBitVector featureVector = fe.forwardFeatureVector(context, i);

                        switch (derivation[step]) {
                        case SHIFT:
                            classifier.train(Action.SHIFT.ordinal(), featureVector);
                            stack.addFirst(arcs[i++]);
                            break;
                        case REDUCE_LEFT:
                            classifier.train(Action.REDUCE_LEFT.ordinal(), featureVector);
                            stack.removeFirst();
                            break;

                        case REDUCE_RIGHT:
                            classifier.train(Action.REDUCE_RIGHT.ordinal(), featureVector);
                            final Arc tmp = stack.removeFirst();
                            stack.removeFirst();
                            stack.addFirst(tmp);
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
                test(trainingExamples, "Training-set", classifier, tokens, pos);
            }
            if (devExamples != null) {
                test(devExamples, "Dev-set", classifier, tokens, pos);
            }
        }

        final ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(outputModelFile));
        oos.writeObject(classifier);
        oos.writeObject(tokens);
        oos.writeObject(pos);
        oos.close();
    }

    private void test(final LinkedList<DependencyGraph> examples, final String label,
            final AveragedPerceptron classifier, final SymbolSet<String> tokens, final SymbolSet<String> pos) {
        int correct = 0, total = 0;
        for (final DependencyGraph example : examples) {
            total += example.size() - 1;
            final DependencyGraph parse = parse(example, classifier, tokens, pos);
            for (int i = 0; i < example.size() - 1; i++) {
                if (parse.arcs[i].head == example.arcs[i].head) {
                    correct++;
                }
            }
        }
        System.out.format("%s accuracy: %.2f\n", label, correct * 1.0 / total);
    }

    public static void main(final String[] args) {
        run(args);
    }
}
