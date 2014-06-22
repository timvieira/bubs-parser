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

package edu.ohsu.cslu.perceptron;

import static org.junit.Assert.assertEquals;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.perceptron.MulticlassClassifier.MulticlassClassifierResult;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for {@link Tagger} and {@link TaggerFeatureExtractor}.
 */
public class TestTagger {

    private String trainingCorpus;

    private SymbolSet<String> lexicon = new SymbolSet<String>();
    private SymbolSet<String> unkClassSet = new SymbolSet<String>();
    private SymbolSet<String> tagSet = new SymbolSet<String>();
    private SymbolSet<String> unigramSuffixSet = new SymbolSet<String>();
    private SymbolSet<String> bigramSuffixSet = new SymbolSet<String>();

    private ArrayList<MulticlassTagSequence> trainingCorpusSequences = null;

    @SuppressWarnings("unused")
    private int nullTag, dtTag, nnTag, rpTag, nullToken, thisToken, timeToken, aroundToken, nullUnk, thisUnk, timeUnk,
            aroundUnk;

    @Before
    public void setUp() {
        this.lexicon.defaultReturnValue(Grammar.nullSymbolStr);
        this.unkClassSet.defaultReturnValue(Grammar.nullSymbolStr);
        this.tagSet.defaultReturnValue(Grammar.nullSymbolStr);
        this.unigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);
        this.bigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);

        final StringBuilder sb = new StringBuilder();
        sb.append("(DT This) (NN time) (RP around) (, ,) (PRP they) (VBP 're) (VBG moving) (RB even) (RBR faster) (. .)\n");
        sb.append("(U1 90) (U2 F-15s) (U3 will) (U3 be) (U3 retired) (U3 in) (U3 the) (U2 2020's) (U4 .)\n");
        trainingCorpus = sb.toString();

        trainingCorpusSequences = new ArrayList<MulticlassTagSequence>();
        for (final String line : trainingCorpus.split("\n")) {
            trainingCorpusSequences.add(new MulticlassTagSequence(line, lexicon, unkClassSet, null, unigramSuffixSet,
                    bigramSuffixSet, tagSet));
        }

        nullTag = tagSet.getIndex(Grammar.nullSymbolStr);
        dtTag = tagSet.getIndex("DT");
        nnTag = tagSet.getIndex("NN");
        rpTag = tagSet.getIndex("RP");

        nullToken = lexicon.getIndex(Grammar.nullSymbolStr);
        thisToken = lexicon.getIndex("This");
        timeToken = lexicon.getIndex("time");
        aroundToken = lexicon.getIndex("around");

        nullUnk = unkClassSet.getIndex(Grammar.nullSymbolStr);
        thisUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("This", true, lexicon));
        timeUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("time", false, lexicon));
        aroundUnk = unkClassSet.getIndex(DecisionTreeTokenClassifier.berkeleyGetSignature("around", false, lexicon));
    }

    @Test
    public void testSymbolSets() {
        // tagSet includes <null>; lexicon includes <null> and UNK classes
        assertEquals(15, tagSet.size());
        assertEquals(19, lexicon.size());
        assertEquals(10, unkClassSet.size());
    }

    @Test
    public void testUnigramFeatureExtractor() {

        // A trivially simple feature extractor
        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor("tm1,w", lexicon, unkClassSet,
                null, tagSet);
        final int offset1 = tagSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag, offset1 + thisToken }),
                fe.featureVector(trainingCorpusSequences.get(0), 0));
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag, offset1 + timeToken }),
                fe.featureVector(trainingCorpusSequences.get(0), 1));
    }

    @Test
    public void testBigramFeatureExtractor() {

        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor("tm1_w,tm2_tm1", lexicon,
                unkClassSet, null, tagSet);
        final int offset1 = tagSet.size() * lexicon.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * lexicon.size() + thisToken,
                offset1 + nullTag * tagSet.size() + nullTag }), fe.featureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * lexicon.size() + timeToken,
                offset1 + nullTag * tagSet.size() + dtTag }), fe.featureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nnTag * lexicon.size() + aroundToken,
                offset1 + dtTag * tagSet.size() + nnTag }), fe.featureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testTrigramFeatureExtractor() {

        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor("tm2_tm1,tm2_tm1_w", lexicon,
                unkClassSet, null, tagSet);

        final int offset1 = tagSet.size() * tagSet.size();
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * tagSet.size() + nullTag,
                offset1 + (nullTag * tagSet.size() + nullTag) * lexicon.size() + thisToken }),
                fe.featureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * tagSet.size() + dtTag,
                offset1 + (nullTag * tagSet.size() + dtTag) * lexicon.size() + timeToken }),
                fe.featureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * tagSet.size() + nnTag,
                offset1 + (dtTag * tagSet.size() + nnTag) * lexicon.size() + aroundToken }),
                fe.featureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testBerkeleyUnkClasses() {
        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor("tm1_u,um2_um1", lexicon,
                unkClassSet, null, tagSet);
        final int offset1 = tagSet.size() * unkClassSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * unkClassSet.size() + thisUnk,
                offset1 + nullTag * unkClassSet.size() + nullTag }),
                fe.featureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * unkClassSet.size() + timeUnk,
                offset1 + nullUnk * unkClassSet.size() + thisUnk }),
                fe.featureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nnTag * unkClassSet.size() + aroundUnk,
                offset1 + thisUnk * unkClassSet.size() + timeUnk }),
                fe.featureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testUnkClusters() {
        final MulticlassTaggerFeatureExtractor fe = new MulticlassTaggerFeatureExtractor(
                "numm1,num20,punctp1,punct20,us,bs", lexicon, unkClassSet, null, tagSet);
        final int offset1 = 2;
        final int offset2 = 4;
        final int offset3 = 6;
        final int offset4 = 8;
        final int offset5 = offset4 + unigramSuffixSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { 0, offset1 + 1, offset2 + 1, offset3,
                offset4 + unigramSuffixSet.getIndex("0"), offset5 + bigramSuffixSet.getIndex("90") }),
                fe.featureVector(trainingCorpusSequences.get(1), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { 1, offset1 + 1, offset2 + 0, offset3 + 1,
                offset4 + unigramSuffixSet.getIndex("s"), offset5 + bigramSuffixSet.getIndex("5s") }),
                fe.featureVector(trainingCorpusSequences.get(1), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { 1, offset1 + 0, offset2 + 0, offset3 + 0,
                offset4 + unigramSuffixSet.getIndex("l"), offset5 + bigramSuffixSet.getIndex("ll") }),
                fe.featureVector(trainingCorpusSequences.get(1), 2));
    }

    @Test
    public void testTraining() throws IOException {
        final String file = "corpora/wsj/wsj_24.postagged.5";

        final Tagger tagger = new Tagger();
        tagger.trainingIterations = 100;
        tagger.train(new BufferedReader(JUnit.unitTestDataAsReader(file)));
        final MulticlassClassifierResult result = tagger.testAccuracy(new MulticlassClassifier.LineIterator(JUnit
                .unitTestDataAsReader(file)));
        // We expect to memorize the training set
        assertEquals(1.0f, result.accuracy(), .01f);
    }
}
