package edu.ohsu.cslu.perceptron;

import java.util.Iterator;

import edu.ohsu.cslu.datastructs.narytree.NaryTree;
import edu.ohsu.cslu.datastructs.vectors.SparseBitVector;
import edu.ohsu.cslu.datastructs.vectors.Vector;
import edu.ohsu.cslu.grammar.Grammar;
import edu.ohsu.cslu.grammar.SymbolSet;

/**
 * Extracts features for classifying whether a lexical item can begin a multiword constituent. Depends only on lexical
 * items (as mapped by the supplied {@link Grammar}) and on prior tags (gold in training, predicted at test time).
 * 
 * TODO Add suffix features as well
 * 
 * TODO For the previous tags, do we use the predicted tags (as we will at test time) or the gold tags?
 * 
 * @author Aaron Dunlop
 * @since Oct 15, 2010
 * 
 * @version $Revision$ $Date$ $Author$
 */
public class BeginConstituentFeatureExtractor extends FeatureExtractor {

    private final Grammar grammar;
    private final int markovOrder;
    private final int featureCount;

    private final SymbolSet<String> twoCharacterSuffixes = new SymbolSet<String>();
    private final SymbolSet<String> threeCharacterSuffixes = new SymbolSet<String>();

    public BeginConstituentFeatureExtractor(final Grammar grammar, final int markovOrder) {
        this.grammar = grammar;
        this.markovOrder = markovOrder;
        final int windowSize = markovOrder * 2 + 1;
        // this.featureCount = grammar.numLexSymbols() * windowSize + markovOrder * 2;
        for (final String token : grammar.lexSet) {
            twoCharacterSuffixes.addSymbol(token.substring(token.length() - 2));
            threeCharacterSuffixes.addSymbol(token.substring(token.length() - 3));
        }
        this.featureCount = grammar.numLexSymbols() * windowSize + markovOrder * 2 + twoCharacterSuffixes.size()
                + threeCharacterSuffixes.size();
    }

    @Override
    public int featureCount() {
        return featureCount;
    }

    /**
     * Feature Order:
     * <ol>
     * <li>0..n : token i - o (i = index, o = Markov order)</li>
     * <li>n+1..2n : token i - o + 1</li>
     * ...
     * <li>2on+1..(2o+1)n : token i + o</li>
     * 
     * <li>bigram suffix i - o</li>
     * <li>bigram suffix i - o + 1</li>
     * ...
     * <li>bigram suffix i + o</li>
     * 
     * <li>trigram suffix i - o</li>
     * <li>trigram suffix i - o + 1</li>
     * ...
     * <li>trigram suffix i + o</li>
     * 
     * <li>tag i - o</li>
     * ...
     * <li>tag i - 1</li>
     * </ol>
     * 
     * @param sentence
     * @param tokenIndex
     * @param tags
     * @return a feature vector representing the specified token and tags
     */
    @Override
    public SparseBitVector forwardFeatureVector(final Object source, final int tokenIndex, final float[] tagScores) {
        final Sentence s = (Sentence) source;
        final int windowSize = markovOrder * 2 + 1;

        int offset = 0;
        // Token features
        final int[] vector = new int[windowSize * 3 + markovOrder];
        for (int j = 0; j < windowSize; j++) {
            vector[j] = offset + j * grammar.numLexSymbols() + s.mappedTokens[tokenIndex + j];
        }
        offset += windowSize * grammar.numLexSymbols();

        // // Suffix features
        // for (int j = 0; j < windowSize; j++) {
        // vector[j] = offset + ;
        // }

        // Tag features
        offset += windowSize * (twoCharacterSuffixes.size() + threeCharacterSuffixes.size());
        for (int j = 0; j < markovOrder; j++) {
            final int tagIndex = j - markovOrder + tokenIndex;
            if (tagIndex >= 0 && tagScores[tagIndex] > 0) { // TODO > 0? > -Infinity?
                vector[windowSize + j] = offset + j * 2;
            } else {
                vector[windowSize + j] = offset + j * 2 + 1;
            }
        }

        // TODO Add a direct constructor to SparseBitVector
        return new SparseBitVector(vector, false);
    }

    @Override
    public Vector forwardFeatureVector(final Object source, final int tokenIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector backwardFeatureVector(final Object source, final int tokenIndex) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Vector backwardFeatureVector(final Object source, final int tokenIndex, final float[] tagScores) {
        // TODO Auto-generated method stub
        return null;
    }

    public class Sentence {
        /**
         * Parallel array of tokens (mapped according to the grammar lexicon) and booleans labeling tokens which start
         * or end multiword constituents. The parallel array is of length n + 2o, where n is the length of the sentence
         * and o is the Markov order. That is, we allow o entries prior to the beginning of the sentence and o following
         * the end, so a Markov-order-2 tagger for a 6-word sentence will be represented by a 10-element array.
         */
        public final String[] tokens;
        public final int[] mappedTokens;

        /** Indexed from 0..n (since null symbols cannot begin multiword constituents */
        public final boolean[] beginsMultiwordConstituent;

        public Sentence(final String sentence) {

            if (sentence.startsWith("(")) {
                final NaryTree<String> tree = NaryTree.read(sentence, String.class);
                final int sentenceLength = tree.leaves();

                tokens = new String[sentenceLength + 2 * markovOrder];
                mappedTokens = new int[sentenceLength + 2 * markovOrder];
                System.arraycopy(grammar.tokenizer.tokenizeToIndex(tree), 0, mappedTokens, markovOrder, sentenceLength);
                beginsMultiwordConstituent = new boolean[sentenceLength];

                for (int k = 0; k < markovOrder; k++) {
                    mappedTokens[k] = grammar.nullWord;
                    mappedTokens[mappedTokens.length - k - 1] = grammar.nullWord;
                }

                int k = markovOrder;
                for (final Iterator<NaryTree<String>> iter = tree.leafIterator(); iter.hasNext();) {
                    final NaryTree<String> leaf = iter.next();
                    beginsMultiwordConstituent[k - markovOrder] = leaf.isLeftmostChild();
                    tokens[k++] = leaf.label();
                }
            } else {
                final String[] tmpTokens = sentence.split(" ");

                mappedTokens = new int[tmpTokens.length + 2 * markovOrder];
                System.arraycopy(grammar.tokenizer.tokenizeToIndex(sentence), 0, mappedTokens, markovOrder,
                        tmpTokens.length);

                tokens = new String[tmpTokens.length + 2 * markovOrder];
                System.arraycopy(tmpTokens, 0, tokens, markovOrder, tmpTokens.length);
                beginsMultiwordConstituent = null;

                for (int k = 0; k < markovOrder; k++) {
                    mappedTokens[k] = grammar.nullWord;
                    mappedTokens[mappedTokens.length - k - 1] = grammar.nullWord;
                }
            }
        }

        public SparseBitVector[] featureVectors() {
            final SparseBitVector[] featureVectors = new SparseBitVector[tokens.length - 2 * markovOrder];
            for (int i = 0; i < featureVectors.length; i++) {
                featureVectors[i] = forwardFeatureVector(this, i, null);
            }
            return featureVectors;
        }

        public boolean[] goldTags() {
            return beginsMultiwordConstituent;
        }

        public int length() {
            return beginsMultiwordConstituent.length;
        }
    }
}
