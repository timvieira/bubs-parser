package edu.ohsu.cslu.parser.fom;

import java.io.BufferedReader;
import java.util.Arrays;

import edu.ohsu.cslu.classifier.Perceptron;
import edu.ohsu.cslu.grammar.ArrayGrammar;
import edu.ohsu.cslu.grammar.GrammarByLeftNonTermList;
import edu.ohsu.cslu.parser.ChartEdgeWithFOM;
import edu.ohsu.cslu.parser.ChartParser;
import edu.ohsu.cslu.parser.ECPCellCrossList;
import edu.ohsu.cslu.parser.traversal.ChartTraversal;
import edu.ohsu.cslu.parser.util.Log;
import edu.ohsu.cslu.parser.util.ParseTree;
import edu.ohsu.cslu.parser.util.ParserUtil;

public class WeightedFeatures extends EdgeFOM {

    private ArrayGrammar grammar;
    private int numFeatures;
    private Perceptron model;

    public WeightedFeatures(final ArrayGrammar grammar) {
        this.grammar = grammar;
        this.numFeatures = 4;
    }

    @Override
    public float calcFOM(final ChartEdgeWithFOM edge) {
        final double[] featVector = getFeatureVector(edge);
        return 0; // model.score(featVector); // we want a regressor here, not a classifier
    }

    public double[] getFeatureVector(final ChartEdgeWithFOM edge) {
        final double[] feats = new double[numFeatures];
        Arrays.fill(feats, 0.0);

        // TODO: extract features from edge

        return feats;
    }

    @Override
    public void train(final BufferedReader inStream) throws Exception {
        ParseTree goldTree;
        String line;
        final ChartParser parser = new ECPCellCrossList((GrammarByLeftNonTermList) grammar, ChartTraversal.ChartTraversalType.LeftRightBottomTopTraversal);

        while ((line = inStream.readLine()) != null) {
            goldTree = ParseTree.readBracketFormat(line);
            if (goldTree.isBinaryTree() == false) {
                Log.info(0, "ERROR: Training trees must be binarized exactly as used in decoding");
                System.exit(1);
            }

            // fill chart
            parser.findBestParse(ParserUtil.join(goldTree.getLeafNodesContent(), " "));

            // goldTree.linkLeavesLeftRight();
            for (final ParseTree node : goldTree.preOrderTraversal()) {

            }

        }
    }

}
