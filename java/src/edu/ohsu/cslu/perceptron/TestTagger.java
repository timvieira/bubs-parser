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

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.perceptron.Tagger.TagSequence;
import edu.ohsu.cslu.tests.JUnit;

/**
 * Unit tests for {@link Tagger}
 */
public class TestTagger {

    private String trainingCorpus;

    private TaggerModel model = null;

    private ArrayList<TagSequence> trainingCorpusSequences = null;

    @SuppressWarnings("unused")
    private int nullTag, dtTag, nnTag, rpTag, nullToken, thisToken, timeToken, aroundToken, nullUnk, thisUnk, timeUnk,
            aroundUnk;

    @Before
    public void setUp() {
        model = new TaggerModel("");

        final StringBuilder sb = new StringBuilder();
        sb.append("(DT This) (NN time) (RP around) (, ,) (PRP they) (VBP 're) (VBG moving) (RB even) (RBR faster) (. .)");
        trainingCorpus = sb.toString();

        trainingCorpusSequences = new ArrayList<Tagger.TagSequence>();
        for (final String line : trainingCorpus.split("\n")) {
            trainingCorpusSequences.add(new TagSequence(line, model));
        }

        nullTag = model.tagSet.getIndex(Tagger.NULL_SYMBOL);
        dtTag = model.tagSet.getIndex("DT");
        nnTag = model.tagSet.getIndex("NN");
        rpTag = model.tagSet.getIndex("RP");

        nullToken = model.lexicon.getIndex(Tagger.NULL_SYMBOL);
        thisToken = model.lexicon.getIndex("This");
        timeToken = model.lexicon.getIndex("time");
        aroundToken = model.lexicon.getIndex("around");

        nullUnk = model.unkClassSet.getIndex(Tagger.NULL_SYMBOL);
        thisUnk = model.unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("This", true, model.lexicon));
        timeUnk = model.unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("time", false, model.lexicon));
        aroundUnk = model.unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("around", false, model.lexicon));
    }

    public void testSymbolSets() {
        // tagSet includes <null>; lexicon includes <null> and UNK classes
        assertEquals(11, model.tagSet.size());
        assertEquals(16, model.lexicon.size());
    }

    @Test
    public void testUnigramFeatureExtractor() {

        // A trivially simple feature extractor
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1,w", model);
        final int offset1 = model.tagSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag, offset1 + thisToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag, offset1 + timeToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));
    }

    @Test
    public void testBigramFeatureExtractor() {

        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1_w,tm2_tm1", model);
        final int offset1 = model.tagSet.size() * model.lexicon.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] {
                nullTag * model.lexicon.size() + thisToken, offset1 + nullTag * model.tagSet.size() + nullTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * model.lexicon.size() + timeToken,
                offset1 + nullTag * model.tagSet.size() + dtTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] {
                nnTag * model.lexicon.size() + aroundToken, offset1 + dtTag * model.tagSet.size() + nnTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testTrigramFeatureExtractor() {

        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm2_tm1,tm2_tm1_w", model);

        final int offset1 = model.tagSet.size() * model.tagSet.size();
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * model.tagSet.size() + nullTag,
                offset1 + (nullTag * model.tagSet.size() + nullTag) * model.lexicon.size() + thisToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * model.tagSet.size() + dtTag,
                offset1 + (nullTag * model.tagSet.size() + dtTag) * model.lexicon.size() + timeToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * model.tagSet.size() + nnTag,
                offset1 + (dtTag * model.tagSet.size() + nnTag) * model.lexicon.size() + aroundToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testUnkClasses() {
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1_u,um2_um1", model);
        final int offset1 = model.tagSet.size() * model.unkClassSet.size();

        assertEquals(
                new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * model.unkClassSet.size() + thisUnk,
                        offset1 + nullTag * model.unkClassSet.size() + nullTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] {
                dtTag * model.unkClassSet.size() + timeUnk, offset1 + nullUnk * model.unkClassSet.size() + thisUnk }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(
                new SparseBitVector(fe.featureVectorLength, new int[] { nnTag * model.unkClassSet.size() + aroundUnk,
                        offset1 + thisUnk * model.unkClassSet.size() + timeUnk }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testTraining() throws IOException {
        final String file = "corpora/wsj/wsj_24.postagged.5";

        final Tagger tagger = new Tagger();
        tagger.trainingIterations = 100;
        tagger.train(new BufferedReader(JUnit.unitTestDataAsReader(file)));
        final int[] results = tagger.tag(new BufferedReader(JUnit.unitTestDataAsReader(file)));
        // We expect to memorize the training set
        assertEquals(1.0f, results[2] * 1.0f / results[1], .01f);
    }
}
