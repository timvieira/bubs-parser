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
import edu.ohsu.cslu.parser.ParserUtil;
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

    @Option(name = "-g", metaVar = "file", usage = "Grammar file")
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

    @Option(name = "-generic")
    private boolean generic = false;

    @Option(name = "-fn")
    private float falseNegative = 0;

    private PerceptronModel model;

    // private int example = 0;

    @Override
    protected void run() throws Exception {

        if (generic) {
            final DataSet train = new DataSet(System.in);
            final InputStream devStream = ParserUtil.file2inputStream(devSet.getAbsolutePath());
            final DataSet dev = new DataSet(devStream);
            trainPerceptron(train, dev);
            System.exit(0);
        }

        // Read grammar
        grammar = Grammar.read(grammarFile.getName());

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

        model = new PerceptronModel(fe.featureCount(), 0f, alpha, new PerceptronModel.ZeroOneLoss());

        // Iterate over training corpus
        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < trainingCorpusFeatures.size(); j++) {
                final SparseBitVector[] featureVectors = trainingCorpusFeatures.get(j);
                final boolean[] goldTags = trainingCorpusGoldTags.get(j);

                for (int k = 0; k < featureVectors.length; k++) {
                    // example++;
                    final SparseBitVector featureVector = featureVectors[k];
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.finest(featureVectorToString(featureVector));
                    }
                    model.train(goldTags[k], featureVector);

                    // final boolean predictedTag = model.rawBinaryOutput(featureVector);
                    //
                    // if (predictedTag != goldTags[k]) {
                    // // Perform (averaged) perceptron update if the predicted tag was incorrect
                    // if (goldTags[k]) {
                    // model.update(featureVector, alpha, example);
                    // } else {
                    // model.update(featureVector, -alpha, example);
                    // }
                    // }

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(toString());
                    }
                }
            }

            if (devSet != null) {
                model.updateAveragedModel();
                // Test the development set
                int total = 0, correct = 0;
                InputStream is = new FileInputStream(devSet);
                if (devSet.getName().endsWith(".gz")) {
                    is = new GZIPInputStream(is);
                }
                br = new BufferedReader(new InputStreamReader(is));
                for (String line = br.readLine(); line != null; line = br.readLine()) {

                    final Sentence s = fe.new Sentence(line);
                    final boolean[] predictedTags = new boolean[s.length()];

                    for (int k = 0; k < predictedTags.length; k++) {
                        predictedTags[k] = model.classifyAverage(fe.featureVector(s, k, predictedTags));
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

    public void trainPerceptron(final DataSet train, final DataSet dev) {
        if (falseNegative > 0) {
            model = new PerceptronModel(train.numFeatures, 0f, alpha, new PerceptronModel.BeamPredictLoss(
                    falseNegative, 1));
        } else {
            model = new PerceptronModel(train.numFeatures, 0f, alpha, new PerceptronModel.ZeroOneLoss());
        }

        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < train.numExamples; j++) {
                final boolean goldClass = train.classification.get(j) == 1f;
                model.train(goldClass, train.features.get(j));

                // System.out.println("raw=" + floatvec2str(model.rawPerceptron()) + "\t avg="
                // + floatvec2str(model.averagedPerceptron()));
            }

            model.updateAveragedModel();
            final float trainLoss = evalDataSetAvgLoss(train, model, false);
            final float devLoss = evalDataSetAvgLoss(dev, model, false);

            System.out.format("ittr=%d\t trainLoss=%.4f\t devLoss=%.4f\n", i, trainLoss, devLoss);
        }
        System.out.print("avgPerceptron=" + model.averagedPerceptron().toString());
        evalDataSetAvgLoss(train, model, true);
        evalDataSetAvgLoss(dev, model, true);

    }

    // private String floatvec2str(final FloatVector v) {
    // String s = "";
    // for (int i = 0; i < v.length(); i++) {
    // s += String.format("%.3f ", v.getFloat(i));
    // }
    // return s;
    // }

    public float evalDataSetAvgLoss(final DataSet data, final PerceptronModel perceptron, final boolean confMatrix) {
        final int numClasses = perceptron.numClasses();
        final int counts[][] = new int[numClasses][numClasses];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                counts[i][j] = 0;
            }
        }

        float loss = 0;
        for (int i = 0; i < data.numExamples; i++) {
            final boolean goldClass = data.classification.get(i) == 1f;
            final boolean guessClass = model.classifyAverage(data.features.get(i));
            // final boolean guessClass = model.classifyRaw(data.features.get(i));

            loss += perceptron.computeLoss(goldClass, guessClass);

            counts[bool2int(goldClass)][bool2int(guessClass)] += 1;
        }

        if (confMatrix) {
            final int tn = counts[0][0], fp = counts[0][1];
            final int fn = counts[1][0], tp = counts[1][1];
            String s = "** Y=gold  X=guess **\n";
            s += String.format("\t%d\t%d\tacc=%.2f\n", tn, fp, tn / (float) (tn + fp) * 100);
            s += String.format("\t%d\t%d\tacc=%.2f\n", fn, tp, tp / (float) (fn + tp) * 100);
            s += String.format("totalAcc=%.2f\n", (tn + tp) / (float) (tn + fp + fn + tp) * 100);
            System.out.println(s);
        }

        return loss / data.numExamples;
    }

    private int bool2int(final boolean val) {
        if (val == true) {
            return 1;
        }
        return 0;
    }

    public class DataSet {
        public ArrayList<SparseBitVector> features = new ArrayList<SparseBitVector>();
        public ArrayList<Integer> classification = new ArrayList<Integer>();
        public int numExamples;
        public int numFeatures = -1;

        public DataSet(final InputStream is) throws Exception {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            // Expected format: classification featVal1 featVal2 featVal3 ...
            numExamples = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final String[] tokens = ParserUtil.tokenize(line);
                assert numFeatures == -1 || numFeatures == tokens.length - 1;
                numFeatures = tokens.length - 1;
                numExamples++;

                classification.add(new Integer(tokens[0]));

                final boolean feats[] = new boolean[numFeatures];
                for (int i = 0; i < numFeatures; i++) {
                    final float val = Float.parseFloat(tokens[i + 1]); // +1 because offset from classification number
                    if (val == 0.0) {
                        feats[i] = false;
                    } else if (val == 1.0) {
                        feats[i] = true;
                    } else {
                        throw new Exception("ERROR: expecting binary 0/1 values in feature vector but found '"
                                + tokens[i] + "'");
                    }
                }
                features.add(new SparseBitVector(feats));
            }
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

    // TODO: I'm guessing this is all temporary? I don't understand a toString() method here
    // except for debugging ...
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        // Tokens (in markov window)
        for (int i = 0; i < markovOrder * 2 + 1; i++) {
            for (int j = 0; j < grammar.numLexSymbols(); j++) {
                final int feature = grammar.numLexSymbols() * i + j;
                // final float weight = model.averagedFeatureWeight(feature, example);
                final float weight = model.averagedPerceptron().getFloat(feature);
                if (weight != 0) {
                    sb.append(String.format("_w_%d_%s : %.2f\n", i - markovOrder, grammar.mapLexicalEntry(j), weight));
                }
            }
        }
        // Previous tags
        for (int i = 0; i < markovOrder * 2; i = i + 2) {
            final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
            // final float trueWeight = model.averagedFeatureWeight(trueFeature, example);
            final float trueWeight = model.averagedPerceptron().getFloat(trueFeature);
            sb.append(String.format("_t_%d_T : %.2f\n", i / 2 - markovOrder, trueWeight));

            final int falseFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i + 1;
            // final float falseWeight = model.averagedFeatureWeight(falseFeature, example);
            final float falseWeight = model.averagedPerceptron().getFloat(falseFeature);
            sb.append(String.format("_t_%d_F : %.2f\n", i / 2 - markovOrder, falseWeight));
        }
        return sb.toString();
    }

    public static void main(final String[] args) {
        run(args);
    }
}
