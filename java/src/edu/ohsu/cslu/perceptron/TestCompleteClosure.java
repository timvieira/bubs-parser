/*
 * Copyright 2010-2012 Aaron Dunlop and Nathan Bodenstab
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
 * along with the BUBS Parser. If not, see <http://www.gnu.org/licenses/>
 */

package edu.ohsu.cslu.perceptron;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.narytree.NaryTree.Binarization;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.parser.chart.Chart;
import edu.ohsu.cslu.perceptron.BinaryClassifier.BinaryClassifierResult;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Tests {@link CompleteClosureSequence} and {@link ConstituentBoundaryFeatureExtractor}.
 */
public class TestCompleteClosure {

    private String trainingCorpus;

    private SymbolSet<String> lexicon = new SymbolSet<String>();
    private SymbolSet<String> unkClassSet = new SymbolSet<String>();
    private SymbolSet<String> vocabulary = new SymbolSet<String>();

    private ArrayList<CompleteClosureSequence> trainingCorpusSequences = null;

    @SuppressWarnings("unused")
    private int nullTag, dtTag, nnTag, prpTag, rpTag, rbrTag, vbpTag, commaTag, periodTag, nullToken, thisToken,
            timeToken, aroundToken, commaToken, theyToken, reToken, fasterToken, periodToken, nullUnk, thisUnk,
            timeUnk, aroundUnk, commaUnk, theyUnk, reUnk, fasterUnk, periodUnk;

    @Before
    public void setUp() {

        lexicon.addSymbol(Grammar.nullSymbolStr);
        unkClassSet.addSymbol(Grammar.nullSymbolStr);
        vocabulary.addSymbol(Grammar.nullSymbolStr);

        final StringBuilder sb = new StringBuilder();
        sb.append("(ROOT (S (NP (NP (DT This) (NN time)) (ADVP (RP around))) (, ,) (NP (PRP they)) (VP (VBP 're) (VP (VBG moving) (ADVP (RB even) (RBR faster)))) (. .)))");
        trainingCorpus = sb.toString();

        trainingCorpusSequences = new ArrayList<CompleteClosureSequence>();
        for (final String line : trainingCorpus.split("\n")) {
            trainingCorpusSequences.add(new CompleteClosureSequence(line, Binarization.LEFT, lexicon, unkClassSet,
                    vocabulary));
        }

        nullTag = vocabulary.getIndex(Grammar.nullSymbolStr);
        dtTag = vocabulary.getIndex("DT");
        nnTag = vocabulary.getIndex("NN");
        rpTag = vocabulary.getIndex("RP");
        prpTag = vocabulary.getIndex("PRP");
        vbpTag = vocabulary.getIndex("VBP");
        rbrTag = vocabulary.getIndex("RBR");
        commaTag = vocabulary.getIndex(",");
        periodTag = vocabulary.getIndex(".");

        nullToken = lexicon.getIndex(Grammar.nullSymbolStr);
        thisToken = lexicon.getIndex("This");
        timeToken = lexicon.getIndex("time");
        aroundToken = lexicon.getIndex("around");
        commaToken = lexicon.getIndex(",");
        theyToken = lexicon.getIndex("they");
        reToken = lexicon.getIndex("'re");
        fasterToken = lexicon.getIndex("faster");
        periodToken = lexicon.getIndex(".");

        nullUnk = unkClassSet.getIndex(Grammar.nullSymbolStr);
        thisUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("This", true, lexicon));
        timeUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("time", false, lexicon));
        aroundUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("around", false, lexicon));
        commaUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(",", false, lexicon));
        theyUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("they", false, lexicon));
        reUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("re", false, lexicon));
        fasterUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("faster", false, lexicon));
        periodUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature(".", false, lexicon));
    }

    @Test
    public void testSymbolSets() {
        // tagSet includes <null>; lexicon includes <null> and UNK classes
        assertEquals(11, vocabulary.size());
        assertEquals(11, lexicon.size());
        assertEquals(6, unkClassSet.size());
    }

    @Test
    public void testUnigramTagFeatures() {

        // A trivially simple feature extractor
        final ConstituentBoundaryFeatureExtractor<CompleteClosureSequence> fe = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(
                "ltm1,lt,rt,rtp1,rtp2", lexicon, unkClassSet, vocabulary, true);
        final int offset1 = vocabulary.size();
        final int offset2 = vocabulary.size() * 2;
        final int offset3 = vocabulary.size() * 3;
        final int offset4 = vocabulary.size() * 4;

        // "This time around" (tests beginning-of-sentence)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag, offset1 + dtTag, offset2 + rpTag,
                offset3 + commaTag, offset4 + prpTag }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(0, 3, 10, true)));

        // " 're moving even faster" (tests middle and end-of-sentence features
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { prpTag, offset1 + vbpTag,
                offset2 + rbrTag, offset3 + periodTag, offset4 + nullTag }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(5, 9, 10, true)));
    }

    @Test
    public void testBigramWordFeatures() {

        final ConstituentBoundaryFeatureExtractor<CompleteClosureSequence> fe = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(
                "lwm1_lw,rw_rwp1", lexicon, unkClassSet, vocabulary, true);
        final int offset1 = lexicon.size() * lexicon.size();

        // "This time around" (tests beginning-of-sentence features)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullToken * lexicon.size() + thisToken,
                offset1 + aroundToken * lexicon.size() + commaToken }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(0, 3, 10, true)));

        // " 're moving even faster " (tests middle and end-of-sentence features)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { theyToken * lexicon.size() + reToken,
                offset1 + fasterToken * lexicon.size() + periodToken }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(5, 9, 10, true)));
    }

    @Test
    public void testSpanFeatures() {

        final ConstituentBoundaryFeatureExtractor<CompleteClosureSequence> fe = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(
                "lt,s2,s3,s4,s5,s10,s20,s30,s40,s50,rs2,rs4,rs6,rs8,rs10", lexicon, unkClassSet, vocabulary, true);

        final int sOffset = vocabulary.size();
        final int rsOffset = sOffset + 9 * 2;

        // "time around" (short-span features)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nnTag,

        sOffset + 1, // s2 - true
                sOffset + 2, // s3 - false
                sOffset + 4, // s4 - false
                sOffset + 6, // s5 - false
                sOffset + 8, // s10 - false
                sOffset + 10, // s20 - false
                sOffset + 12, // s30 - false
                sOffset + 14, // s40 - false
                sOffset + 16, // s50 - false
                rsOffset + 1, // rs2 - true
                rsOffset + 2, // rs4 - false
                rsOffset + 4, // rs6 - false
                rsOffset + 6, // rs8 - false
                rsOffset + 8 // rs10 - false
                }), fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(1, 3, 10, true)));

        // " 're moving even faster " (longer-span features)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { vbpTag,

        sOffset + 1, // s2 - true
                sOffset + 3, // s3 - true
                sOffset + 5, // s4 - true
                sOffset + 6, // s5 - false
                sOffset + 8, // s10 - false
                sOffset + 10, // s20 - false
                sOffset + 12, // s30 - false
                sOffset + 14, // s40 - false
                sOffset + 16, // s50 - false
                rsOffset + 1, // rs2 - true
                rsOffset + 3, // rs4 - true
                rsOffset + 4, // rs6 - false
                rsOffset + 6, // rs8 - false
                rsOffset + 8 // rs10 - false
                }), fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(5, 9, 10, true)));
    }

    @Test
    public void testUnkFeatures() {
        final ConstituentBoundaryFeatureExtractor<CompleteClosureSequence> fe = new ConstituentBoundaryFeatureExtractor<CompleteClosureSequence>(
                "lum1,lu,rup1", lexicon, unkClassSet, vocabulary, true);
        final int offset1 = unkClassSet.size();
        final int offset2 = offset1 + unkClassSet.size();

        // "This time around"
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullUnk, offset1 + thisUnk,
                offset2 + commaUnk }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(0, 3, 10, true)));

        // " 're moving even faster " (longer-span features)
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { theyUnk, offset1 + reUnk,
                offset2 + periodUnk }),
                fe.featureVector(trainingCorpusSequences.get(0), Chart.cellIndex(5, 9, 10, true)));
    }

    @Test
    public void testOneSentenceTraining() throws IOException {
        final String tree = "(ROOT (S (NP (DT The) (NN fish) (NN market)) (VP (VB stands) (RB last))))";
        final CompleteClosureClassifier classifier = new CompleteClosureClassifier("lw_rw");
        classifier.trainingIterations = 2;
        classifier.lexicon = new SymbolSet<String>();
        classifier.decisionTreeUnkClassSet = new SymbolSet<String>();
        classifier.train(new BufferedReader(new StringReader(tree)));

        final BinaryClassifierResult result = classifier.classify(new BufferedReader(new StringReader(tree)));
        // We expect to memorize the training set
        assertEquals(1.0f, result.accuracy(), .01f);
    }

    @Test
    public void testTraining() throws IOException {
        final String file = "corpora/wsj/wsj_24.mrgEC.20";
        final CompleteClosureClassifier classifier = new CompleteClosureClassifier();
        classifier.trainingIterations = 25;
        classifier.lexicon = new SymbolSet<String>();
        classifier.decisionTreeUnkClassSet = new SymbolSet<String>();

        classifier.train(new BufferedReader(JUnit.unitTestDataAsReader(file)));
        final BinaryClassifierResult result = classifier.classify(new BufferedReader(JUnit.unitTestDataAsReader(file)));
        // We expect to (nearly) memorize the training set
        assertTrue("Expected at least 97.7%, but was " + result.precision(), result.precision() > .977f);
    }

}
