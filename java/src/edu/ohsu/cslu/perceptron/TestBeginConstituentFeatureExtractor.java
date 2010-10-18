package edu.ohsu.cslu.perceptron;

import org.junit.Assert;
import org.junit.Test;

import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.GrammarTestCase;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.SimpleShiftFunction;
import edu.ohsu.cslu.perceptron.BeginConstituentFeatureExtractor.Sentence;

/**
 * Unit tests for {@link BeginConstituentFeatureExtractor}.
 * 
 * @author Aaron Dunlop
 * @since Oct 15, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class TestBeginConstituentFeatureExtractor {

    private final int NULL = 0;
    private final int SYSTEMS = 1;
    private final int ANALYST = 2;
    private final int ARBITRATION = 3;
    private final int CHEF = 4;
    private final int v = 6;

    // systems
    // null, null, null, systems, analyst, arbitration, chef, <tag-3 = not begin>, <tag-2 = not begin>, <tag-1 = not
    // begin>
    private final int[] SYSTEMS_ELEMENTS = new int[] { 0 + NULL, v + NULL, 2 * v + NULL, 3 * v + SYSTEMS,
            4 * v + ANALYST, 5 * v + ARBITRATION, 6 * v + CHEF, 7 * v + 1, 7 * v + 3, 7 * v + 5 };

    // analyst
    // null, null, systems, analyst, arbitration, chef, null, <tag-2 = not begin>, <tag-1 = not begin>, <systems =
    // begin>
    private final int[] ANALYST_ELEMENTS = new int[] { 0 + NULL, v + NULL, 2 * v + SYSTEMS, 3 * v + ANALYST,
            4 * v + ARBITRATION, 5 * v + CHEF, 6 * v + NULL, 7 * v + 1, 7 * v + 3, 7 * v + 4 };

    // arbitration
    // null, systems, analyst, arbitration, chef, null, null, <tag-1 = not begin>, <systems = begin>, <analyst = not
    // begin>
    private final int[] ARBITRATION_ELEMENTS = new int[] { 0 + NULL, v + SYSTEMS, 2 * v + ANALYST, 3 * v + ARBITRATION,
            4 * v + CHEF, 5 * v + NULL, 6 * v + NULL, 7 * v + 1, 7 * v + 2, 7 * v + 5 };

    // chef
    // systems, analyst, arbitration, chef, null, null, null, <systems = begin>, <analyst = not
    // begin>, <arbitration = not begin>
    private final int[] CHEF_ELEMENTS = new int[] { 0 + SYSTEMS, v + ANALYST, 2 * v + ARBITRATION, 3 * v + CHEF,
            4 * v + NULL, 5 * v + NULL, 6 * v + NULL, 7 * v + 0, 7 * v + 3, 7 * v + 5 };

    @Test
    public void testExtractFeaturesFromParsedSentence() throws Exception {
        final Grammar g = GrammarTestCase.createSimpleGrammar(Grammar.class, SimpleShiftFunction.class);
        final String parsedSentence = "(TOP (NP (NP (NP (NN systems) (NN analyst)) (NN arbitration)) (NN chef)))";

        final BeginConstituentFeatureExtractor fe = new BeginConstituentFeatureExtractor(g, 3);
        final Sentence s = fe.new Sentence(parsedSentence);

        Assert.assertArrayEquals(SYSTEMS_ELEMENTS, fe.featureVector(s, 0).elements());
        Assert.assertArrayEquals(ANALYST_ELEMENTS, fe.featureVector(s, 1).elements());
        Assert.assertArrayEquals(ARBITRATION_ELEMENTS, fe.featureVector(s, 2).elements());
        Assert.assertArrayEquals(CHEF_ELEMENTS, fe.featureVector(s, 3).elements());
    }

    @Test
    public void testExtractFeaturesFromTokens() throws Exception {
        final Grammar g = GrammarTestCase.createSimpleGrammar(Grammar.class, SimpleShiftFunction.class);
        final String tokens = "systems analyst arbitration chef";

        final BeginConstituentFeatureExtractor fe = new BeginConstituentFeatureExtractor(g, 3);
        final Sentence s = fe.new Sentence(tokens);

        final boolean[] tags = new boolean[] { true, false, false, false };
        Assert.assertArrayEquals(SYSTEMS_ELEMENTS, fe.featureVector(s, 0, tags).elements());
        Assert.assertArrayEquals(ANALYST_ELEMENTS, fe.featureVector(s, 1, tags).elements());
        Assert.assertArrayEquals(ARBITRATION_ELEMENTS, fe.featureVector(s, 2, tags).elements());
        Assert.assertArrayEquals(CHEF_ELEMENTS, fe.featureVector(s, 3, tags).elements());
    }
}
