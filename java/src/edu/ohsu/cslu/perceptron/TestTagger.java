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

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.grammar.SymbolSet;
import edu.ohsu.cslu.grammar.Tokenizer;
import edu.ohsu.cslu.perceptron.TrainTagger.TagSequence;
import edu.ohsu.cslu.perceptron.TrainTagger.TaggerFeatureExtractor;

/**
 * Unit tests for {@link TrainTagger}
 */
public class TestTagger {

    private String trainingCorpus;

    private SymbolSet<String> lexicon = null;
    private SymbolSet<String> unkClassSet = null;
    private SymbolSet<String> tagSet = null;

    private ArrayList<TagSequence> trainingCorpusSequences = null;

    @SuppressWarnings("unused")
    private int nullTag, dtTag, nnTag, rpTag, nullToken, thisToken, timeToken, aroundToken, nullUnk, thisUnk, timeUnk,
            aroundUnk;

    @Before
    public void setUp() {
        final StringBuilder sb = new StringBuilder();
        sb.append("(DT This) (NN time) (RP around) (, ,) (PRP they) (VBP 're) (VBG moving) (RB even) (RBR faster) (. .)");
        trainingCorpus = sb.toString();

        lexicon = new SymbolSet<String>();
        lexicon.defaultReturnValue(TrainTagger.NULL_SYMBOL);

        unkClassSet = new SymbolSet<String>();
        unkClassSet.defaultReturnValue(TrainTagger.NULL_SYMBOL);

        tagSet = new SymbolSet<String>();
        tagSet.defaultReturnValue(TrainTagger.NULL_SYMBOL);

        trainingCorpusSequences = new ArrayList<TrainTagger.TagSequence>();
        for (final String line : trainingCorpus.split("\n")) {
            trainingCorpusSequences.add(new TagSequence(line, lexicon, unkClassSet, tagSet));
        }

        nullTag = tagSet.getIndex(TrainTagger.NULL_SYMBOL);
        dtTag = tagSet.getIndex("DT");
        nnTag = tagSet.getIndex("NN");
        rpTag = tagSet.getIndex("RP");

        nullToken = lexicon.getIndex(TrainTagger.NULL_SYMBOL);
        thisToken = lexicon.getIndex("This");
        timeToken = lexicon.getIndex("time");
        aroundToken = lexicon.getIndex("around");

        nullUnk = unkClassSet.getIndex(TrainTagger.NULL_SYMBOL);
        thisUnk = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("This", true, lexicon));
        timeUnk = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("time", false, lexicon));
        aroundUnk = unkClassSet.getIndex(Tokenizer.berkeleyGetSignature("around", false, lexicon));
    }

    public void testSymbolSets() {
        // tagSet includes <null>; lexicon includes <null> and UNK classes
        assertEquals(11, tagSet.size());
        assertEquals(16, lexicon.size());
    }

    @Test
    public void testUnigramFeatureExtractor() {

        // A trivially simple feature extractor
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1,w", lexicon, unkClassSet, tagSet);
        final int offset1 = tagSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag, offset1 + thisToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag, offset1 + timeToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));
    }

    @Test
    public void testBigramFeatureExtractor() {

        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1_w,tm2_tm1", lexicon, unkClassSet, tagSet);
        final int offset1 = tagSet.size() * lexicon.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * lexicon.size() + thisToken,
                offset1 + nullTag * tagSet.size() + nullTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * lexicon.size() + timeToken,
                offset1 + nullTag * tagSet.size() + dtTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nnTag * lexicon.size() + aroundToken,
                offset1 + dtTag * tagSet.size() + nnTag }), fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testTrigramFeatureExtractor() {

        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm2_tm1,tm2_tm1_w", lexicon, unkClassSet, tagSet);

        final int offset1 = tagSet.size() * tagSet.size();
        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * tagSet.size() + nullTag,
                offset1 + (nullTag * tagSet.size() + nullTag) * lexicon.size() + thisToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * tagSet.size() + dtTag,
                offset1 + (nullTag * tagSet.size() + dtTag) * lexicon.size() + timeToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * tagSet.size() + nnTag,
                offset1 + (dtTag * tagSet.size() + nnTag) * lexicon.size() + aroundToken }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }

    @Test
    public void testUnkClasses() {
        final TaggerFeatureExtractor fe = new TaggerFeatureExtractor("tm1_u,um2_um1", lexicon, unkClassSet, tagSet);
        final int offset1 = tagSet.size() * unkClassSet.size();

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nullTag * unkClassSet.size() + thisUnk,
                offset1 + nullTag * unkClassSet.size() + nullTag }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 0));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { dtTag * unkClassSet.size() + timeUnk,
                offset1 + nullUnk * unkClassSet.size() + thisUnk }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 1));

        assertEquals(new SparseBitVector(fe.featureVectorLength, new int[] { nnTag * unkClassSet.size() + aroundUnk,
                offset1 + thisUnk * unkClassSet.size() + timeUnk }),
                fe.forwardFeatureVector(trainingCorpusSequences.get(0), 2));
    }
}
