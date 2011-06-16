/*
 * Copyright 2010, 2011 Aaron Dunlop and Nathan Bodenstab
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
package edu.ohsu.cslu.lela;

import it.unimi.dsi.fastutil.ints.Int2FloatOpenHashMap;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2FloatOpenHashMap;
import it.unimi.dsi.fastutil.shorts.Short2ObjectOpenHashMap;
import edu.ohsu.cslu.grammar.Production;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PackingFunction;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Grammar computed from (fractional) observation counts, constrained by a base (unsplit) grammar.
 * 
 * @author Aaron Dunlop
 * @since Jan 13, 2011
 */
public class ConstrainedCountGrammar extends FractionalCountGrammar {

    // // TODO Rename these; they're not really base rules, they're sums over split parents
    // /** Parent -> Base grammar left child -> Base grammar right child -> log(count) */
    // private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> baseBinaryRuleLogCounts =
    // new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
    // /** Parent -> Base grammar child -> log(count) */
    // private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> baseUnaryRuleLogCounts = new
    // Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
    // private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> baseLexicalRuleLogCounts = new
    // Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();

    // /** Parent -> Base grammar packed children -> log(count) */
    // private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> basePackedBinaryRuleLogCounts;

    /** Base grammar parent -> Base grammar left child -> Base grammar right child -> log(probability) */
    private final Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>> baseBinaryRuleLogProbabilities;
    /** Base grammar parent -> Base grammar child -> log(probability) */
    private final Short2ObjectOpenHashMap<Short2FloatOpenHashMap> baseUnaryRuleLogProbabilities;
    private final Short2ObjectOpenHashMap<Int2FloatOpenHashMap> baseLexicalRuleLogProbabilities;

    private final Int2IntOpenHashMap basePackedChildren;

    public ConstrainedCountGrammar(final SplitVocabulary vocabulary, final SymbolSet<String> lexicon,
            final PackingFunction packingFunction) {

        super(vocabulary, lexicon, packingFunction);

        if (packingFunction != null) {
            // basePackedBinaryRuleLogCounts = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
            basePackedChildren = new Int2IntOpenHashMap();
            basePackedChildren.defaultReturnValue(Integer.MIN_VALUE);
            baseBinaryRuleLogProbabilities = new Short2ObjectOpenHashMap<Short2ObjectOpenHashMap<Short2FloatOpenHashMap>>();
            baseUnaryRuleLogProbabilities = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
            baseLexicalRuleLogProbabilities = new Short2ObjectOpenHashMap<Int2FloatOpenHashMap>();
        } else {
            // basePackedBinaryRuleLogCounts = null;
            basePackedChildren = null;
            baseBinaryRuleLogProbabilities = null;
            baseUnaryRuleLogProbabilities = null;
            baseLexicalRuleLogProbabilities = null;
        }
    }

    public ConstrainedCountGrammar(final ConstrainedCsrSparseMatrixGrammar grammar) {
        this((SplitVocabulary) grammar.nonTermSet, grammar.lexSet, grammar.packingFunction);

        // Initialize maps of base grammar probabilities
        for (final Production p : grammar.baseGrammar.binaryProductions) {
            Short2ObjectOpenHashMap<Short2FloatOpenHashMap> leftChildMap = baseBinaryRuleLogProbabilities
                    .get((short) p.parent);
            if (leftChildMap == null) {
                leftChildMap = new Short2ObjectOpenHashMap<Short2FloatOpenHashMap>();
                baseBinaryRuleLogProbabilities.put((short) p.parent, leftChildMap);
            }

            Short2FloatOpenHashMap rightChildMap = leftChildMap.get((short) p.leftChild);
            if (rightChildMap == null) {
                rightChildMap = new Short2FloatOpenHashMap();
                rightChildMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                leftChildMap.put((short) p.leftChild, rightChildMap);
            }

            rightChildMap.put((short) p.rightChild, p.prob);
        }

        for (final Production p : grammar.baseGrammar.unaryProductions) {
            Short2FloatOpenHashMap childMap = baseUnaryRuleLogProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Short2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                baseUnaryRuleLogProbabilities.put((short) p.parent, childMap);
            }

            childMap.put((short) p.leftChild, p.prob);
        }

        for (final Production p : grammar.baseGrammar.lexicalProductions) {
            Int2FloatOpenHashMap childMap = baseLexicalRuleLogProbabilities.get((short) p.parent);
            if (childMap == null) {
                childMap = new Int2FloatOpenHashMap();
                childMap.defaultReturnValue(Float.NEGATIVE_INFINITY);
                baseLexicalRuleLogProbabilities.put((short) p.parent, childMap);
            }

            childMap.put(p.leftChild, p.prob);
        }
    }

    @Override
    public void incrementBinaryLogCount(final short parent, final short leftChild, final short rightChild,
            final float logIncrement) {
        super.incrementBinaryLogCount(parent, leftChild, rightChild, logIncrement);
        // incrementParentLogCount(parent, logIncrement);
    }

    @Override
    public void incrementBinaryLogCount(final short parent, final int packedChildren, final float logIncrement) {
        super.incrementBinaryLogCount(parent, packedChildren, logIncrement);

        // int baseChildren = basePackedChildren.get(packedChildren);
        // if (baseChildren < 0) {
        // final short baseLeftChild = vocabulary.baseCategoryIndices[packingFunction.unpackLeftChild(packedChildren)];
        // final short baseRightChild = vocabulary.baseCategoryIndices[packingFunction
        // .unpackRightChild(packedChildren)];
        // baseChildren = baseLeftChild << 16 | baseRightChild;
        // }
    }
}
