package edu.ohsu.cslu.perceptron;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.zip.GZIPInputStream;

import cltool4j.BaseCommandlineTool;
import cltool4j.args4j.Option;
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

    @Option(name = "-overLoss")
    private float overPenalty = 1;

    @Option(name = "-underLoss")
    private float underPenalty = 1;

    @Option(name = "-feats", usage = "Feature template string: lt rt lt_lt-1 rw_rt loc ...")
    public String featTemplate = null;

    @Option(name = "-bins", usage = "Value to Class mapping bins. ex: '0,5,10,30' ")
    private String binsStr;

    // TODO: This class should take a "feature template" as input and learn a model based on that
    // example: t t-1 t-2 t+1 t+2 w w-1 w+1 t_w t_t-1 t-1_t-2 ... t=tag w=word _=joint

    @Override
    protected void run() throws Exception {
        natesTraining();
        // aaronsTraining();
    }

    public void aaronsTraining() throws IOException, ClassNotFoundException {

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

        final AveragedPerceptron model = new AveragedPerceptron();

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

                    // TODO: fix this to work with ints instead of bools
                    // model.train(goldTags[k], featureVector);

                    if (logger.isLoggable(Level.FINER)) {
                        logger.finer(toString());
                    }
                }
            }

            if (devSet != null) {
                // Test the development set
                int total = 0;
                final int correct = 0;
                InputStream is = new FileInputStream(devSet);
                if (devSet.getName().endsWith(".gz")) {
                    is = new GZIPInputStream(is);
                }
                br = new BufferedReader(new InputStreamReader(is));
                for (String line = br.readLine(); line != null; line = br.readLine()) {

                    final Sentence s = fe.new Sentence(line);
                    final int[] predictedTags = new int[s.length()];

                    for (int k = 0; k < predictedTags.length; k++) {

                        // TODO: fix this to work with ints instead of bools

                        // predictedTags[k] = model.classify(fe.featureVector(s, k, predictedTags));
                        // if (predictedTags[k] == s.goldTags()[k]) {
                        // correct++;
                        // }
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

    public void natesTraining() throws Exception {

        if (featTemplate == null) {
            logger.info("ERROR: Training a model from pre-computed features requires -feats to be non-empty");
            System.exit(1);
        }
        if (binsStr == null) {
            logger.info("ERROR: Training a model from pre-computed features requires -bins to be non-empty");
            System.exit(1);
        }

        final AveragedPerceptron perceptron = new AveragedPerceptron(alpha, new Perceptron.OverUnderLoss(overPenalty,
                underPenalty), binsStr, featTemplate, null);
        final DataSet train = new DataSet(System.in, perceptron);
        final InputStream devStream = ParserUtil.file2inputStream(devSet.getAbsolutePath());
        final DataSet dev = new DataSet(devStream, perceptron);
        trainPerceptron(perceptron, train, dev);
        System.exit(0);
    }

    // Nate's training method...
    public void trainPerceptron(final Perceptron perceptron, final DataSet train, final DataSet dev) {

        for (int i = 0; i < iterations; i++) {
            for (int j = 0; j < train.numExamples; j++) {
                final int goldClass = train.classification.get(j);
                perceptron.train(goldClass, train.features.get(j));
            }

            final float trainLoss = evalDataSetAvgLoss(train, perceptron, false);
            final float devLoss = evalDataSetAvgLoss(dev, perceptron, false);

            System.out.format("ittr=%d\t trainLoss=%.4f\t devLoss=%.4f\n", i, trainLoss, devLoss);
        }

        evalDataSetAvgLoss(train, perceptron, true);
        evalDataSetAvgLoss(dev, perceptron, true);

        System.out.print(perceptron.toString());
    }

    public float evalDataSetAvgLoss(final DataSet data, final Perceptron perceptron, final boolean confMatrix) {
        final int numClasses = perceptron.numClasses();
        final int counts[][] = new int[numClasses][numClasses];
        for (int i = 0; i < numClasses; i++) {
            for (int j = 0; j < numClasses; j++) {
                counts[i][j] = 0;
            }
        }

        float loss = 0;
        for (int i = 0; i < data.numExamples; i++) {
            final int goldClass = data.classification.get(i);
            final int guessClass = perceptron.classify(data.features.get(i));

            // System.out.println("gold=" + goldClass + " guess=" + guessClass);

            loss += perceptron.computeLoss(goldClass, guessClass);
            counts[goldClass][guessClass] += 1;
        }

        if (confMatrix) {
            String s = "** Y=guess  X=gold **\n";
            int correct = 0;
            for (int y = 0; y < numClasses; y++) {
                int rowTotal = 0;
                for (int x = 0; x < numClasses; x++) {
                    s += "\t" + counts[x][y];
                    rowTotal += counts[x][y];
                }
                s += String.format("\tacc=%.2f\n", (float) counts[y][y] / rowTotal);
                correct += counts[y][y];
            }
            s += String.format("totalAcc=%.2f\n", (float) correct / data.numExamples);
            System.out.println(s);
        }

        return loss / data.numExamples;
    }

    public class DataSet {
        // parallel ArrayLists: a gold class (classification) for each feature vector (features)
        public ArrayList<SparseBitVector> features = new ArrayList<SparseBitVector>();
        public ArrayList<Integer> classification = new ArrayList<Integer>();
        public int numExamples;
        public int numFeatures = -1;

        public DataSet(final InputStream is) throws Exception {
            readDataSet(is, null);
            // readOldFormat(is);
        }

        public DataSet(final InputStream is, final Perceptron perceptron) throws Exception {
            readDataSet(is, perceptron);
        }

        /**
         * 
         * Expected format (goldClass : featLen posFeat1 posFeat2 ...) 9 : 98695 0 38 68 136 179 237 1067 2684 2714 2782
         * 2825 2883 7348 30622 55242 95299 0 : 98695 0 19 87 117 185 228 1342 2665 2733 2763 2831 2874 23458 31295
         * 71352 79105 2 : 98695 0 38 68 136 166 234 430 2684 2714 2782 2812 2880 7264 47405 55158 95299 ....
         */
        private void readDataSet(final InputStream is, final Perceptron perceptron) throws Exception {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            numExamples = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final String[] tokens = ParserUtil.tokenize(line);
                assert numFeatures == tokens.length - 1 || numFeatures == -1;
                numFeatures = Integer.parseInt(tokens[2]);
                final int numPosFeats = tokens.length - 3;
                numExamples++;

                int goldClass = new Integer(tokens[0]);
                if (perceptron != null) {
                    goldClass = perceptron.value2class(goldClass);
                }
                classification.add(goldClass);

                final int feats[] = new int[numPosFeats];
                for (int i = 0; i < numPosFeats; i++) {
                    feats[i] = new Integer(tokens[i + 3]);
                }
                features.add(new SparseBitVector(numFeatures, feats));
            }
        }

        public void readOldFormat(final InputStream is) throws Exception {
            final BufferedReader br = new BufferedReader(new InputStreamReader(is));
            // Expected format: classification featVal1 featVal2 featVal3 ...
            numExamples = 0;
            for (String line = br.readLine(); line != null; line = br.readLine()) {
                final String[] tokens = ParserUtil.tokenize(line);
                assert numFeatures == tokens.length - 1 || numFeatures == -1;
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

    // // TODO: I'm guessing this is all temporary? I don't understand a toString() method here
    // // except for debugging ...
    // @Override
    // public String toString() {
    // final StringBuilder sb = new StringBuilder();
    // // Tokens (in markov window)
    // for (int i = 0; i < markovOrder * 2 + 1; i++) {
    // for (int j = 0; j < grammar.numLexSymbols(); j++) {
    // final int feature = grammar.numLexSymbols() * i + j;
    // // final float weight = model.averagedFeatureWeight(feature, example);
    // final float weight = model.averagedPerceptron().getFloat(feature);
    // if (weight != 0) {
    // sb.append(String.format("_w_%d_%s : %.2f\n", i - markovOrder, grammar.mapLexicalEntry(j), weight));
    // }
    // }
    // }
    // // Previous tags
    // for (int i = 0; i < markovOrder * 2; i = i + 2) {
    // final int trueFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i;
    // // final float trueWeight = model.averagedFeatureWeight(trueFeature, example);
    // final float trueWeight = model.averagedPerceptron().getFloat(trueFeature);
    // sb.append(String.format("_t_%d_T : %.2f\n", i / 2 - markovOrder, trueWeight));
    //
    // final int falseFeature = grammar.numLexSymbols() * (markovOrder * 2 + 1) + i + 1;
    // // final float falseWeight = model.averagedFeatureWeight(falseFeature, example);
    // final float falseWeight = model.averagedPerceptron().getFloat(falseFeature);
    // sb.append(String.format("_t_%d_F : %.2f\n", i / 2 - markovOrder, falseWeight));
    // }
    // return sb.toString();
    // }

    public static void main(final String[] args) {
        run(args);
    }
}
