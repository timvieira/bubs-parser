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

package edu.ohsu.cslu.dep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * @author Aaron Dunlop
 * @since May 24, 2012
 */
public class TestTransitionParserFeatureExtractor {

    private MutableEnumeration<String> lexicon;
    private int lexiconSize;

    private MutableEnumeration<String> pos;
    private int posSetSize;

    private MutableEnumeration<String> labels;
    @SuppressWarnings("unused")
    private int labelSetSize;

    private Arc[] arcs;

    @Before
    public void setUp() {
        lexicon = new MutableEnumeration<String>();
        lexicon.addSymbol(DependencyGraph.NULL);
        lexicon.addSymbol("the");
        lexicon.addSymbol("dog");
        lexicon.addSymbol("barked");
        lexicon.addSymbol(DependencyGraph.ROOT.token);
        lexiconSize = lexicon.size();

        pos = new MutableEnumeration<String>();
        pos.addSymbol(DependencyGraph.NULL);
        pos.addSymbol("DT");
        pos.addSymbol("NN");
        pos.addSymbol("VBD");
        pos.addSymbol(DependencyGraph.ROOT.pos);
        posSetSize = pos.size();

        labels = new MutableEnumeration<String>();
        labels.addSymbol(DependencyGraph.NULL);
        labels.addSymbol("NMOD");
        labels.addSymbol("SBJ");
        labels.addSymbol("ROOT");
        labelSetSize = labels.size();

        arcs = new Arc[4];
        arcs[0] = new Arc("the", "DT", "DT", 1, 2, "NMOD");
        arcs[1] = new Arc("dog", "NN", "NN", 2, 3, "SBJ");
        arcs[2] = new Arc("barked", "VB", "VBD", 3, 0, "ROOT");
        arcs[3] = DependencyGraph.ROOT;
    }

    /**
     * Simple examples using only unigram and bigram word features
     */
    @Test
    public void testWordFeatures() {

        TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor("i0w,s0w,s1w_s0w", lexicon, pos,
                labels);
        assertEquals(lexiconSize * 2 + lexiconSize * lexiconSize, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        // Features when the stack contains 2 words
        BitVector features = fe.featureVector(new NivreParserContext(stack, arcs, 0), 1);
        assertTrue(features.contains(lexicon.getIndex("dog")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("the")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex(DependencyGraph.NULL) * lexiconSize
                + lexicon.getIndex("the")));

        // A version when the stack contains 2 words
        stack.push(arcs[1]);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 1), 2);
        assertTrue(features.contains(lexicon.getIndex("barked")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("dog")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex("the") * lexiconSize + lexicon.getIndex("dog")));

        // A version when all words have been pushed onto the stack
        stack.push(arcs[2]);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(lexicon.getIndex(DependencyGraph.ROOT.token)));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("barked")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex("dog") * lexiconSize
                + lexicon.getIndex("barked")));

        // And using lookahead into the buffer
        fe = new TransitionParserFeatureExtractor("i0w,s0w,s0w_i0w", lexicon, pos, labels);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(lexicon.getIndex(DependencyGraph.ROOT.token)));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("barked")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex("barked") * lexiconSize
                + lexicon.getIndex(DependencyGraph.ROOT.token)));
    }

    /**
     * A simple example using unigram and bigram tag features
     */
    @Test
    public void testPosFeatures() {
        TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor("i0t,s0t,s1t_s0w", lexicon, pos,
                labels);
        assertEquals(posSetSize * 2 + posSetSize * lexiconSize, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        // Features when the stack contains only 1 word
        BitVector features = fe.featureVector(new NivreParserContext(stack, arcs, 0), 1);
        assertTrue(features.contains(pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize + pos.getIndex("DT")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex(DependencyGraph.NULL) * lexiconSize
                + lexicon.getIndex("the")));

        // A version when the stack contains 2 words
        stack.push(arcs[1]);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 1), 2);
        assertTrue(features.contains(pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize + pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("DT") * lexiconSize + lexicon.getIndex("dog")));

        // A version when all words have been pushed onto the stack
        stack.push(arcs[2]);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(pos.getIndex(DependencyGraph.ROOT.pos)));
        assertTrue(features.contains(posSetSize + pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("NN") * lexiconSize + lexicon.getIndex("barked")));

        // And using lookahead into the buffer
        fe = new TransitionParserFeatureExtractor("i0t,s0t,s0t_i0t", lexicon, pos, labels);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(pos.getIndex(DependencyGraph.ROOT.pos)));
        assertTrue(features.contains(posSetSize + pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("VBD") * posSetSize
                + pos.getIndex(DependencyGraph.ROOT.pos)));
    }

    /**
     * Tests features for words offset from stack contents
     */
    @Test
    public void testAbsoluteOffsetFeatures() {
        TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor("s0m2t,s0m1t,s02t", lexicon, pos,
                labels);
        assertEquals(posSetSize * 3, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        BitVector features = fe.featureVector(new NivreParserContext(stack, arcs, 0), 1);
        assertTrue(features.contains(pos.getIndex(DependencyGraph.NULL)));
        assertTrue(features.contains(posSetSize + pos.getIndex(DependencyGraph.NULL)));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("VBD")));

        stack.push(arcs[1]);
        stack.push(arcs[2]);
        fe = new TransitionParserFeatureExtractor("s1m1w,s11w", lexicon, pos, labels);
        assertEquals(lexiconSize * 2, fe.featureVectorLength);
        features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(lexicon.getIndex("the")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("barked")));
    }

    @Test
    public void testDependentFeatures() {
        final TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor("s0lt,s1lt,s0rt,s1pt",
                lexicon, pos, labels);
        assertEquals(posSetSize * 4, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();

        // 'dog'
        stack.push(arcs[1]);
        // 'barked'
        stack.push(arcs[2]);

        // Fake predictions that wouldn't be made yet in this simple sentence, just to test the feature-extractor

        // 'The' -> 'dog'
        arcs[0].predictedHead = 2;

        // 'dog' -> barked
        arcs[1].predictedHead = 3;

        final BitVector features = fe.featureVector(new NivreParserContext(stack, arcs, 2), 3);
        assertTrue(features.contains(pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize + pos.getIndex("DT")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize * 3 + pos.getIndex("VBD")));
    }

    /**
     * A simple example using distance tag features
     */
    @Test
    public void testDistanceFeatures() {
        final TransitionParserFeatureExtractor fe = new TransitionParserFeatureExtractor("s1t_s0t,d", lexicon, pos,
                labels);
        assertEquals(posSetSize * posSetSize + TransitionParserFeatureExtractor.DISTANCE_BINS, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);
        stack.push(arcs[2]);

        // Features when the stack contains 2 words that are not sequential (1 word has
        // already been reduced between the two)
        final BitVector features = fe.featureVector(new NivreParserContext(stack, arcs, 1), 2);
        assertTrue(features.contains(pos.getIndex("DT") * posSetSize + pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize * posSetSize + TransitionParserFeatureExtractor.DISTANCE_2));
    }
}
