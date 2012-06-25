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

package edu.ohsu.cslu.dep;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.junit.Before;
import org.junit.Test;

import edu.ohsu.cslu.datastructs.vectors.BitVector;
import edu.ohsu.cslu.dep.DependencyGraph.Arc;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * @author Aaron Dunlop
 * @since May 24, 2012
 */
public class TestNivreParserFeatureExtractor {

    private SymbolSet<String> lexicon;
    private int lexiconSize;

    private SymbolSet<String> pos;
    private int posSetSize;

    private SymbolSet<String> labels;
    private int labelSetSize;

    private Arc[] arcs;

    @Before
    public void setUp() {
        lexicon = new SymbolSet<String>();
        lexicon.addSymbol(DependencyGraph.NULL);
        lexicon.addSymbol("the");
        lexicon.addSymbol("dog");
        lexicon.addSymbol("barked");
        lexicon.addSymbol(DependencyGraph.ROOT.token);
        lexiconSize = lexicon.size();

        pos = new SymbolSet<String>();
        pos.addSymbol(DependencyGraph.NULL);
        pos.addSymbol("DT");
        pos.addSymbol("NN");
        pos.addSymbol("VBD");
        pos.addSymbol(DependencyGraph.ROOT.pos);
        posSetSize = pos.size();

        labels = new SymbolSet<String>();
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

        NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor("i0w,s0w,s1w_s0w", lexicon, pos, labels);
        assertEquals(lexiconSize * 2 + lexiconSize * lexiconSize, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        // Features when the stack contains 2 words
        BitVector features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 1);
        assertTrue(features.contains(lexicon.getIndex("dog")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("the")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex(DependencyGraph.NULL) * lexiconSize
                + lexicon.getIndex("the")));

        // A version when the stack contains 2 words
        stack.push(arcs[1]);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 2);
        assertTrue(features.contains(lexicon.getIndex("barked")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("dog")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex("the") * lexiconSize + lexicon.getIndex("dog")));

        // A version when all words have been pushed onto the stack
        stack.push(arcs[2]);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
        assertTrue(features.contains(lexicon.getIndex(DependencyGraph.ROOT.token)));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("barked")));
        assertTrue(features.contains(lexiconSize * 2 + lexicon.getIndex("dog") * lexiconSize
                + lexicon.getIndex("barked")));

        // And using lookahead into the buffer
        fe = new NivreParserFeatureExtractor("i0w,s0w,s0w_i0w", lexicon, pos, labels);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
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
        NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor("i0t,s0t,s1t_s0w", lexicon, pos, labels);
        assertEquals(posSetSize * 2 + posSetSize * lexiconSize, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        // Features when the stack contains only 1 word
        BitVector features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 1);
        assertTrue(features.contains(pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize + pos.getIndex("DT")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex(DependencyGraph.NULL) * lexiconSize
                + lexicon.getIndex("the")));

        // A version when the stack contains 2 words
        stack.push(arcs[1]);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 2);
        assertTrue(features.contains(pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize + pos.getIndex("NN")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("DT") * lexiconSize + lexicon.getIndex("dog")));

        // A version when all words have been pushed onto the stack
        stack.push(arcs[2]);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
        assertTrue(features.contains(pos.getIndex(DependencyGraph.ROOT.pos)));
        assertTrue(features.contains(posSetSize + pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("NN") * lexiconSize + lexicon.getIndex("barked")));

        // And using lookahead into the buffer
        fe = new NivreParserFeatureExtractor("i0t,s0t,s0t_i0t", lexicon, pos, labels);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
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
        NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor("s0m2t,s0m1t,s02t", lexicon, pos, labels);
        assertEquals(posSetSize * 3, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);

        BitVector features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 1);
        assertTrue(features.contains(pos.getIndex(DependencyGraph.NULL)));
        assertTrue(features.contains(posSetSize + pos.getIndex(DependencyGraph.NULL)));
        assertTrue(features.contains(posSetSize * 2 + pos.getIndex("VBD")));

        stack.push(arcs[1]);
        stack.push(arcs[2]);
        fe = new NivreParserFeatureExtractor("s1m1w,s11w", lexicon, pos, labels);
        assertEquals(lexiconSize * 2, fe.featureVectorLength);
        features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
        assertTrue(features.contains(lexicon.getIndex("the")));
        assertTrue(features.contains(lexiconSize + lexicon.getIndex("barked")));
    }

    @Test
    public void testDependentFeatures() {
        final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor("s0t,s1t,s1ldep,s0rdep", lexicon, pos,
                labels);
        assertEquals(lexiconSize * 2 + labelSetSize * 2, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();

        stack.push(arcs[1]);
        stack.push(arcs[2]);

        // Fake predictions that wouldn't be made yet in this simple sentence, just to test the feature-extractor
        arcs[0].predictedHead = 2;
        arcs[0].predictedLabel = "PMOD";

        arcs[1].predictedHead = 3;
        arcs[1].predictedLabel = "SBJ";

        final BitVector features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 3);
        assertTrue(features.contains(posSetSize * 2 + labels.getIndex("PMOD")));
        assertTrue(features.contains(posSetSize * 2 + labelSetSize + labels.getIndex("SBJ")));
    }

    /**
     * A simple example using distance tag features
     */
    @Test
    public void testDistanceFeatures() {
        final NivreParserFeatureExtractor fe = new NivreParserFeatureExtractor("s1t_s0t,d", lexicon, pos, labels);
        assertEquals(posSetSize * posSetSize + NivreParserFeatureExtractor.DISTANCE_BINS, fe.featureVectorLength);
        final LinkedList<Arc> stack = new LinkedList<Arc>();
        stack.push(arcs[0]);
        stack.push(arcs[2]);

        // Features when the stack contains 2 words that are not sequential (1 word has
        // already been reduced between the two)
        final BitVector features = fe.forwardFeatureVector(new NivreParserContext(stack, arcs), 2);
        assertTrue(features.contains(pos.getIndex("DT") * posSetSize + pos.getIndex("VBD")));
        assertTrue(features.contains(posSetSize * posSetSize + NivreParserFeatureExtractor.DISTANCE_2));
    }
}
