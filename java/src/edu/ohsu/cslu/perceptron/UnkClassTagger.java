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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;

import java.io.File;

import cltool4j.BaseLogger;
import cltool4j.args4j.Option;
import edu.ohsu.cslu.grammar.DecisionTreeTokenClassifier;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.LeftCscSparseMatrixGrammar;
import edu.ohsu.cslu.grammar.SparseMatrixGrammar.PerfectIntPairHashPackingFunction;
import edu.ohsu.cslu.util.MutableEnumeration;

/**
 * Classifies unknown-words into clusters, using lexical features and the surrounding syntax. Trains on transformed
 * corpus including POS tags, tokens, and unknown-word classes for all rare words. Class/token format is
 * UNK-&lt;clusterID&gt;|&lt;token&gt;. Supports bracketed input (POS token) (POS UNK*) (POS token)... or trees.
 * 
 * @author Aaron Dunlop
 */
public class UnkClassTagger extends Tagger {

    private static final long serialVersionUID = 1L;

    // Training an UNK-class tagger requires an input grammar. We can test tagging without it, but at inference time, we
    // need the lexicon indices to match
    @Option(name = "-g", requires = "-m", metaVar = "grammar", usage = "Grammar file.")
    protected File grammarFile;

    MutableEnumeration<String> unigramSuffixSet;
    MutableEnumeration<String> bigramSuffixSet;

    /**
     * Default Feature Templates:
     * 
     * <pre>
     * # Contains-numeral and numeral percentage
     * num
     * num20
     * num40
     * num60
     * num80
     * num100
     * 
     * # Contains-punctuation and punctuation percentage
     * punct
     * punct20
     * punct40
     * punct60
     * punct80
     * punct100
     * 
     * # POS features
     * posm1
     * pos
     * posp1
     * 
     * # Unigram and bigram suffixes
     * us
     * bs
     * </pre>
     */
    @Override
    protected final String DEFAULT_FEATURE_TEMPLATES() {
        return "num,num20,num40,num60,num80,num100,punct,punct20,punct40,punct60,punct80,punct100,posm1,pos,posp1,us,usm1,bs,bsm1,punct,punctm1,punctp1";
    }

    public UnkClassTagger() {
        this.posSet = new MutableEnumeration<String>();
        this.posSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.unigramSuffixSet = new MutableEnumeration<String>();
        this.unigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);

        this.bigramSuffixSet = new MutableEnumeration<String>();
        this.bigramSuffixSet.defaultReturnValue(Grammar.nullSymbolStr);
    }

    @Override
    protected void setup() throws Exception {
        super.setup();
        if (trainingIterations > 0 && crossValidationFolds == 0) {
            BaseLogger.singleton().info("Reading grammar file...");
            final Grammar g = new LeftCscSparseMatrixGrammar(fileAsBufferedReader(grammarFile),
                    new DecisionTreeTokenClassifier(), PerfectIntPairHashPackingFunction.class);
            init(g.lexSet, g.unkClassSet());
        }
    }

    @Override
    protected MulticlassTagSequence createSequence(final String line) {
        return new UnkClassSequence(line, this);
    }

    /**
     * Overrides the superclass implementation to only classify unknown words
     */
    @Override
    public short[] classify(final MulticlassTagSequence sequence) {

        for (int i = 0; i < sequence.length; i++) {
            if (sequence.mappedTokens[i] < 0) {
                sequence.predictedClasses[i] = classify(featureExtractor.featureVector(sequence, i));
            } else {
                sequence.predictedClasses[i] = -1;
            }
        }
        return sequence.predictedClasses;
    }

    @Override
    protected void finalizeMaps() {
        super.finalizeMaps();

        unigramSuffixSet.finalize();
        bigramSuffixSet.finalize();
    }

    @Override
    protected void initFromModel(final edu.ohsu.cslu.perceptron.Tagger.Model tmp) {
        this.unigramSuffixSet = ((Model) tmp).unigramSuffixSet;
        this.bigramSuffixSet = ((Model) tmp).bigramSuffixSet;
        super.initFromModel(tmp);
    }

    /**
     * Overrides the superclass implementation to include {@link #unigramSuffixSet} and {@link #bigramSuffixSet}.
     */
    @Override
    protected MulticlassTaggerFeatureExtractor featureExtractor() {
        return new MulticlassTaggerFeatureExtractor(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet,
                unigramSuffixSet, bigramSuffixSet, tagSet);
    }

    @Override
    protected Model model() {
        return new Model(featureTemplates, lexicon, decisionTreeUnkClassSet, posSet, unigramSuffixSet, bigramSuffixSet,
                tagSet(), parallelArrayOffsetMap, parallelWeightArrayTags, parallelWeightArray);
    }

    public static void main(final String[] args) {
        run(args);
    }

    protected static class Model extends Tagger.Model {

        private static final long serialVersionUID = 1L;

        final MutableEnumeration<String> unigramSuffixSet;
        final MutableEnumeration<String> bigramSuffixSet;

        protected Model(final String featureTemplates, final MutableEnumeration<String> lexicon,
                final MutableEnumeration<String> unkClassSet, final MutableEnumeration<String> posSet,
                final MutableEnumeration<String> unigramSuffixSet, final MutableEnumeration<String> bigramSuffixSet,
                final MutableEnumeration<String> tagSet, final Long2IntOpenHashMap parallelArrayOffsetMap,
                final short[] parallelWeightArrayTags, final float[] parallelWeightArray) {

            super(featureTemplates, lexicon, unkClassSet, posSet, tagSet, parallelArrayOffsetMap,
                    parallelWeightArrayTags, parallelWeightArray);
            this.unigramSuffixSet = unigramSuffixSet;
            this.bigramSuffixSet = bigramSuffixSet;
        }
    }
}
