package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import org.kohsuke.args4j.Option;

import cltool.BaseCommandlineTool;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.perceptron.BeginConstituentFeatureExtractor.Sentence;

/**
 * Trains a perceptron model from a corpus. Features and objective tags are derived using a {@link FeatureExtractor}.
 * 
 * @author Aaron Dunlop
 * @since Oct 9, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class ModelTrainer extends BaseCommandlineTool {

    @Option(name = "-g", metaVar = "file", required = true, usage = "Grammar file")
    private File grammarFile;
    private Grammar grammar;

    @Option(name = "-i", aliases = { "--iterations" }, metaVar = "count", usage = "Iterations over training corpus")
    private int iterations = 5;

    @Option(name = "-o", aliases = { "--order" }, metaVar = "order", usage = "Markov Order")
    private int markovOrder = 2;

    @Option(name = "-a", aliases = { "--alpha" }, metaVar = "value", usage = "Update step size (alpha)")
    private float alpha = 0.1f;

    @Option(name = "-d", metaVar = "file", usage = "Development set. If specified, test results are output after each training iteration.")
    private File devSet;

    @Option(name = "-s", metaVar = "file", usage = "Output model file (Java Serialized Object)")
    private File outputModelFile;

    private PerceptronModel model;
    private int example = 0;

    @Override
    protected void run() throws Exception {
        // Read grammar
        InputStream is = new FileInputStream(grammarFile);
        if (grammarFile.getName().endsWith(".gz")) {
            is = new GZIPInputStream(is);
        }
        grammar = Grammar.read(is);
        is.close();

        final ArrayList<SparseBitVector[]> trainingCorpusFeatures = new ArrayList<SparseBitVector[]>();
        final ArrayList<boolean[]> trainingCorpusGoldTags = new ArrayList<boolean[]>();

        // TODO Generalize to use other feature extractors
        final BeginConstituentFeatureExtractor fe = new BeginConstituentFeatureExtractor(grammar, markovOrder);

        // Read in the training corpus and map each token
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        for (String line = br.readLine(); line != null; line = br.readLine()) {
            final Sentence s = fe.new Sentence(line);
            final SparseBitVector[] featureVectors = s.featureVectors();
            trainingCorpusFeatures.add(featureVectors);
            trainingCorpusGoldTags.add(s.goldTags());
        }
        br.close();

        model = new PerceptronModel(fe.featureCount(), 0f);

        // Iterate over training corpus
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final SparseBitVector[] featureVectors = trainingCorpusFeatures.get(j);
                final boolean[] goldTags = trainingCorpusGoldTags.get(j);

                for (int k = 0; k < featureVectors.length; k++) {
                    example++;
                    final SparseBitVector featureVector = featureVectors[k];
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(featureVectorToString(featureVector));
                    }
                    final boolean predictedTag = model.rawBinaryOutput(featureVector);

                    if (predictedTag != goldTags[k]) {
                        // Perform (averaged) perceptron update if the predicted tag was incorrect
                        if (goldTags[k]) {
                            model.update(featureVector, alpha, example);
                        } else {
                            model.update(featureVector, -alpha, example);
                        }
                    }

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(toString());
                    }
                }
            }

            if (devSet != null) {
                model.updateAveragedModel(example);
                // Test the development set
                int total = 0, correct = 0;
                is = new FileInputStream(devSet);
                if (devSet.getName().endsWith(".gz")) {
                    is = new GZIPInputStream(is);
                }
                br = new BufferedReader(new InputStreamReader(is));
                for (String line = br.readLine(); line != null; line = br.readLine()) {

                    final Sentence s = fe.new Sentence(line);
                    final boolean[] predictedTags = new boolean[s.length()];

                    for (int k = 0; k < predictedTags.length; k++) {
                        predictedTags[k] = model.averagedBinaryOutput(fe.featureVector(s, k, predictedTags));
                        if (predictedTags[k] == s.goldTags()[k]) {
                            correct++;
                        }
                        total++;
                    }
                }
                System.out.format("Iteration=%d DevsetAccuracy=%.2f\n", i, correct * 100f / total);
                br.close();
            }
        }

        // Write out the model file
        if (outputModelFile != null) {

        }
    }

    public String featureVectorToString(final SparseBitVector featureVector) {
        final StringBuilder sb = new StringBuilder();
        for (int i = 0; i < markovOrder * 2 + 1; i++) {
            for (int j = 0; j < grammar.numLexSymbols(); j++) {
                final int feature = grammar.numLexSymbols() * i + j;
                if (featureVector.getBoolean(feature)) {
                    sb.append(String.format("_w_%d_%s\n", i - markovOrder, grammar.mapLexicalEntry(j)));
                }
            }
        }
        // Previous tags
        for (int i = 0; i < markovOrder * 2; i = i + 2) {
            final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
            sb.append(String.format("_t_%d_T : %s\n", i / 2 - markovOrder, featureVector.getBoolean(trueFeature) ? "T"
                    : "F"));
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        // Tokens (in markov window)
        for (int i = 0; i < markovOrder * 2 + 1; i++) {
            for (int j = 0; j < grammar.numLexSymbols(); j++) {
                final int feature = grammar.numLexSymbols() * i + j;
                final float weight = model.averagedFeatureWeight(feature, example);
                if (weight != 0) {
                    sb.append(String.format("_w_%d_%s : %.2f\n", i - markovOrder, grammar.mapLexicalEntry(j), weight));
                }
            }
        }
        // Previous tags
        for (int i = 0; i < markovOrder * 2; i = i + 2) {
            final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
            final float trueWeight = model.averagedFeatureWeight(trueFeature, example);
            sb.append(String.format("_t_%d_T : %.2f\n", i / 2 - markovOrder, trueWeight));

            final int falseFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i + 1;
            final float falseWeight = model.averagedFeatureWeight(falseFeature, example);
            sb.append(String.format("_t_%d_F : %.2f\n", i / 2 - markovOrder, falseWeight));
        }
        return sb.toString();
    }

    public static void main(final String[] args) {
        run(args);
    }
}
